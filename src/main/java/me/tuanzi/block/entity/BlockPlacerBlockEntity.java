package me.tuanzi.block.entity;

import me.tuanzi.block.BlockPlacerBlock;
import me.tuanzi.block.PlacerLampState;
import me.tuanzi.init.ModBlockTags;
import me.tuanzi.init.ModBlocks;
import me.tuanzi.network.BlockPlacerActionPacket;
import me.tuanzi.util.ModLog;
import me.tuanzi.util.SimulatorFakePlayer;
import me.tuanzi.world.inventory.BlockPlacerMenu;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 方块放置器方块实体
 */
public class BlockPlacerBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer {
    public static final int FILTER_SLOT = 0;
    public static final int STORAGE_START = 1;
    public static final int STORAGE_COUNT = 9;
    public static final int TOTAL_SLOTS = 10;

    // 红石触发模式
    public static final int MODE_SINGLE = 0;      // 单次/边沿（默认）
    public static final int MODE_CONTINUOUS = 1;  // 连续/周期
    public static final int MODE_PULSE_LATCH = 2; // 脉冲/锁存

    // 连续模式可选间隔（tick）
    public static final int[] INTERVAL_OPTIONS = new int[]{4, 10, 20};
    public static final int DEFAULT_INTERVAL = 20;
    public static final int LATCH_COOLDOWN_TICKS = 2;

    // 每 tick 全局触发限频
    public static final int MAX_TRIGGERS_PER_TICK = 64;
    private static long lastLimitTick = Long.MIN_VALUE;
    private static int triggersInCurrentTick = 0;

    // 状态码定义
    public static final int STATUS_READY = 0;           // 绿灯：就绪
    public static final int STATUS_SUCCESS = 1;         // 绿灯：放置成功
    public static final int STATUS_NO_ITEM = 2;         // 黄灯：缺料
    public static final int STATUS_FILTER_MISMATCH = 3; // 黄灯：过滤不匹配
    public static final int STATUS_TARGET_BLOCKED = 4;  // 红灯：目标不可放置/不可替换
    public static final int STATUS_COLLISION = 5;       // 红灯：碰撞阻挡
    public static final int STATUS_PROTECTED = 6;       // 红灯：被保护拒绝
    public static final int STATUS_COOLDOWN = 7;        // 黄灯：冷却中/限频

