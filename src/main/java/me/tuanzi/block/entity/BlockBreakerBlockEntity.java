package me.tuanzi.block.entity;

import me.tuanzi.block.BlockBreakerBlock;
import me.tuanzi.init.ModBlockTags;
import me.tuanzi.init.ModBlocks;
import me.tuanzi.init.ModItemTags;
import me.tuanzi.util.ModLog;
import me.tuanzi.util.SimulatorFakePlayer;
import me.tuanzi.world.inventory.BlockBreakerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BlockBreakerBlockEntity extends BlockEntity implements MenuProvider, WorldlyContainer {
    public static final int TOOL_SLOT = 0;
    public static final int BUFFER_START = 1;
    public static final int BUFFER_COUNT = 9;
    public static final int TOTAL_SLOTS = 10;

    // 状态码定义
    public static final int STATUS_READY = 0;
    public static final int STATUS_MINING = 1;
    public static final int STATUS_NO_TOOL = 2;
    public static final int STATUS_TOOL_BROKEN = 3;
    public static final int STATUS_BUFFER_FULL = 4;
    public static final int STATUS_UNBREAKABLE = 5;
    public static final int STATUS_WRONG_TOOL_LEVEL = 6;
    public static final int STATUS_COOLDOWN = 7;

    private final NonNullList<ItemStack> items = NonNullList.withSize(TOTAL_SLOTS, ItemStack.EMPTY);

    // 运行参数与状态
    private boolean continuousMode = false; // false: 单次脉冲, true: 持续循环
    private int cooldown = 0;
    private @Nullable BlockPos miningPos = null;
    private float miningProgress = 0.0f;
    private int statusCode = STATUS_READY;

    // 活塞移动标记与延迟
    private boolean isMovingByPiston = false;
    private int delayAfterPistonMove = 0;
    private boolean hasTickedOnce = false;

    // 模拟假玩家
    private @Nullable SimulatorFakePlayer fakePlayer = null;

    // 菜单同步数据 (0: 模式, 1: 进度百分比, 2: 冷却, 3: 状态码, 4: pos.X, 5: pos.Y, 6: pos.Z)
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> BlockBreakerBlockEntity.this.continuousMode ? 1 : 0;
                case 1 -> (int) (BlockBreakerBlockEntity.this.miningProgress * 100.0f);
                case 2 -> BlockBreakerBlockEntity.this.cooldown;
                case 3 -> BlockBreakerBlockEntity.this.statusCode;
                case 4 -> BlockBreakerBlockEntity.this.worldPosition.getX();
                case 5 -> BlockBreakerBlockEntity.this.worldPosition.getY();
                case 6 -> BlockBreakerBlockEntity.this.worldPosition.getZ();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> BlockBreakerBlockEntity.this.continuousMode = (value == 1);
                case 1 -> BlockBreakerBlockEntity.this.miningProgress = value / 100.0f;
                case 2 -> BlockBreakerBlockEntity.this.cooldown = value;
                case 3 -> BlockBreakerBlockEntity.this.statusCode = value;
            }
        }

        @Override
        public int getCount() {
            return 7;
        }
    };

    public BlockBreakerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.BLOCK_BREAKER_BLOCK_ENTITY, pos, state);
        ModLog.debug("[方块破坏器创建] 实例: " + System.identityHashCode(this) + " at " + pos);
    }

    @Override
    public void setRemoved() {
        ModLog.debug("[方块破坏器移除] 实例: " + System.identityHashCode(this) + " at " + this.worldPosition);
        super.setRemoved();
    }

    public SimulatorFakePlayer getOrLoadFakePlayer(ServerLevel serverLevel) {
        if (this.fakePlayer == null) {
            this.fakePlayer = new SimulatorFakePlayer(serverLevel);
        }
        return this.fakePlayer;
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        ModLog.debug("[方块破坏器 setLevel] 实例: " + System.identityHashCode(this) + " at " + this.worldPosition);
        if (!level.isClientSide() && BlockBreakerPistonManager.hasPendingMigration(level.dimension(), this.worldPosition)) {
            CompoundTag tag = BlockBreakerPistonManager.getMigration(level.dimension(), this.worldPosition);
            if (tag != null) {
                this.loadFromTag(tag);
                ModLog.debug("[方块破坏器活塞移动] 在 setLevel 成功恢复迁移数据: " + this.worldPosition + " 实例: " + System.identityHashCode(this));
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BlockBreakerBlockEntity blockEntity) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!blockEntity.hasTickedOnce) {
            blockEntity.hasTickedOnce = true;
            ModLog.debug("[方块破坏器 serverTick 首次运行] 实例: " + System.identityHashCode(blockEntity) + " at " + pos + " items=" + blockEntity.items);
        }

        // 双重保障检查活塞数据迁移与落地确认
        if (BlockBreakerPistonManager.hasPendingMigration(serverLevel.dimension(), pos)) {
            CompoundTag tag = BlockBreakerPistonManager.getMigration(serverLevel.dimension(), pos);
            if (tag != null && blockEntity.isEmpty()) {
                blockEntity.loadFromTag(tag);
                ModLog.debug("[方块破坏器活塞移动] 在 serverTick 兜底恢复迁移数据: " + pos + " 实例: " + System.identityHashCode(blockEntity));
            }
            BlockBreakerPistonManager.confirmMigration(serverLevel.dimension(), pos);
        }

        if (blockEntity.isMovingByPiston) {
            return;
        }

        if (blockEntity.delayAfterPistonMove > 0) {
            blockEntity.delayAfterPistonMove--;
            return;
        }

        // 1. 冷却递减
        if (blockEntity.cooldown > 0) {
            blockEntity.cooldown--;
            if (blockEntity.cooldown == 0 && blockEntity.miningPos == null) {
                if (blockEntity.statusCode == STATUS_COOLDOWN) {
                    blockEntity.statusCode = STATUS_READY;
                }
            }
        }

        // 2. 状态机推进
        if (blockEntity.miningPos != null) {
            // 当前处于挖掘作业中
            BlockState targetState = serverLevel.getBlockState(blockEntity.miningPos);
            SimulatorFakePlayer fakePlayer = blockEntity.getOrLoadFakePlayer(serverLevel);
            ItemStack toolStack = blockEntity.items.get(TOOL_SLOT);

            // 检查目标方块是否已被移走或变为空气
            if (targetState.isAir()) {
                ModLog.debug("[方块破坏器] 目标方块已变为空气，取消当前挖掘任务");
                blockEntity.cancelMining(serverLevel, fakePlayer);
                blockEntity.statusCode = STATUS_READY;
                blockEntity.cooldown = 2;
                blockEntity.setChanged();
                return;
            }

            // 检查工具是否损坏或被取出
            if (!isToolItem(toolStack)) {
                ModLog.debug("[方块破坏器] 工具被移走，取消挖掘");
                blockEntity.cancelMining(serverLevel, fakePlayer);
                blockEntity.statusCode = STATUS_NO_TOOL;
                blockEntity.setChanged();
                return;
            }
            if (toolStack.isDamageableItem() && toolStack.getDamageValue() >= toolStack.getMaxDamage()) {
                ModLog.debug("[方块破坏器] 工具已损坏，取消挖掘");
                blockEntity.cancelMining(serverLevel, fakePlayer);
                blockEntity.statusCode = STATUS_TOOL_BROKEN;
                blockEntity.setChanged();
                return;
            }

            // 检查输出缓冲是否已满且无法容纳
            if (blockEntity.isBufferCompletelyFull()) {
                ModLog.debug("[方块破坏器] 输出缓冲已满，暂停挖掘");
                blockEntity.cancelMining(serverLevel, fakePlayer);
                blockEntity.statusCode = STATUS_BUFFER_FULL;
                blockEntity.setChanged();
                return;
            }

            // 模拟玩家主手握持工具并踩在地面
            ItemStack prevTool = fakePlayer.getMainHandItem().copy();
            fakePlayer.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, toolStack);
            fakePlayer.updateEquipmentModifiers(prevTool, toolStack);
            fakePlayer.getInventory().setItem(0, toolStack);
            fakePlayer.getInventory().setSelectedSlot(0);
            fakePlayer.setOnGround(true);

            // 每 tick 按假玩家手持工具的挖掘速度推进进度
            float speed = targetState.getDestroyProgress(fakePlayer, serverLevel, blockEntity.miningPos);
            blockEntity.miningProgress += speed;
            int animStage = (int) (blockEntity.miningProgress * 10.0f);
            serverLevel.destroyBlockProgress(fakePlayer.getId(), blockEntity.miningPos, animStage >= 0 && animStage < 10 ? animStage : -1);

            // 挖掘完成结算
            if (blockEntity.miningProgress >= 1.0f) {
                serverLevel.destroyBlockProgress(fakePlayer.getId(), blockEntity.miningPos, -1);
                BlockEntity targetBe = serverLevel.getBlockEntity(blockEntity.miningPos);

                // 结算掉落物（精准采集、时运等全部生效）
                List<ItemStack> drops = Block.getDrops(targetState, serverLevel, blockEntity.miningPos, targetBe, fakePlayer, toolStack);

                // 放入输出缓冲
                for (ItemStack drop : drops) {
                    ItemStack remainder = blockEntity.insertToBuffer(drop);
                    if (!remainder.isEmpty()) {
                        // 极端情况下缓冲溢出则爆在方块正前方
                        Block.popResource(serverLevel, blockEntity.miningPos, remainder);
                    }
                }

                // 消耗工具 1 点耐久（遵守耐久附魔 Unbreaking）
                if (toolStack.isDamageableItem()) {
                    ModLog.debug("[方块破坏器] 挖掘成功，扣除工具 1 点耐久。工具: " + toolStack + ", 目标方块: " + targetState);
                    toolStack.hurtAndBreak(1, serverLevel, fakePlayer, item -> {
                        ModLog.debug("[方块破坏器] 工具耗尽损坏: " + item);
                    });
                }

                // 播放原版破坏粒子与音效
                serverLevel.levelEvent(2001, blockEntity.miningPos, Block.getId(targetState));

                // 摧毁目标方块
                serverLevel.destroyBlock(blockEntity.miningPos, false, fakePlayer);
                net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.AFTER.invoker().afterBlockBreak(serverLevel, fakePlayer, blockEntity.miningPos, targetState, targetBe);

                blockEntity.miningPos = null;
                blockEntity.miningProgress = 0.0f;
                blockEntity.cooldown = 20; // 冷却 20 ticks (1秒)
                blockEntity.statusCode = STATUS_COOLDOWN;
                blockEntity.setChanged();
            }
        } else {
            // 没有正在进行的挖掘：持续模式且背面有红石信号且不在冷却时，自动尝试下一次挖掘
            if (blockEntity.continuousMode && blockEntity.cooldown <= 0) {
                Direction facing = state.getValue(BlockBreakerBlock.FACING);
                if (blockEntity.hasBackSignal(serverLevel, pos, facing)) {
                    blockEntity.tryStartMining(serverLevel, pos, state);
                }
            }
        }
    }

    /**
     * 取消挖掘并清除裂纹动画
     */
    public void cancelMining(ServerLevel serverLevel, @Nullable SimulatorFakePlayer fakePlayer) {
        if (this.miningPos != null) {
            int entityId = fakePlayer != null ? fakePlayer.getId() : -1;
            serverLevel.destroyBlockProgress(entityId, this.miningPos, -1);
            this.miningPos = null;
            this.miningProgress = 0.0f;
        }
    }

    /**
     * 检查方块背面是否有红石信号
     */
    public boolean hasBackSignal(Level level, BlockPos pos, Direction facing) {
        Direction backDir = facing.getOpposite();
        BlockPos backPos = pos.relative(backDir);
        return level.hasSignal(backPos, backDir);
    }

    /**
     * 红石上升沿触发
     */
    public void trigger() {
        if (this.level instanceof ServerLevel serverLevel && !this.isMovingByPiston && this.delayAfterPistonMove <= 0) {
            BlockState state = this.getBlockState();
            tryStartMining(serverLevel, this.worldPosition, state);
        }
    }

    /**
     * 尝试开始挖掘前置检查与启动
     */
    public void tryStartMining(ServerLevel serverLevel, BlockPos pos, BlockState state) {
        if (this.miningPos != null) {
            return; // 已经有挖掘作业正在执行
        }

        Direction facing = state.getValue(BlockBreakerBlock.FACING);
        BlockPos targetPos = pos.relative(facing);
        BlockState targetState = serverLevel.getBlockState(targetPos);

        // 1. 空气检查
        if (targetState.isAir()) {
            this.statusCode = STATUS_READY;
            return;
        }

        // 2. 黑名单、方块实体与硬度 -1 (基岩、屏障等) 检查
        if (targetState.getDestroySpeed(serverLevel, targetPos) == -1.0F
                || targetState.is(ModBlockTags.BLOCK_BREAKER_BLACKLIST)
                || targetState.hasBlockEntity()) {
            ModLog.debug("[方块破坏器] 前方方块为黑名单、方块实体或不可破坏方块: " + targetState);
            this.statusCode = STATUS_UNBREAKABLE;
            this.setChanged();
            return;
        }

        // 3. 工具槽检查
        ItemStack tool = this.items.get(TOOL_SLOT);
        if (!isToolItem(tool)) {
            ModLog.debug("[方块破坏器] 无有效工具");
            this.statusCode = STATUS_NO_TOOL;
            this.setChanged();
            return;
        }
        if (tool.isDamageableItem() && tool.getDamageValue() >= tool.getMaxDamage()) {
            ModLog.debug("[方块破坏器] 工具已损坏");
            this.statusCode = STATUS_TOOL_BROKEN;
            this.setChanged();
            return;
        }

        // 4. 工具采掘等级检查
        SimulatorFakePlayer fakePlayer = getOrLoadFakePlayer(serverLevel);
        ItemStack prevTool = fakePlayer.getMainHandItem().copy();
        fakePlayer.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, tool);
        fakePlayer.updateEquipmentModifiers(prevTool, tool);
        fakePlayer.getInventory().setItem(0, tool);
        fakePlayer.getInventory().setSelectedSlot(0);

        if (targetState.requiresCorrectToolForDrops() && !fakePlayer.hasCorrectToolForDrops(targetState)) {
            ModLog.debug("[方块破坏器] 工具采掘等级不足，无法掉落该方块: " + targetState);
            this.statusCode = STATUS_WRONG_TOOL_LEVEL;
            this.setChanged();
            return;
        }

        // 5. 输出缓冲空间检查
        if (isBufferCompletelyFull()) {
            ModLog.debug("[方块破坏器] 输出缓冲已完全占满");
            this.statusCode = STATUS_BUFFER_FULL;
            this.setChanged();
            return;
        }

        // 6. 领地/事件保护拦截检查
        if (!net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(serverLevel, fakePlayer, targetPos, targetState, serverLevel.getBlockEntity(targetPos))) {
            ModLog.debug("[方块破坏器] 玩家破坏事件被领地/保护插件拒绝: " + targetPos);
            this.statusCode = STATUS_UNBREAKABLE;
            this.setChanged();
            return;
        }

        // 检查全部通过，正式启动逐 Tick 挖掘
        this.miningPos = targetPos;
        this.miningProgress = 0.0f;
        this.statusCode = STATUS_MINING;
        this.setChanged();
        ModLog.debug("[方块破坏器] 开始挖掘目标方块: " + targetPos + " (" + targetState + ")");
    }

    /**
     * 将物品安全存入 1~9 号输出缓冲槽
     */
    public ItemStack insertToBuffer(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack toInsert = stack.copy();

        // 优先合并到已有相同物品的槽位
        for (int i = BUFFER_START; i < TOTAL_SLOTS; i++) {
            ItemStack existing = this.items.get(i);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, toInsert)) {
                int space = existing.getMaxStackSize() - existing.getCount();
                if (space > 0) {
                    int toAdd = Math.min(space, toInsert.getCount());
                    existing.grow(toAdd);
                    toInsert.shrink(toAdd);
                    if (toInsert.isEmpty()) {
                        this.setChanged();
                        return ItemStack.EMPTY;
                    }
                }
            }
        }

        // 放入空槽位
        for (int i = BUFFER_START; i < TOTAL_SLOTS; i++) {
            ItemStack existing = this.items.get(i);
            if (existing.isEmpty()) {
                this.items.set(i, toInsert.copy());
                this.setChanged();
                return ItemStack.EMPTY;
            }
        }

        this.setChanged();
        return toInsert;
    }

    public boolean isBufferCompletelyFull() {
        for (int i = BUFFER_START; i < TOTAL_SLOTS; i++) {
            ItemStack stack = this.items.get(i);
            if (stack.isEmpty() || stack.getCount() < stack.getMaxStackSize()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 计算比较器输出红石信号：
     * 0: 无工具
     * 1~14: 9 格缓冲比例
     * 15: 缓冲满 或 错误（如工具损坏、无法破坏、等级不足）
     */
    public static boolean isToolItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.is(ModItemTags.BLOCK_BREAKER_TOOLS) 
                || stack.has(net.minecraft.core.component.DataComponents.TOOL)
                || stack.is(net.minecraft.world.item.Items.SHEARS);
    }

    public int getComparatorSignal() {
        ItemStack tool = this.items.get(TOOL_SLOT);
        if (!isToolItem(tool)) {
            return 0;
        }
        if (tool.isDamageableItem() && tool.getDamageValue() >= tool.getMaxDamage()) {
            return 15;
        }
        if (this.statusCode == STATUS_UNBREAKABLE || this.statusCode == STATUS_WRONG_TOOL_LEVEL || this.statusCode == STATUS_TOOL_BROKEN) {
            return 15;
        }
        if (isBufferCompletelyFull() || this.statusCode == STATUS_BUFFER_FULL) {
            return 15;
        }

        // 计算 9 个缓冲槽的占用比例
        float totalRatio = 0.0f;
        boolean hasAny = false;
        for (int i = BUFFER_START; i < TOTAL_SLOTS; i++) {
            ItemStack stack = this.items.get(i);
            if (!stack.isEmpty()) {
                hasAny = true;
                totalRatio += (float) stack.getCount() / (float) stack.getMaxStackSize();
            }
        }

        if (!hasAny) {
            return 1; // 有工具且缓冲为空的基础信号
        }

        float avg = totalRatio / (float) BUFFER_COUNT;
        int signal = 1 + (int) (avg * 13.99f);
        return Math.min(14, Math.max(1, signal));
    }

    /**
     * 切换工作模式（单次脉冲 / 持续循环）
     */
    public void toggleMode() {
        this.continuousMode = !this.continuousMode;
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public boolean isContinuousMode() {
        return this.continuousMode;
    }

    // ================== 活塞迁移逻辑 ==================

    public void onPistonMoveStart() {
        this.isMovingByPiston = true;
        if (this.level instanceof ServerLevel serverLevel) {
            cancelMining(serverLevel, this.fakePlayer);
        }
    }

    public CompoundTag saveToTag() {
        if (this.level != null) {
            CompoundTag tag = this.saveCustomOnly(this.level.registryAccess());
            ModLog.debug("[方块破坏器活塞移动] saveToTag at " + this.worldPosition + ": " + tag);
            return tag;
        }
        return new CompoundTag();
    }

    public void loadFromTag(CompoundTag tag) {
        ModLog.debug("[方块破坏器活塞移动] loadFromTag at " + this.worldPosition + " incoming tag: " + tag);
        if (this.level != null && tag != null && !tag.isEmpty()) {
            this.items.clear();
            ValueInput input = net.minecraft.world.level.storage.TagValueInput.create(ProblemReporter.DISCARDING, this.level.registryAccess(), tag);
            this.loadCustomOnly(input);
            ModLog.debug("[方块破坏器活塞移动] loadFromTag loaded items: " + this.items);
        }
        this.delayAfterPistonMove = 2; // 移动后延迟 2 ticks 再响应红石
        this.isMovingByPiston = false;
        this.setChanged();
    }

    // ================== 潜影盒序列化支持 ==================

    public void saveToItemStack(ItemStack stack) {
        CompoundTag tag = this.saveToTag();
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        // 构造详细 Tooltip 信息
        List<Component> loreList = new ArrayList<>();
        ItemStack tool = this.items.get(TOOL_SLOT);
        if (!tool.isEmpty()) {
            loreList.add(Component.literal("§7工具: §e" + tool.getHoverName().getString()));
        } else {
            loreList.add(Component.literal("§7工具: §8无"));
        }

        int filledBuffers = 0;
        for (int i = BUFFER_START; i < TOTAL_SLOTS; i++) {
            if (!this.items.get(i).isEmpty()) {
                filledBuffers++;
            }
        }
        loreList.add(Component.literal("§7缓冲物品: §a" + filledBuffers + " / 9 格"));
        loreList.add(Component.literal("§7模式: §b" + (this.continuousMode ? "持续循环" : "单次脉冲")));
        stack.set(DataComponents.LORE, new ItemLore(loreList));
    }

    public void loadFromItemStack(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            this.loadFromTag(tag);
        }
    }

    // ================== WorldlyContainer 漏斗交互 ==================

    private static final int[] SLOTS_TOP = new int[]{TOOL_SLOT};
    private static final int[] SLOTS_SIDES = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9};

    @Override
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.UP) {
            return SLOTS_TOP;
        }
        return SLOTS_SIDES;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        // 顶面只允许漏斗存入有效未损坏的工具到工具槽，且仅当工具槽为空时才存入
        if (slot == TOOL_SLOT && side == Direction.UP) {
            if (!isToolItem(stack)) {
                return false;
            }
            if (stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage()) {
                return false;
            }
            return this.items.get(TOOL_SLOT).isEmpty();
        }
        return false; // 严禁外部漏斗往 1~9 输出缓冲塞杂物
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        // 侧面和底面仅供漏斗抽出 1~9 输出缓冲物品，工具槽禁止抽出
        return slot >= BUFFER_START && slot < TOTAL_SLOTS && side != Direction.UP;
    }

    // ================== 基础 Container 实现 ==================

    @Override
    public int getContainerSize() {
        return TOTAL_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack item : this.items) {
            if (!item.isEmpty()) return false;
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
        if (!result.isEmpty()) this.setChanged();
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
        return Component.translatable("container.tuanzis_mod.block_breaker");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BlockBreakerMenu(containerId, playerInventory, this, this.data);
    }

    public NonNullList<ItemStack> getItems() {
        return this.items;
    }

    public boolean isMovingByPiston() {
        return this.isMovingByPiston;
    }

    // ================== NBT 存取 ==================

    @Override
    protected void loadAdditional(final ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, this.items);
        this.continuousMode = input.getBooleanOr("ContinuousMode", false);
        this.cooldown = input.getIntOr("Cooldown", 0);
        this.statusCode = input.getIntOr("StatusCode", STATUS_READY);
        this.miningPos = input.read("MiningPos", BlockPos.CODEC).orElse(null);
        this.miningProgress = input.getFloatOr("MiningProgress", 0.0f);
    }

    @Override
    protected void saveAdditional(final ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        output.putBoolean("ContinuousMode", this.continuousMode);
        output.putInt("Cooldown", this.cooldown);
        output.putInt("StatusCode", this.statusCode);
        if (this.miningPos != null) {
            output.store("MiningPos", BlockPos.CODEC, this.miningPos);
        }
        output.putFloat("MiningProgress", this.miningProgress);
    }
}
