package me.tuanzi.block.entity;

import me.tuanzi.block.PlayerSimulatorBlock;
import me.tuanzi.init.ModBlocks;
import me.tuanzi.util.SimulatorFakePlayer;
import me.tuanzi.world.inventory.PlayerSimulatorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class PlayerSimulatorBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer {
    private final NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);

    // 状态机字段
    private boolean running = false;
    private int currentSlot = 0;
    private int cooldown = 0;
    private int actionTick = 0;
    private @Nullable BlockPos miningPos = null;
    private float miningProgress = 0.0f;
    private int targetChargeTicks = 0;
    private int chargeTicks = 0;

    // 缓存的假玩家实例
    private @Nullable SimulatorFakePlayer fakePlayer = null;

    // 拥有者归属数据
    private @Nullable java.util.UUID ownerUuid = null;
    private String ownerName = "";
    
    // 持续充能攻击的锁定实体ID (-1 表示未锁定)
    private int attackingEntityId = -1;

    public PlayerSimulatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.PLAYER_SIMULATOR_BLOCK_ENTITY, pos, state);
    }

    public NonNullList<ItemStack> getItems() {
        return this.items;
    }

    public void setOwner(java.util.UUID uuid, String name) {
        this.ownerUuid = uuid;
        this.ownerName = name;
        this.setChanged();
    }

    public @Nullable java.util.UUID getOwnerUuid() {
        return this.ownerUuid;
    }

    public String getOwnerName() {
        return this.ownerName;
    }

    public boolean hasOwner() {
        return this.ownerUuid != null;
    }

    private SimulatorFakePlayer getOrLoadFakePlayer() {
        if (this.fakePlayer == null || this.fakePlayer.level() != this.level) {
            this.fakePlayer = new SimulatorFakePlayer((ServerLevel) this.level);
        }
        if (this.ownerUuid != null) {
            this.fakePlayer.setOwnerUuid(this.ownerUuid);
        }
        return this.fakePlayer;
    }

    // 触发上升沿序列
    public void trigger() {
        if (this.running || this.cooldown > 0) {
            return;
        }

        // 寻找下一个非空槽位
        int startSlot = this.currentSlot;
        int targetSlot = -1;
        for (int i = 0; i < 9; i++) {
            int checkSlot = (startSlot + i) % 9;
            if (!this.items.get(checkSlot).isEmpty()) {
                targetSlot = checkSlot;
                break;
            }
        }

        if (targetSlot != -1) {
            this.currentSlot = targetSlot;
            this.running = true;
            this.actionTick = 0;
            this.miningPos = null;
            this.miningProgress = 0.0f;
            this.targetChargeTicks = 0;

            boolean hasDelay = this.executeSlotAction(this.currentSlot);
            if (!hasDelay) {
                // 瞬间完成的动作，当场结束序列，进入冷却，移至下一格
                this.running = false;
                this.cooldown = 2;
                this.currentSlot = (this.currentSlot + 1) % 9;
                this.setChanged();
            }
        }
    }

    // 执行指定槽位的动作。返回 true 代表动作需要持续 tick（即有延时），false 代表瞬发完成
    private boolean executeSlotAction(int slotIdx) {
        if (this.level == null || this.level.isClientSide()) {
            return false;
        }

        ItemStack stack = this.items.get(slotIdx);
        if (stack.isEmpty()) {
            return false;
        }

        SimulatorFakePlayer player = this.getOrLoadFakePlayer();
        Direction facing = this.getBlockState().getValue(PlayerSimulatorBlock.FACING);

        // 设置位置和朝向（将假玩家置于模拟器方块中心，并通过扣除眼睛高度保证视线起始点位于方块几何中心）
        double x = this.worldPosition.getX() + 0.5;
        double y = this.worldPosition.getY() + 0.5 - player.getEyeHeight();
        double z = this.worldPosition.getZ() + 0.5;
        player.setPos(x, y, z);

        float yaw = facing.getAxis().isVertical() ? 0.0f : facing.toYRot();
        float pitch = facing == Direction.DOWN ? 90.0f : (facing == Direction.UP ? -90.0f : 0.0f);
        player.setXRot(pitch);
        player.setYRot(yaw);
        player.yRotO = yaw;
        player.xRotO = pitch;
        player.setYHeadRot(yaw);

        // 主手持有该物品，必须通过 setItemSlot 触发属性修改器的加载
        ItemStack previous = player.getMainHandItem().copy();
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, stack);
        player.updateEquipmentModifiers(previous, stack);
        player.getInventory().setItem(0, stack);
        player.getInventory().setSelectedSlot(0);

        // 射线检测前方目标 (缩短至前方 1 个方块的范围，从中心 0.5 到 1.5 恰好覆盖前面这一个格)
        double reach = 1.5;
        // 眼睛起点移至方块边界外侧（0.501格），防止射线与大炮自身碰撞箱发生误判自撞
        Vec3 eyePos = new Vec3(
            this.worldPosition.getX() + 0.5 + facing.getStepX() * 0.501,
            this.worldPosition.getY() + 0.5 + facing.getStepY() * 0.501,
            this.worldPosition.getZ() + 0.5 + facing.getStepZ() * 0.501
        );
        // 直接使用方块面向对应的三维方向向量，确保绝对方向一致、杜绝角度反置或偏差
        Vec3 lookVec = new Vec3(facing.getStepX(), facing.getStepY(), facing.getStepZ());
        Vec3 endPos = eyePos.add(lookVec.scale(reach));

        me.tuanzi.util.ModLog.debug("[玩家模拟器] 开始触发第 " + slotIdx + " 槽位，执行物品: " + stack.getItem().getName(stack).getString() + "，朝向: " + facing + "，定位坐标: " + player.position() + "，眼睛位置: " + eyePos);

        // 1. 块射线检测
        ClipContext clipCtx = new ClipContext(
            eyePos, 
            endPos, 
            ClipContext.Block.OUTLINE, 
            ClipContext.Fluid.NONE, 
            player
        );
        BlockHitResult blockHit = this.level.clip(clipCtx);

        // 2. 实体检测：精确捕获大炮前方 1 格（包含上下各 1 格范围）的所有活体实体，摆脱窄射线 Y 轴失准缺陷
        BlockPos targetPos = this.worldPosition.relative(facing);
        net.minecraft.world.phys.AABB aabb = new net.minecraft.world.phys.AABB(targetPos).inflate(0.5D, 1.0D, 0.5D);
        java.util.List<net.minecraft.world.entity.Entity> entities = this.level.getEntities(
            player, 
            aabb, 
            entity -> !entity.isSpectator() && entity.isPickable()
        );

        net.minecraft.world.entity.Entity targetEntity = null;
        double closestDist = Double.MAX_VALUE;
        for (net.minecraft.world.entity.Entity entity : entities) {
            double dist = entity.getBoundingBox().distanceToSqr(eyePos);
            if (dist < closestDist) {
                closestDist = dist;
                targetEntity = entity;
            }
        }
        EntityHitResult entityHit = targetEntity != null ? new EntityHitResult(targetEntity, targetEntity.getBoundingBox().getCenter()) : null;

        // 判定瞄准对象
        boolean hitEntity = false;
        if (entityHit != null && blockHit.getType() != HitResult.Type.MISS) {
            double entityDist = entityHit.getLocation().distanceToSqr(eyePos);
            double blockDist = blockHit.getLocation().distanceToSqr(eyePos);
            hitEntity = entityDist < blockDist;
        } else if (entityHit != null) {
            hitEntity = true;
        }

        me.tuanzi.util.ModLog.debug("[玩家模拟器] 射线检测结果 - 目标方块: " + (blockHit.getType() == HitResult.Type.MISS ? "无" : blockHit.getBlockPos() + " (" + this.level.getBlockState(blockHit.getBlockPos()).getBlock().getName().getString() + ")") + ", 目标实体: " + (entityHit != null ? entityHit.getEntity().getType().getDescription().getString() : "无") + ", 判定实体优先: " + hitEntity);

        // ================== 优先尝试右键 ==================
        
        // 特殊：食物/药水/牛奶等瞬发消耗并给予效果的物品
        if (stack.has(net.minecraft.core.component.DataComponents.FOOD) || stack.is(Items.POTION) || stack.is(Items.MILK_BUCKET)) {
            me.tuanzi.util.ModLog.debug("[玩家模拟器] 识别为食品/药水瞬发消耗，执行 finishUsingItem。");
            ItemStack finished = stack.finishUsingItem(this.level, player);
            player.setItemInHand(InteractionHand.MAIN_HAND, finished);
            this.syncFakePlayerInventory(slotIdx);
            return false;
        }

        // 特殊：武器蓄力判定
        if (stack.is(Items.BOW)) {
            this.targetChargeTicks = 20;
            this.actionTick = 0;
            player.startUsingItem(InteractionHand.MAIN_HAND);
            me.tuanzi.util.ModLog.debug("[玩家模拟器] 弓蓄力判定 - 开启蓄力 " + this.targetChargeTicks + " ticks");
            return true;
        }
        if (stack.is(Items.TRIDENT)) {
            this.targetChargeTicks = 10;
            this.actionTick = 0;
            player.startUsingItem(InteractionHand.MAIN_HAND);
            me.tuanzi.util.ModLog.debug("[玩家模拟器] 三叉戟蓄力判定 - 开启蓄力 " + this.targetChargeTicks + " ticks");
            return true;
        }
        if (stack.is(Items.CROSSBOW)) {
            if (CrossbowItem.isCharged(stack)) {
                // 已经装填的弩：直接发射
                stack.use(this.level, player, InteractionHand.MAIN_HAND);
                me.tuanzi.util.ModLog.debug("[玩家模拟器] 弩蓄力判定 - 已经装填并直接发射！");
                this.syncFakePlayerInventory(slotIdx);
                return false;
            } else {
                // 未装填的弩：开启蓄力装填
                this.targetChargeTicks = CrossbowItem.getChargeDuration(stack, player);
                this.actionTick = 0;
                player.startUsingItem(InteractionHand.MAIN_HAND);
                me.tuanzi.util.ModLog.debug("[玩家模拟器] 弩蓄力判定 - 未装填，开启蓄力装填 " + this.targetChargeTicks + " ticks");
                return true;
            }
        }

        // 正常右键：优先右键瞄准目标，否则右键空气
        if (hitEntity && entityHit != null) {
            InteractionResult interactRes = player.interactOn(entityHit.getEntity(), InteractionHand.MAIN_HAND, entityHit.getLocation().subtract(entityHit.getEntity().position()));
            if (interactRes.consumesAction()) {
                me.tuanzi.util.ModLog.debug("[玩家模拟器] 右键交互实体成功: " + entityHit.getEntity().getType().getDescription().getString() + " (结果: " + interactRes + ")");
                this.syncFakePlayerInventory(slotIdx);
                return false;
            }
        } else if (blockHit.getType() != HitResult.Type.MISS) {
            InteractionResult interactRes = player.gameMode.useItemOn(
                player, 
                this.level, 
                stack, 
                InteractionHand.MAIN_HAND, 
                blockHit
            );
            if (interactRes.consumesAction()) {
                me.tuanzi.util.ModLog.debug("[玩家模拟器] 右键使用方块/物品成功，方块位置: " + blockHit.getBlockPos() + " (结果: " + interactRes + ")");
                this.syncFakePlayerInventory(slotIdx);
                return false;
            }
        }

        // 右键空气
        InteractionResult useAirRes = stack.use(this.level, player, InteractionHand.MAIN_HAND);
        if (useAirRes.consumesAction()) {
            me.tuanzi.util.ModLog.debug("[玩家模拟器] 右键空气成功 (结果: " + useAirRes + ")");
            this.syncFakePlayerInventory(slotIdx);
            return false;
        }

        // ================== 右键无效，执行左键 ==================
        if (hitEntity && entityHit != null && entityHit.getEntity() instanceof LivingEntity) {
            // 不再即时攻击，而是挂起蓄力，等待充能结束时释放
            this.attackingEntityId = entityHit.getEntity().getId();
            this.actionTick = 0;
            me.tuanzi.util.ModLog.debug("[玩家模拟器] 发现攻击目标，开启持续充能攻击挂起，目标: " + entityHit.getEntity().getType().getDescription().getString() + "，目标ID: " + this.attackingEntityId);
            return true; // 返回 true 挂起状态机运行
        } else if (blockHit.getType() != HitResult.Type.MISS) {
            BlockPos targetBlockPos = blockHit.getBlockPos();
            BlockState blockState = this.level.getBlockState(targetBlockPos);
            if (!blockState.isAir() && blockState.getDestroySpeed(this.level, targetBlockPos) >= 0.0f) {
                float dmgSpeed = blockState.getDestroyProgress(player, this.level, targetBlockPos);
                if (dmgSpeed >= 1.0f) {
                    // 瞬间挖掘：手动扣除耐久、结算时运/精准等全部工具掉落物并生成
                    me.tuanzi.util.ModLog.debug("[玩家模拟器] 右键无效，执行瞬间挖掘方块: " + targetBlockPos + " (挖掘进度: " + dmgSpeed + ")");
                    
                    if (this.level instanceof ServerLevel serverLevel) {
                        BlockEntity mineBe = this.level.getBlockEntity(targetBlockPos);
                        boolean canDrop = player.hasCorrectToolForDrops(blockState);
                        if (canDrop) {
                            java.util.List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(blockState, serverLevel, targetBlockPos, mineBe, player, stack);
                            drops.forEach(drop -> net.minecraft.world.level.block.Block.popResource(this.level, targetBlockPos, drop));
                        }
                        if (stack.isDamageableItem()) {
                            me.tuanzi.util.ModLog.debug("[玩家模拟器] 触发瞬间挖掘耐久计算。物品: " + stack + ", 目标方块: " + blockState);
                            stack.mineBlock(serverLevel, blockState, targetBlockPos, player);
                        }
                    }
                    this.level.destroyBlock(targetBlockPos, false, player);
                    
                    this.syncFakePlayerInventory(slotIdx);
                    return false;
                } else {
                    // 开启持续挖掘
                    me.tuanzi.util.ModLog.debug("[玩家模拟器] 右键无效，执行左键开启挂起持续挖掘方块: " + targetBlockPos + " (初始挖掘速度: " + dmgSpeed + ")");
                    this.miningPos = targetBlockPos.immutable();
                    this.miningProgress = 0.0f;
                    this.actionTick = 0;
                    this.level.destroyBlockProgress(player.getId(), this.miningPos, 0);
                    return true;
                }
            }
        }

        // 没有任何操作成功，把物品放回
        me.tuanzi.util.ModLog.debug("[玩家模拟器] 没有任何操作成功，回写并结束触发。");
        this.syncFakePlayerInventory(slotIdx);
        return false;
    }

    private void executeOwnerAttackAndReset(Level level, SimulatorFakePlayer fakePlayer) {
        net.minecraft.world.entity.Entity target = level.getEntity(this.attackingEntityId);
        if (target != null && target.isAlive() && target.distanceToSqr(fakePlayer) <= 36.0) {
            // 确保假玩家手持当前物品，并刷新属性修改器
            ItemStack toolStack = this.items.get(this.currentSlot);
            ItemStack prevTool = fakePlayer.getMainHandItem().copy();
            fakePlayer.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, toolStack);
            fakePlayer.updateEquipmentModifiers(prevTool, toolStack);
            fakePlayer.getInventory().setItem(0, toolStack);
            fakePlayer.getInventory().setSelectedSlot(0);

            // 写入充能 tick 并攻击
            fakePlayer.setAttackStrengthTicker(this.chargeTicks);
            me.tuanzi.util.ModLog.debug("[玩家模拟器] 充能结束，执行重定向物理攻击！手持: " + toolStack + "，当前充能Ticks: " + this.chargeTicks + "，攻击强度比例: " + fakePlayer.getAttackStrengthScale(0.5f));
            fakePlayer.attack(target);
        } else {
            me.tuanzi.util.ModLog.debug("[玩家模拟器] 攻击释放失败：目标已消失或超出范围");
        }

        // 重置状态
        this.chargeTicks = 0;
        this.attackingEntityId = -1;
        this.syncFakePlayerInventory(this.currentSlot);
        this.currentSlot = (this.currentSlot + 1) % 9;
        this.running = false;
        this.cooldown = 2;
        this.setChanged();
    }


    // 将假玩家物品栏的变动和返回残留物写回模拟器
    private void syncFakePlayerInventory(int slotIdx) {
        if (this.fakePlayer == null) return;

        // 主手物品归位
        this.setItem(slotIdx, this.fakePlayer.getMainHandItem());

        // 多余物品塞入模拟器或丢在世界
        for (int i = 1; i < this.fakePlayer.getInventory().getContainerSize(); i++) {
            ItemStack extra = this.fakePlayer.getInventory().getItem(i);
            if (!extra.isEmpty()) {
                ItemStack remaining = this.insertItemIntoSimulator(extra);
                if (!remaining.isEmpty()) {
                    Direction facing = this.getBlockState().getValue(PlayerSimulatorBlock.FACING);
                    BlockPos spawnPos = this.worldPosition.relative(facing);
                    net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                        this.level, 
                        spawnPos.getX() + 0.5, 
                        spawnPos.getY() + 0.5, 
                        spawnPos.getZ() + 0.5, 
                        remaining
                    );
                    this.level.addFreshEntity(itemEntity);
                }
                this.fakePlayer.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
        this.setChanged();
    }

    private ItemStack insertItemIntoSimulator(ItemStack stack) {
        ItemStack copy = stack.copy();
        // 尝试合并
        for (int i = 0; i < 9; i++) {
            ItemStack existing = this.getItem(i);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, copy)) {
                int maxCount = Math.min(this.getMaxStackSize(), existing.getMaxStackSize());
                int toAdd = Math.min(copy.getCount(), maxCount - existing.getCount());
                if (toAdd > 0) {
                    existing.grow(toAdd);
                    copy.shrink(toAdd);
                    this.setChanged();
                    if (copy.isEmpty()) {
                        return ItemStack.EMPTY;
                    }
                }
            }
        }
        // 尝试放入空槽
        for (int i = 0; i < 9; i++) {
            ItemStack existing = this.getItem(i);
            if (existing.isEmpty()) {
                int maxCount = Math.min(this.getMaxStackSize(), copy.getMaxStackSize());
                if (copy.getCount() > maxCount) {
                    this.setItem(i, copy.split(maxCount));
                } else {
                    this.setItem(i, copy);
                    return ItemStack.EMPTY;
                }
            }
        }
        return copy;
    }

    // 状态机 tick 更新
    public static void serverTick(Level level, BlockPos pos, BlockState state, PlayerSimulatorBlockEntity blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        // 0. 递增充能计时器 (仅在接收到红石充能信号时递增，上限设为 200 ticks 防止溢出)
        if (level.hasNeighborSignal(pos)) {
            if (blockEntity.chargeTicks < 200) {
                blockEntity.chargeTicks++;
            }
        }

        // 1. 冷却倒计时更新
        if (blockEntity.cooldown > 0) {
            blockEntity.cooldown--;
            if (blockEntity.cooldown == 0 && !blockEntity.running) {
                // 冷却结束，释放红石触发状态
                level.setBlock(pos, state.setValue(PlayerSimulatorBlock.TRIGGERED, false), 2);
            }
        }

        // 2. 状态机处于运行中更新
        if (blockEntity.running) {
            SimulatorFakePlayer fakePlayer = blockEntity.getOrLoadFakePlayer();

            // A. 持续挖掘判定
            if (blockEntity.miningPos != null) {
                // 当其破坏方块时，没在充能就停止破坏，在充能时则持续破坏
                if (!level.hasNeighborSignal(pos)) {
                    me.tuanzi.util.ModLog.debug("[玩家模拟器] 红石信号断开（没在充能），立即停止破坏方块: " + blockEntity.miningPos);
                    level.destroyBlockProgress(fakePlayer.getId(), blockEntity.miningPos, -1);
                    blockEntity.miningPos = null;
                    blockEntity.miningProgress = 0.0f;
                    blockEntity.syncFakePlayerInventory(blockEntity.currentSlot);
                    blockEntity.running = false;
                    blockEntity.cooldown = 2;
                    blockEntity.setChanged();
                    return;
                }

                BlockState targetState = level.getBlockState(blockEntity.miningPos);
                if (targetState.isAir()) {
                    // 方块已被破坏（可能由水流或者其他外部触发破坏）
                    level.destroyBlockProgress(fakePlayer.getId(), blockEntity.miningPos, -1);
                    blockEntity.miningPos = null;
                    blockEntity.miningProgress = 0.0f;
                    blockEntity.syncFakePlayerInventory(blockEntity.currentSlot);
                    blockEntity.currentSlot = (blockEntity.currentSlot + 1) % 9;
                    blockEntity.running = false;
                    blockEntity.cooldown = 2;
                    blockEntity.setChanged();
                } else {
                    blockEntity.actionTick++;
                    // 在每 tick 计算进度前，确保假玩家主手持枪/手持镐，并同步加载其属性修改器以获得实际破坏速度
                    ItemStack toolStack = blockEntity.items.get(blockEntity.currentSlot);
                    ItemStack prevTool = fakePlayer.getMainHandItem().copy();
                    fakePlayer.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, toolStack);
                    fakePlayer.updateEquipmentModifiers(prevTool, toolStack);
                    fakePlayer.getInventory().setItem(0, toolStack);
                    fakePlayer.getInventory().setSelectedSlot(0);
                    // 强制假玩家为踩在地上状态，彻底避开浮空带来的 5 倍挖掘慢速惩罚！
                    fakePlayer.setOnGround(true);

                    // 持续增加进度
                    float speed = targetState.getDestroyProgress(fakePlayer, level, blockEntity.miningPos);
                    blockEntity.miningProgress += speed;
                    int animStage = (int)(blockEntity.miningProgress * 10.0f);
                    level.destroyBlockProgress(fakePlayer.getId(), blockEntity.miningPos, animStage >= 0 && animStage < 10 ? animStage : -1);

                    if (blockEntity.miningProgress >= 1.0f) {
                        // 破坏完成：手动扣除耐久、结算时运/精准等全部工具掉落物并生成
                        level.destroyBlockProgress(fakePlayer.getId(), blockEntity.miningPos, -1);
                        if (level instanceof ServerLevel serverLevel) {
                            BlockEntity mineBe = level.getBlockEntity(blockEntity.miningPos);
                            boolean canDrop = fakePlayer.hasCorrectToolForDrops(targetState);
                            if (canDrop) {
                                java.util.List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(targetState, serverLevel, blockEntity.miningPos, mineBe, fakePlayer, toolStack);
                                drops.forEach(drop -> net.minecraft.world.level.block.Block.popResource(level, blockEntity.miningPos, drop));
                            }
                            if (toolStack.isDamageableItem()) {
                                me.tuanzi.util.ModLog.debug("[玩家模拟器] 触发持续挖掘完成耐久计算。物品: " + toolStack + ", 目标方块: " + targetState);
                                toolStack.mineBlock(serverLevel, targetState, blockEntity.miningPos, fakePlayer);
                            }
                        }
                        level.destroyBlock(blockEntity.miningPos, false, fakePlayer);

                        blockEntity.miningPos = null;
                        blockEntity.miningProgress = 0.0f;
                        blockEntity.syncFakePlayerInventory(blockEntity.currentSlot);
                        blockEntity.currentSlot = (blockEntity.currentSlot + 1) % 9;
                        blockEntity.running = false;
                        blockEntity.cooldown = 2;
                        blockEntity.setChanged();
                    }
                }
            }
            // B. 武器蓄力判定 (拉弓/三叉戟等右键蓄力)
            else if (blockEntity.targetChargeTicks > 0) {
                blockEntity.actionTick++;
                // 假玩家更新使用状态，确保使用时间递减
                fakePlayer.publicUpdateUsingItem();

                if (blockEntity.actionTick >= blockEntity.targetChargeTicks) {
                    // 蓄力完毕，释放！
                    fakePlayer.releaseUsingItem();
                    blockEntity.targetChargeTicks = 0;
                    blockEntity.syncFakePlayerInventory(blockEntity.currentSlot);
                    blockEntity.currentSlot = (blockEntity.currentSlot + 1) % 9;
                    blockEntity.running = false;
                    blockEntity.cooldown = 2;
                    blockEntity.setChanged();
                }
            }
            // C. 持续充能左键攻击判定 (蓄力攻击生物，红石信号断开时释放伤害)
            else if (blockEntity.attackingEntityId != -1) {
                if (level.hasNeighborSignal(pos)) {
                    blockEntity.actionTick++;
                    // 蓄力上限保护：如果充能了 200 ticks，强制自动打出去
                    if (blockEntity.chargeTicks >= 200) {
                        blockEntity.executeOwnerAttackAndReset(level, fakePlayer);
                    }
                } else {
                    // 充能结束 (红石信号断开)，释放全力一击！
                    blockEntity.executeOwnerAttackAndReset(level, fakePlayer);
                }
            }
        }
    }

    // ================== WorldlyContainer & Container 接口实现 ==================

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack itemStack, @Nullable Direction direction) {
        // 允许从顶面和侧面注入物品，底面不可输入
        return direction != Direction.DOWN;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack itemStack, Direction direction) {
        // 允许从底面抽取结果
        return direction == Direction.DOWN;
    }

    @Override
    public int getContainerSize() {
        return 9;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(this.items, slot, amount);
        if (!result.isEmpty()) {
            this.setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        if (stack.getCount() > this.getMaxStackSize()) {
            stack.setCount(this.getMaxStackSize());
        }
        this.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.items.clear();
        this.setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.tuanzis_mod.player_simulator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new PlayerSimulatorMenu(containerId, playerInventory, this);
    }

    // ================== NBT 存取 ==================

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, this.items);
        this.running = input.getBooleanOr("Running", false);
        this.currentSlot = input.getIntOr("CurrentSlot", 0);
        this.cooldown = input.getIntOr("Cooldown", 0);
        this.actionTick = input.getIntOr("ActionTick", 0);
        this.miningPos = input.read("MiningPos", BlockPos.CODEC).orElse(null);
        this.miningProgress = input.getFloatOr("MiningProgress", 0.0f);
        this.targetChargeTicks = input.getIntOr("TargetChargeTicks", 0);
        this.chargeTicks = input.getIntOr("ChargeTicks", 0);

        String uuidStr = input.getStringOr("OwnerUUID", "");
        if (!uuidStr.isEmpty()) {
            try {
                this.ownerUuid = java.util.UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                this.ownerUuid = null;
            }
        } else {
            this.ownerUuid = null;
        }
        this.ownerName = input.getStringOr("OwnerName", "");
        this.attackingEntityId = input.getIntOr("AttackingEntityId", -1);
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        output.putBoolean("Running", this.running);
        output.putInt("CurrentSlot", this.currentSlot);
        output.putInt("Cooldown", this.cooldown);
        output.putInt("ActionTick", this.actionTick);
        if (this.miningPos != null) {
            output.store("MiningPos", BlockPos.CODEC, this.miningPos);
        }
        output.putFloat("MiningProgress", this.miningProgress);
        output.putInt("TargetChargeTicks", this.targetChargeTicks);
        output.putInt("ChargeTicks", this.chargeTicks);

        if (this.ownerUuid != null) {
            output.putString("OwnerUUID", this.ownerUuid.toString());
        }
        output.putString("OwnerName", this.ownerName);
        output.putInt("AttackingEntityId", this.attackingEntityId);
    }
}