    private static final int[] STORAGE_SLOTS = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9};

    private final NonNullList<ItemStack> items = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);

    // 可持久化配置与运行状态
    private int redstoneMode = MODE_SINGLE;
    private int continuousInterval = DEFAULT_INTERVAL;
    private boolean simulateSneak = true;
    private boolean allowBottomExtract = true;
    private int statusCode = STATUS_READY;
    private boolean wasPowered = false;
    private boolean latched = false;
    private int latchCooldown = 0;
    private int continuousTimer = 0;

    private @Nullable SimulatorFakePlayer fakePlayer = null;

    // 菜单同步数据 (0: 模式, 1: 连续间隔, 2: 模拟潜行, 3: 底面抽出, 4: 状态码, 5..6: X, 7: Y, 8..9: Z)
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> BlockPlacerBlockEntity.this.redstoneMode;
                case 1 -> BlockPlacerBlockEntity.this.continuousInterval;
                case 2 -> BlockPlacerBlockEntity.this.simulateSneak ? 1 : 0;
                case 3 -> BlockPlacerBlockEntity.this.allowBottomExtract ? 1 : 0;
                case 4 -> BlockPlacerBlockEntity.this.statusCode;
                case 5 -> BlockPlacerBlockEntity.this.worldPosition.getX() & 0xFFFF;
                case 6 -> BlockPlacerBlockEntity.this.worldPosition.getX() >>> 16;
                case 7 -> BlockPlacerBlockEntity.this.worldPosition.getY();
                case 8 -> BlockPlacerBlockEntity.this.worldPosition.getZ() & 0xFFFF;
                case 9 -> BlockPlacerBlockEntity.this.worldPosition.getZ() >>> 16;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> BlockPlacerBlockEntity.this.redstoneMode = clampMode(value);
                case 1 -> BlockPlacerBlockEntity.this.continuousInterval = normalizeInterval(value);
                case 2 -> BlockPlacerBlockEntity.this.simulateSneak = (value == 1);
                case 3 -> BlockPlacerBlockEntity.this.allowBottomExtract = (value == 1);
                case 4 -> BlockPlacerBlockEntity.this.statusCode = value;
            }
        }

        @Override
        public int getCount() {
            return 10;
        }
    };

    public BlockPlacerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.BLOCK_PLACER_BLOCK_ENTITY, pos, state);
    }

    public static int clampMode(int mode) {
        if (mode < MODE_SINGLE || mode > MODE_PULSE_LATCH) {
            return MODE_SINGLE;
        }
        return mode;
    }

    public static int normalizeInterval(int interval) {
        for (int option : INTERVAL_OPTIONS) {
            if (option == interval) {
                return option;
            }
        }
        return DEFAULT_INTERVAL;
    }

    public static int nextInterval(int current) {
        return switch (current) {
            case 4 -> 10;
            case 10 -> 20;
            default -> 4;
        };
    }

    public static PlacerLampState getLampForStatus(int statusCode) {
        return switch (statusCode) {
            case STATUS_READY, STATUS_SUCCESS -> PlacerLampState.GREEN;
            case STATUS_NO_ITEM, STATUS_FILTER_MISMATCH, STATUS_COOLDOWN -> PlacerLampState.YELLOW;
            case STATUS_TARGET_BLOCKED, STATUS_COLLISION, STATUS_PROTECTED -> PlacerLampState.RED;
            default -> PlacerLampState.GREEN;
        };
    }

    private SimulatorFakePlayer getOrLoadFakePlayer(ServerLevel serverLevel) {
        if (this.fakePlayer == null || this.fakePlayer.level() != serverLevel) {
            this.fakePlayer = new SimulatorFakePlayer(serverLevel);
        }
        return this.fakePlayer;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BlockPlacerBlockEntity be) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!serverLevel.isLoaded(pos)) {
            return;
        }

        if (be.latchCooldown > 0) {
            be.latchCooldown--;
            if (be.latchCooldown == 0 && be.statusCode == STATUS_COOLDOWN) {
                be.updateStatus(serverLevel, pos, state, STATUS_READY);
            }
        }

        if (be.redstoneMode == MODE_CONTINUOUS) {
            Direction facing = state.getValue(BlockPlacerBlock.FACING);
            boolean powered = BlockPlacerBlock.hasNonFrontSignal(serverLevel, pos, facing);
            if (powered) {
                if (!be.wasPowered) {
                    // 兜底捕获未走 neighborChanged 的初始高电平上升沿
                    be.wasPowered = true;
                    be.continuousTimer = 0;
                    be.setTriggeredProperty(serverLevel, pos, true);
                    be.tryPlace(serverLevel, pos, serverLevel.getBlockState(pos));
                } else {
                    be.continuousTimer++;
                    if (be.continuousTimer >= be.continuousInterval) {
                        be.continuousTimer = 0;
                        be.tryPlace(serverLevel, pos, serverLevel.getBlockState(pos));
                    }
                }
            } else if (be.wasPowered) {
                be.wasPowered = false;
                be.continuousTimer = 0;
                be.setTriggeredProperty(serverLevel, pos, false);
            }
        }
    }

    /**
     * 响应邻居红石状态变更
     */
    public void onNeighborSignalChanged(BlockState state) {
        if (!(this.level instanceof ServerLevel serverLevel) || !serverLevel.isLoaded(this.worldPosition)) {
            return;
        }

        Direction facing = state.getValue(BlockPlacerBlock.FACING);
        boolean powered = BlockPlacerBlock.hasNonFrontSignal(serverLevel, this.worldPosition, facing);

        if (!this.wasPowered && powered) {
            // 上升沿
            this.wasPowered = true;
            setTriggeredProperty(serverLevel, this.worldPosition, true);
            ModLog.debug("[方块放置器] 检测到红石上升沿，位置=" + this.worldPosition + "，模式=" + this.redstoneMode);

            switch (this.redstoneMode) {
                case MODE_SINGLE -> tryPlace(serverLevel, this.worldPosition, serverLevel.getBlockState(this.worldPosition));
                case MODE_CONTINUOUS -> {
                    this.continuousTimer = 0;
                    tryPlace(serverLevel, this.worldPosition, serverLevel.getBlockState(this.worldPosition));
                }
                case MODE_PULSE_LATCH -> {
                    if (!this.latched && this.latchCooldown <= 0) {
                        this.latched = true;
                        tryPlace(serverLevel, this.worldPosition, serverLevel.getBlockState(this.worldPosition));
                    } else if (this.latchCooldown > 0) {
                        ModLog.debug("[方块放置器] 锁存冷却中跳过触发，剩余=" + this.latchCooldown + "t");
                        updateStatus(serverLevel, this.worldPosition, serverLevel.getBlockState(this.worldPosition), STATUS_COOLDOWN);
                    }
                }
            }
        } else if (this.wasPowered && !powered) {
            // 下降沿
            this.wasPowered = false;
            this.continuousTimer = 0;
            setTriggeredProperty(serverLevel, this.worldPosition, false);

            if (this.redstoneMode == MODE_PULSE_LATCH && this.latched) {
                this.latched = false;
                this.latchCooldown = LATCH_COOLDOWN_TICKS;
                ModLog.debug("[方块放置器] 锁存信号断开，进入 " + LATCH_COOLDOWN_TICKS + " tick 冷却，位置=" + this.worldPosition);
            }
        }
        this.setChanged();
    }

    private void setTriggeredProperty(ServerLevel serverLevel, BlockPos pos, boolean triggered) {
        BlockState current = serverLevel.getBlockState(pos);
        if (current.getBlock() instanceof BlockPlacerBlock && current.getValue(BlockPlacerBlock.TRIGGERED) != triggered) {
            serverLevel.setBlock(pos, current.setValue(BlockPlacerBlock.TRIGGERED, triggered), 2);
        }
    }

    private void updateStatus(ServerLevel serverLevel, BlockPos pos, BlockState state, int newStatus) {
        this.statusCode = newStatus;
        PlacerLampState targetLamp = getLampForStatus(newStatus);
        BlockState current = serverLevel.getBlockState(pos);
        if (current.getBlock() instanceof BlockPlacerBlock && current.getValue(BlockPlacerBlock.LAMP_STATE) != targetLamp) {
            serverLevel.setBlock(pos, current.setValue(BlockPlacerBlock.LAMP_STATE, targetLamp), 3);
        }
        this.setChanged();
    }

    /**
     * 核心放置逻辑（全部在服务端执行）
     */
    public void tryPlace(ServerLevel serverLevel, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof BlockPlacerBlock)) {
            return;
        }
        if (!serverLevel.isLoaded(pos)) {
            return;
        }

        // 每 tick 全局触发限频，防止大批放置器同时工作卡服
        long gameTime = serverLevel.getGameTime();
        if (lastLimitTick != gameTime) {
            lastLimitTick = gameTime;
            triggersInCurrentTick = 0;
        }
        if (triggersInCurrentTick >= MAX_TRIGGERS_PER_TICK) {
            ModLog.debug("[方块放置器] 达到单 tick 全局触发上限 (" + MAX_TRIGGERS_PER_TICK + ")，本次跳过: " + pos);
            updateStatus(serverLevel, pos, state, STATUS_COOLDOWN);
            return;
        }
        triggersInCurrentTick++;

        Direction facing = state.getValue(BlockPlacerBlock.FACING);
        BlockPos targetPos = pos.relative(facing);
        if (!serverLevel.isLoaded(targetPos) || !serverLevel.isInWorldBounds(targetPos)) {
            ModLog.debug("[方块放置器] 目标位置未加载或超出建筑高度: " + targetPos);
            updateStatus(serverLevel, pos, state, STATUS_TARGET_BLOCKED);
            return;
        }

        // 1. 按从左到右 (1..9) 扫描库存中第一个匹配过滤且合法的 BlockItem
        int chosenSlot = -1;
        boolean hasAnyItem = false;
        for (int i = STORAGE_START; i < TOTAL_SLOTS; i++) {
            ItemStack candidate = this.items.get(i);
            if (!candidate.isEmpty()) {
                hasAnyItem = true;
                if (matchesFilter(candidate)) {
                    chosenSlot = i;
                    break;
                }
            }
        }

        if (chosenSlot == -1) {
            int failStatus = hasAnyItem ? STATUS_FILTER_MISMATCH : STATUS_NO_ITEM;
            ModLog.debug("[方块放置器] 无可放置方块（" + (hasAnyItem ? "过滤不匹配/黑名单" : "缺料") + "），位置=" + pos);
            updateStatus(serverLevel, pos, state, failStatus);
            return;
        }

        ItemStack chosenStack = this.items.get(chosenSlot);
        if (!(chosenStack.getItem() instanceof BlockItem blockItem)) {
            updateStatus(serverLevel, pos, state, STATUS_FILTER_MISMATCH);
            return;
        }

        // 2. 构建假玩家上下文：位置在放置器中心，视线朝向放置器正面，手持物品副本，潜行状态按配置
        SimulatorFakePlayer fakePlayer = getOrLoadFakePlayer(serverLevel);
        double px = pos.getX() + 0.5;
        double py = pos.getY() + 0.5 - fakePlayer.getEyeHeight();
        double pz = pos.getZ() + 0.5;
        fakePlayer.setPos(px, py, pz);

        float yaw = facing.getAxis().isVertical() ? 0.0f : facing.toYRot();
        float pitch = facing == Direction.DOWN ? 90.0f : (facing == Direction.UP ? -90.0f : 0.0f);
        fakePlayer.setYRot(yaw);
        fakePlayer.setXRot(pitch);
        fakePlayer.yRotO = yaw;
        fakePlayer.xRotO = pitch;
        fakePlayer.setYHeadRot(yaw);
        fakePlayer.setShiftKeyDown(this.simulateSneak);

        ItemStack handCopy = chosenStack.copyWithCount(1);
        fakePlayer.setItemSlot(EquipmentSlot.MAINHAND, handCopy);

        Vec3 hitLocation = new Vec3(
                pos.getX() + 0.5 + facing.getStepX() * 0.5,
                pos.getY() + 0.5 + facing.getStepY() * 0.5,
                pos.getZ() + 0.5 + facing.getStepZ() * 0.5
        );
        BlockHitResult placeHit = new BlockHitResult(hitLocation, facing, pos, false);
        BlockPlaceContext placeContext = new BlockPlaceContext(serverLevel, fakePlayer, InteractionHand.MAIN_HAND, handCopy, placeHit);

        // 3. 目标位置可替换性/方块实体检查
        BlockState currentTargetState = serverLevel.getBlockState(targetPos);
        if (currentTargetState.hasBlockEntity() || !currentTargetState.canBeReplaced(placeContext)) {
            fakePlayer.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            ModLog.debug("[方块放置器] 目标格不可替换或含方块实体: " + targetPos + " (" + currentTargetState + ")");
            updateStatus(serverLevel, pos, state, STATUS_TARGET_BLOCKED);
            return;
        }

        // 4. 原版区域权限与保护事件检查
        if (!serverLevel.mayInteract(fakePlayer, targetPos)) {
            fakePlayer.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            ModLog.debug("[方块放置器] mayInteract 被拒绝: " + targetPos);
            updateStatus(serverLevel, pos, state, STATUS_PROTECTED);
            return;
        }

        BlockHitResult protectHit = new BlockHitResult(hitLocation, facing.getOpposite(), targetPos, false);
        InteractionResult protectResult = UseBlockCallback.EVENT.invoker().interact(fakePlayer, serverLevel, InteractionHand.MAIN_HAND, protectHit);
        if (protectResult == InteractionResult.FAIL) {
            fakePlayer.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            ModLog.debug("[方块放置器] UseBlockCallback 保护事件取消放置: " + targetPos);
            updateStatus(serverLevel, pos, state, STATUS_PROTECTED);
            return;
        }

        // 5. 预判原版支撑与实体碰撞，区分失败原因
        BlockPlaceContext updatedContext = blockItem.updatePlacementContext(placeContext);
        if (updatedContext == null) {
            fakePlayer.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            ModLog.debug("[方块放置器] updatePlacementContext 返回空，不可放置: " + targetPos);
            updateStatus(serverLevel, pos, state, STATUS_TARGET_BLOCKED);
            return;
        }

        BlockPos finalPlacePos = updatedContext.getClickedPos();
        BlockState finalTargetState = serverLevel.getBlockState(finalPlacePos);
        if (finalTargetState.hasBlockEntity() || !updatedContext.canPlace()) {
            fakePlayer.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            updateStatus(serverLevel, pos, state, STATUS_TARGET_BLOCKED);
            return;
        }

        BlockState placementState = blockItem.getBlock().getStateForPlacement(updatedContext);
        if (placementState == null || !placementState.canSurvive(serverLevel, finalPlacePos)) {
            fakePlayer.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            ModLog.debug("[方块放置器] 无法获得有效放置状态或不满足支撑条件: " + finalPlacePos);
            updateStatus(serverLevel, pos, state, STATUS_TARGET_BLOCKED);
            return;
        }

        if (!serverLevel.isUnobstructed(placementState, finalPlacePos, CollisionContext.placementContext(fakePlayer))) {
            fakePlayer.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            ModLog.debug("[方块放置器] 目标位置存在实体碰撞阻挡: " + finalPlacePos);
            updateStatus(serverLevel, pos, state, STATUS_COLLISION);
            return;
        }

        // 6. 调用原版 BlockItem.place（使用副本，成功后再从库存扣除 1 个）
        InteractionResult placeResult = blockItem.place(placeContext);
        fakePlayer.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);

        if (placeResult.consumesAction()) {
            chosenStack.shrink(1);
            BlockState placedState = serverLevel.getBlockState(finalPlacePos);
            serverLevel.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, placedState),
                    finalPlacePos.getX() + 0.5,
                    finalPlacePos.getY() + 0.5,
                    finalPlacePos.getZ() + 0.5,
                    6,
                    0.25, 0.25, 0.25,
                    0.05
            );
            ModLog.debug("[方块放置器] 放置成功: " + placedState + " at " + finalPlacePos + " (槽位=" + chosenSlot + ", 潜行=" + this.simulateSneak + ")");
            updateStatus(serverLevel, pos, state, STATUS_SUCCESS);
        } else {
            ModLog.debug("[方块放置器] 原版 BlockItem.place 返回失败: " + placeResult + " at " + finalPlacePos);
            updateStatus(serverLevel, pos, state, STATUS_TARGET_BLOCKED);
        }
    }

    /**
     * 判断物品是否为允许放入且不在安全黑名单中的 BlockItem
     */
    public static boolean isSafeBlockItem(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        Block block = blockItem.getBlock();
        if (block.defaultDestroyTime() < 0.0f) {
            return false;
        }
        try {
            return !block.defaultBlockState().is(ModBlockTags.BLOCK_PLACER_BLACKLIST);
        } catch (IllegalStateException ignored) {
            return true;
        }
    }

    /**
     * 结合过滤槽白名单与安全黑名单校验物品
     */
    public boolean matchesFilter(ItemStack stack) {
        return matchesFilter(stack, this.items.get(FILTER_SLOT));
    }

    public static boolean matchesFilter(ItemStack stack, ItemStack filterStack) {
        if (!isSafeBlockItem(stack)) {
            return false;
        }
        if (filterStack.isEmpty()) {
            return true;
        }
        return stack.is(filterStack.getItem());
    }

    /**
     * 处理来自 GUI 的设置操作
     */
    public void handleGuiAction(int action) {
        switch (action) {
            case BlockPlacerActionPacket.ACTION_CYCLE_MODE -> {
                this.redstoneMode = (this.redstoneMode + 1) % 3;
                this.latched = false;
                this.latchCooldown = 0;
                this.continuousTimer = 0;
                ModLog.debug("[方块放置器] 切换红石模式为: " + this.redstoneMode);
            }
            case BlockPlacerActionPacket.ACTION_CYCLE_INTERVAL -> {
                this.continuousInterval = nextInterval(this.continuousInterval);
                this.continuousTimer = 0;
                ModLog.debug("[方块放置器] 切换连续间隔为: " + this.continuousInterval + " tick");
            }
            case BlockPlacerActionPacket.ACTION_TOGGLE_SNEAK -> {
                this.simulateSneak = !this.simulateSneak;
                ModLog.debug("[方块放置器] 切换模拟潜行状态为: " + this.simulateSneak);
            }
            case BlockPlacerActionPacket.ACTION_TOGGLE_EXTRACT -> {
                this.allowBottomExtract = !this.allowBottomExtract;
                ModLog.debug("[方块放置器] 切换允许底面抽出为: " + this.allowBottomExtract);
            }
            default -> {
                return;
            }
        }
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    /**
     * 比较器输出：仅反映 1~9 格库存占用比例（0~15）
     */
    public int getComparatorSignal() {
        return computeComparatorSignal(this.items.subList(STORAGE_START, TOTAL_SLOTS));
    }

    public static int computeComparatorSignal(List<ItemStack> storageSlots) {
        int slotCount = storageSlots.size();
        if (slotCount == 0) {
            return 0;
        }
        float totalRatio = 0.0f;
        boolean hasAny = false;
        for (ItemStack stack : storageSlots) {
            if (!stack.isEmpty()) {
                hasAny = true;
                totalRatio += (float) stack.getCount() / (float) Math.max(1, stack.getMaxStackSize());
            }
        }
        if (!hasAny) {
            return 0;
        }
        return 1 + (int) ((totalRatio / (float) slotCount) * 14.0f);
    }

    // ================== WorldlyContainer 漏斗交互 ==================

    @Override
    public int[] getSlotsForFace(Direction side) {
        return STORAGE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        if (slot < STORAGE_START || slot >= TOTAL_SLOTS) {
            return false;
        }
        if (side == Direction.DOWN) {
            return false;
        }
        return matchesFilter(stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (slot < STORAGE_START || slot >= TOTAL_SLOTS) {
            return false;
        }
        return side == Direction.DOWN && this.allowBottomExtract;
    }

    // ================== 基础 Container 实现 ==================

    @Override
    public int getContainerSize() {
        return TOTAL_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                return false;
            }
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
        int limit = (slot == FILTER_SLOT) ? 1 : this.getMaxStackSize();
        if (stack.getCount() > limit) {
            stack.setCount(limit);
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
        return Component.translatable("container.tuanzis_mod.block_placer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BlockPlacerMenu(containerId, playerInventory, this, this.data);
    }

    // ================== NBT 存取 ==================

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        this.items.clear();
        ContainerHelper.loadAllItems(input, this.items);
        this.redstoneMode = clampMode(input.getIntOr("RedstoneMode", MODE_SINGLE));
        this.continuousInterval = normalizeInterval(input.getIntOr("ContinuousInterval", DEFAULT_INTERVAL));
        this.simulateSneak = input.getBooleanOr("SimulateSneak", true);
        this.allowBottomExtract = input.getBooleanOr("AllowBottomExtract", true);
        this.statusCode = input.getIntOr("StatusCode", STATUS_READY);
        this.wasPowered = input.getBooleanOr("WasPowered", false);
        this.latched = input.getBooleanOr("Latched", false);
        this.latchCooldown = input.getIntOr("LatchCooldown", 0);
        this.continuousTimer = input.getIntOr("ContinuousTimer", 0);
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        output.putInt("RedstoneMode", this.redstoneMode);
        output.putInt("ContinuousInterval", this.continuousInterval);
        output.putBoolean("SimulateSneak", this.simulateSneak);
        output.putBoolean("AllowBottomExtract", this.allowBottomExtract);
        output.putInt("StatusCode", this.statusCode);
        output.putBoolean("WasPowered", this.wasPowered);
        output.putBoolean("Latched", this.latched);
        output.putInt("LatchCooldown", this.latchCooldown);
        output.putInt("ContinuousTimer", this.continuousTimer);
    }
}
