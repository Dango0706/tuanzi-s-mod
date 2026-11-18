package me.tuanzi.block.entity;

import me.tuanzi.block.cannon.BlueprintBuildPlanner;
import me.tuanzi.block.cannon.BlueprintBuildPlanner.BlueprintCell;
import me.tuanzi.block.cannon.BlueprintBuildPlanner.MultiBlockGroup;
import me.tuanzi.block.cannon.BlueprintBuildPlanner.ParsedBlueprint;
import me.tuanzi.block.cannon.LaunchedItem;
import me.tuanzi.init.ModBlocks;
import me.tuanzi.init.ModItems;
import me.tuanzi.item.StructureBlueprintItem;
import me.tuanzi.network.BlueprintCannonShootS2CPacket;
import me.tuanzi.world.inventory.BlueprintCannonMenu;
import me.tuanzi.world.inventory.GoldenPocketMenu;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class BlueprintCannonBlockEntity extends BlockEntity implements MenuProvider, Container {

    // 机械动力风格飞行方块列表（LaunchedItem 队列与瞄准状态）
    public final List<LaunchedItem> flyingBlocks = new LinkedList<>();
    // GUI 属性同步 Data（共 11 个槽位）
    public final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> buildState;
                case 1 -> currentIndex;    // index 1: 当前进度索引（即已处理格子数）
                case 2 -> totalBlocks;     // index 2: 总格子数（blocks.length）
                case 3 -> destroyMode ? 1 : 0;
                case 4 -> exactMatch ? 1 : 0;
                case 5 -> getSlurryDurability();
                case 6 -> worldPosition.getX();
                case 7 -> worldPosition.getY();
                case 8 -> worldPosition.getZ();
                case 9 -> pauseReason;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> buildState = value;
                case 1 -> currentIndex = value;
                case 2 -> totalBlocks = value;
                case 3 -> destroyMode = (value == 1);
                case 4 -> exactMatch = (value == 1);
                case 9 -> pauseReason = value;
            }
        }

        @Override
        public int getCount() {
            return 11;
        }
    };
    // 3 个物品槽：Slot 0 为蓝图，Slot 1 为压缩浆料燃料，Slot 2 为书与笔（材料清单输出）
    private final NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    public BlockPos currentTarget = null;
    public BlockPos previousTarget = null;
    public float defaultYaw = 0.0f;
    public boolean firstRenderTick = true;
    // 客户端平滑炮管朝向状态（避免每帧在 previousTarget 与 currentTarget 间反复插值抽搐）
    public float renderYaw = 0.0f;
    public float prevRenderYaw = 0.0f;
    public float renderPitch = 30.0f;
    public float prevRenderPitch = 30.0f;
    // 建造状态：0=IDLE, 1=RUNNING, 2=PAUSED
    private int buildState = 0;
    private int currentIndex = 0; // 已处理的进度分子
    private int totalBlocks = 0;  // 总工作量（进度分母）
    private boolean destroyMode = false;
    private boolean exactMatch = false;
    private int fuel = 0;
    private int pauseReason = 0; // 0=none, 1=no fuel, 2=no blocks
    private int ticksToNextPlace = 5;
    private List<ContainerSource> cachedSources = null;
    private @Nullable ActiveBuildPlan activePlan = null;

    public BlueprintCannonBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.BLUEPRINT_CANNON_BLOCK_ENTITY, pos, state);
    }

    public static Item getRequiredItemForEntity(String entityId, CompoundTag nbt) {
        ItemStack base = me.tuanzi.block.cannon.BlueprintEntityHelper.getBaseItemStack(entityId, nbt, null);
        return base.getItem();
    }

    public static double[] computeTargetAngles(BlockPos cannonPos, @Nullable BlockPos target, float defaultYaw) {
        if (target == null) {
            return new double[]{defaultYaw, 30.0};
        }
        Vec3 diff = Vec3.atLowerCornerOf(target.subtract(cannonPos));
        double diffX = diff.x();
        double diffZ = diff.z();
        double yaw = Mth.atan2(diffX, diffZ) / Math.PI * 180.0;
        float distance = Mth.sqrt((float) (diffX * diffX + diffZ * diffZ));
        double targetPitch = Math.toDegrees(Math.atan2(diff.y() + distance * 0.35, distance));
        double pitch = Mth.clamp(targetPitch, 10.0, 75.0);
        return new double[]{yaw, pitch};
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, BlueprintCannonBlockEntity be) {
        be.tickFlyingBlocks(level);

        double[] desired = computeTargetAngles(pos, be.currentTarget, be.defaultYaw);
        if (be.firstRenderTick) {
            be.renderYaw = be.prevRenderYaw = (float) desired[0];
            be.renderPitch = be.prevRenderPitch = (float) desired[1];
            be.firstRenderTick = false;
        } else {
            be.prevRenderYaw = be.renderYaw;
            be.prevRenderPitch = be.renderPitch;
            float yawDiff = Mth.wrapDegrees((float) desired[0] - be.renderYaw);
            float pitchDiff = (float) desired[1] - be.renderPitch;
            be.renderYaw = Math.abs(yawDiff) < 0.1f
                    ? (float) desired[0]
                    : Mth.wrapDegrees(be.renderYaw + yawDiff * 0.45f);
            be.renderPitch = Math.abs(pitchDiff) < 0.1f
                    ? (float) desired[1]
                    : be.renderPitch + pitchDiff * 0.45f;
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BlueprintCannonBlockEntity be) {
        // 每 tick 推动空中所有已发射的方块向目标飞行并在到达时落地筑造/破坏
        be.tickFlyingBlocks(level);

        if (be.buildState != 1) return; // 只在 RUNNING 时工作

        ItemStack blueprint = be.items.get(0);
        if (blueprint.isEmpty() || !(blueprint.getItem() instanceof StructureBlueprintItem)) {
            be.resetToIdle();
            return;
        }

        CustomData customData = blueprint.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            be.resetToIdle();
            return;
        }

        CompoundTag blueprintTag = customData.copyTag();
        if (!blueprintTag.contains("Width")) {
            be.resetToIdle();
            return;
        }

        int width = blueprintTag.getIntOr("Width", 0);
        int height = blueprintTag.getIntOr("Height", 0);
        int length = blueprintTag.getIntOr("Length", 0);
        int[] blocks = blueprintTag.getIntArray("Blocks").orElse(new int[0]);

        if (blocks.length == 0) {
            be.resetToIdle();
            return;
        }

        ListTag entitiesTag = blueprintTag.getListOrEmpty("Entities");
        if (be.activePlan == null || !be.activePlan.matches(blueprintTag, be.destroyMode, be.exactMatch)) {
            be.activePlan = ActiveBuildPlan.create(blueprintTag, level, be.destroyMode, be.exactMatch);
            me.tuanzi.util.ModLog.debug("BlueprintCannon: 构建建造计划 phase=" + be.activePlan.phase
                    + ", destroyQueue=" + be.activePlan.destroyQueue.size()
                    + ", singleQueue=" + be.activePlan.primarySingleQueue.size()
                    + ", multiQueue=" + be.activePlan.multiQueue.size()
                    + ", entityQueue=" + be.activePlan.entityQueue.size());
        }

        ActiveBuildPlan plan = be.activePlan;
        int planTotal = plan.getTotalWork();
        if (be.totalBlocks != planTotal) {
            be.totalBlocks = planTotal;
            be.setChanged();
        }

        be.ticksToNextPlace--;
        if (be.ticksToNextPlace <= 0) {
            be.ticksToNextPlace = 0;

            if (be.cachedSources == null) {
                be.scanNearbyContainers(level, pos);
            }

            be.processBuildPlanTick(level, pos, state, blueprintTag, width, height, length, entitiesTag, plan);
        }
    }

    private void resetToIdle() {
        this.buildState = 0;
        this.currentIndex = 0;
        this.totalBlocks = 0;
        this.activePlan = null;
        this.currentTarget = null;
        this.previousTarget = null;
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            BlockState s = this.getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, s, s, 3);
        }
    }

    private void processBuildPlanTick(
            Level level,
            BlockPos pos,
            BlockState cannonState,
            CompoundTag blueprintTag,
            int width,
            int height,
            int length,
            ListTag entitiesTag,
            ActiveBuildPlan plan
    ) {
        int checksThisTick = 0;
        final int MAX_CHECKS_PER_TICK = 512;

        while (checksThisTick < MAX_CHECKS_PER_TICK) {
            checksThisTick++;

            switch (plan.phase) {
                case DESTROY -> {
                    if (!plan.destroyQueue.isEmpty()) {
                        BlockPos targetPos = plan.destroyQueue.peekFirst();
                        BlueprintCell cell = plan.parsed.cellMap().get(targetPos);
                        BlockState worldState = level.getBlockState(targetPos);

                        boolean needsDestroy = false;
                        if (BlueprintBuildPlanner.isBreakableBlock(level, targetPos, worldState)) {
                            if (cell == null) {
                                needsDestroy = true;
                            } else if (!BlueprintBuildPlanner.isStateMatch(worldState, cell.state(), this.exactMatch)) {
                                needsDestroy = true;
                            }
                        }

                        if (!needsDestroy || isDestroyInFlight(targetPos)) {
                            plan.destroyQueue.pollFirst();
                            plan.destroyDone++;
                            this.currentIndex = plan.getCompletedWork();
                            continue;
                        }

                        if (!this.consumeFuel(level, pos)) {
                            this.ticksToNextPlace = 0;
                            return;
                        }

                        plan.destroyQueue.pollFirst();
                        plan.destroyDone++;
                        this.currentIndex = plan.getCompletedWork();
                        this.launchBlock(level, targetPos, ItemStack.EMPTY, Blocks.AIR.defaultBlockState(), null, true);
                        this.ticksToNextPlace = 5;
                        this.setChanged();
                        return;
                    }

                    // 等待所有拆除飞弹落地后再校验是否有下落方块等残余障碍
                    if (hasFlyingDestroyBlocks()) {
                        this.currentIndex = plan.getCompletedWork();
                        return;
                    }

                    boolean foundRemaining = false;
                    for (BlockPos candidate : plan.parsed.destroyCandidatesTopDown()) {
                        BlockState worldState = level.getBlockState(candidate);
                        if (!BlueprintBuildPlanner.isBreakableBlock(level, candidate, worldState)) {
                            continue;
                        }
                        BlueprintCell cell = plan.parsed.cellMap().get(candidate);
                        if (cell == null || !BlueprintBuildPlanner.isStateMatch(worldState, cell.state(), this.exactMatch)) {
                            plan.destroyQueue.addLast(candidate);
                            foundRemaining = true;
                        }
                    }

                    if (!foundRemaining) {
                        plan.phase = BuildPhase.SINGLE_PRIMARY;
                        me.tuanzi.util.ModLog.debug("BlueprintCannon: 破坏阶段完成，进入基础方块放置阶段");
                    }
                }
                case SINGLE_PRIMARY -> {
                    if (!plan.primarySingleQueue.isEmpty()) {
                        BlueprintCell cell = plan.primarySingleQueue.peekFirst();
                        BlockState worldState = level.getBlockState(cell.pos());

                        if (BlueprintBuildPlanner.isStateMatch(worldState, cell.state(), this.exactMatch)
                                || !BlueprintBuildPlanner.canOverwriteWorldState(worldState)
                                || cell.state().getBlock().asItem() == Items.AIR) {
                            plan.primarySingleQueue.pollFirst();
                            plan.placementDone++;
                            this.currentIndex = plan.getCompletedWork();
                            continue;
                        }

                        if (!BlueprintBuildPlanner.isSingleBlockReady(level, cell.pos(), cell.state(), plan.parsed.cellMap())) {
                            plan.primarySingleQueue.pollFirst();
                            plan.deferredSingleQueue.addLast(cell);
                            continue;
                        }

                        LaunchOutcome outcome = tryLaunchSingleCell(level, pos, cell, false);
                        if (outcome == LaunchOutcome.LAUNCHED) {
                            plan.primarySingleQueue.pollFirst();
                            plan.placementDone++;
                            this.currentIndex = plan.getCompletedWork();
                            this.ticksToNextPlace = 5;
                            this.setChanged();
                            return;
                        }
                        return;
                    }

                    plan.deferredSinglePassRemaining = plan.deferredSingleQueue.size();
                    plan.phase = BuildPhase.SINGLE_DEPENDENT;
                }
                case SINGLE_DEPENDENT -> {
                    if (plan.deferredSingleQueue.isEmpty()) {
                        plan.phase = BuildPhase.MULTI_BLOCK;
                        continue;
                    }

                    if (plan.deferredSinglePassRemaining <= 0) {
                        // 本轮没有可放置的依附方块：若仍有飞行中的方块，则等待其落地后再复检
                        if (hasFlyingBlockItems()) {
                            plan.deferredSinglePassRemaining = plan.deferredSingleQueue.size();
                            return;
                        }
                        // 所有先导单方块已落地，仍有未就绪的依附方块（可能依附于多方块或悬浮结构），先进入多方块阶段
                        plan.phase = BuildPhase.MULTI_BLOCK;
                        continue;
                    }

                    BlueprintCell cell = plan.deferredSingleQueue.peekFirst();
                    BlockState worldState = level.getBlockState(cell.pos());

                    if (BlueprintBuildPlanner.isStateMatch(worldState, cell.state(), this.exactMatch)
                            || !BlueprintBuildPlanner.canOverwriteWorldState(worldState)
                            || cell.state().getBlock().asItem() == Items.AIR) {
                        plan.deferredSingleQueue.pollFirst();
                        plan.placementDone++;
                        plan.deferredSinglePassRemaining = plan.deferredSingleQueue.size();
                        this.currentIndex = plan.getCompletedWork();
                        continue;
                    }

                    if (BlueprintBuildPlanner.isSingleBlockReady(level, cell.pos(), cell.state(), plan.parsed.cellMap())) {
                        LaunchOutcome outcome = tryLaunchSingleCell(level, pos, cell, false);
                        if (outcome == LaunchOutcome.LAUNCHED) {
                            plan.deferredSingleQueue.pollFirst();
                            plan.placementDone++;
                            plan.deferredSinglePassRemaining = plan.deferredSingleQueue.size();
                            this.currentIndex = plan.getCompletedWork();
                            this.ticksToNextPlace = 5;
                            this.setChanged();
                            return;
                        }
                        return;
                    }

                    plan.deferredSingleQueue.pollFirst();
                    plan.deferredSingleQueue.addLast(cell);
                    plan.deferredSinglePassRemaining--;
                }
                case MULTI_BLOCK -> {
                    if (!plan.multiQueue.isEmpty()) {
                        MultiBlockGroup group = plan.multiQueue.peekFirst();

                        if (isMultiBlockAlreadyComplete(level, group) || isMultiBlockBlocked(level, group)) {
                            plan.multiQueue.pollFirst();
                            plan.placementDone += group.parts().size();
                            this.currentIndex = plan.getCompletedWork();
                            continue;
                        }

                        if (!BlueprintBuildPlanner.isMultiBlockReady(level, group, plan.parsed.cellMap())) {
                            if (hasFlyingBlockItems()) {
                                return;
                            }
                            plan.multiQueue.pollFirst();
                            plan.deferredMultiQueue.addLast(group);
                            continue;
                        }

                        LaunchOutcome outcome = tryLaunchMultiGroup(level, pos, group, false);
                        if (outcome == LaunchOutcome.LAUNCHED) {
                            plan.multiQueue.pollFirst();
                            plan.placementDone += group.parts().size();
                            this.currentIndex = plan.getCompletedWork();
                            this.ticksToNextPlace = 5;
                            this.setChanged();
                            return;
                        }
                        return;
                    }

                    plan.deferredMultiPassRemaining = plan.deferredMultiQueue.size();
                    plan.deferredSinglePassRemaining = plan.deferredSingleQueue.size();
                    plan.phase = BuildPhase.POST_MULTI_DEPENDENT;
                }
                case POST_MULTI_DEPENDENT -> {
                    if (hasFlyingBlockItems()) {
                        return;
                    }

                    if (!plan.deferredMultiQueue.isEmpty()) {
                        MultiBlockGroup group = plan.deferredMultiQueue.peekFirst();
                        if (isMultiBlockAlreadyComplete(level, group) || isMultiBlockBlocked(level, group)) {
                            plan.deferredMultiQueue.pollFirst();
                            plan.placementDone += group.parts().size();
                            plan.deferredMultiPassRemaining = plan.deferredMultiQueue.size();
                            this.currentIndex = plan.getCompletedWork();
                            continue;
                        }
                        boolean ready = BlueprintBuildPlanner.isMultiBlockReady(level, group, plan.parsed.cellMap());
                        if (ready || plan.deferredMultiPassRemaining <= 0) {
                            LaunchOutcome outcome = tryLaunchMultiGroup(level, pos, group, !ready);
                            if (outcome == LaunchOutcome.LAUNCHED) {
                                plan.deferredMultiQueue.pollFirst();
                                plan.placementDone += group.parts().size();
                                plan.deferredMultiPassRemaining = plan.deferredMultiQueue.size();
                                this.currentIndex = plan.getCompletedWork();
                                this.ticksToNextPlace = 5;
                                this.setChanged();
                                return;
                            }
                            return;
                        }
                        plan.deferredMultiQueue.pollFirst();
                        plan.deferredMultiQueue.addLast(group);
                        plan.deferredMultiPassRemaining--;
                        continue;
                    }

                    if (!plan.deferredSingleQueue.isEmpty()) {
                        BlueprintCell cell = plan.deferredSingleQueue.peekFirst();
                        BlockState worldState = level.getBlockState(cell.pos());
                        if (BlueprintBuildPlanner.isStateMatch(worldState, cell.state(), this.exactMatch)
                                || !BlueprintBuildPlanner.canOverwriteWorldState(worldState)
                                || cell.state().getBlock().asItem() == Items.AIR) {
                            plan.deferredSingleQueue.pollFirst();
                            plan.placementDone++;
                            plan.deferredSinglePassRemaining = plan.deferredSingleQueue.size();
                            this.currentIndex = plan.getCompletedWork();
                            continue;
                        }

                        boolean ready = BlueprintBuildPlanner.isSingleBlockReady(level, cell.pos(), cell.state(), plan.parsed.cellMap());
                        if (ready || plan.deferredSinglePassRemaining <= 0) {
                            LaunchOutcome outcome = tryLaunchSingleCell(level, pos, cell, !ready);
                            if (outcome == LaunchOutcome.LAUNCHED) {
                                plan.deferredSingleQueue.pollFirst();
                                plan.placementDone++;
                                plan.deferredSinglePassRemaining = plan.deferredSingleQueue.size();
                                this.currentIndex = plan.getCompletedWork();
                                this.ticksToNextPlace = 5;
                                this.setChanged();
                                return;
                            }
                            return;
                        }
                        plan.deferredSingleQueue.pollFirst();
                        plan.deferredSingleQueue.addLast(cell);
                        plan.deferredSinglePassRemaining--;
                        continue;
                    }

                    plan.phase = BuildPhase.ENTITIES;
                }
                case ENTITIES -> {
                    if (hasFlyingBlockItems()) {
                        return;
                    }
                    if (!plan.entityQueue.isEmpty()) {
                        int entityIdx = plan.entityQueue.peekFirst();
                        EntityStepOutcome outcome = processEntityStep(level, pos, blueprintTag, width, height, length, entitiesTag, entityIdx);
                        switch (outcome) {
                            case SKIP -> {
                                plan.entityQueue.pollFirst();
                                plan.placementDone++;
                                this.currentIndex = plan.getCompletedWork();
                                continue;
                            }
                            case LAUNCHED -> {
                                plan.entityQueue.pollFirst();
                                plan.placementDone++;
                                this.currentIndex = plan.getCompletedWork();
                                this.ticksToNextPlace = 5;
                                this.setChanged();
                                return;
                            }
                            case DESTROYED_CONFLICT -> {
                                this.ticksToNextPlace = 5;
                                this.setChanged();
                                return;
                            }
                            case PAUSED -> {
                                return;
                            }
                        }
                    }
                    plan.phase = BuildPhase.DONE;
                }
                case DONE -> {
                    if (!this.flyingBlocks.isEmpty()) {
                        return;
                    }
                    this.totalBlocks = plan.getTotalWork();
                    this.currentIndex = this.totalBlocks;
                    this.buildState = 0;
                    this.ticksToNextPlace = 0;
                    this.currentTarget = null;
                    this.previousTarget = null;
                    this.activePlan = null;
                    me.tuanzi.util.ModLog.debug("蓝图建造完成，currentIndex=" + this.currentIndex + " totalBlocks=" + this.totalBlocks);
                    level.playSound(null, pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 1.0f, 1.2f);
                    this.setChanged();
                    level.sendBlockUpdated(pos, cannonState, cannonState, 3);
                    return;
                }
            }
        }
    }

    private enum LaunchOutcome {
        LAUNCHED,
        PAUSED
    }

    private enum EntityStepOutcome {
        SKIP,
        LAUNCHED,
        DESTROYED_CONFLICT,
        PAUSED
    }

    private LaunchOutcome tryLaunchSingleCell(Level level, BlockPos cannonPos, BlueprintCell cell, boolean forceKeepShape) {
        Item requiredItem = cell.state().getBlock().asItem();
        boolean hasCreativeCore = ensureCreativeCoreOrRescan(level, cannonPos);
        ContainerSource source = hasCreativeCore ? null : findItemOrRescan(level, cannonPos, requiredItem);

        if (source == null && !hasCreativeCore) {
            this.buildState = 2;
            this.pauseReason = 2;
            this.ticksToNextPlace = 0;
            level.playSound(null, cannonPos, SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.BLOCKS, 1.0f, 0.8f);
            this.setChanged();
            return LaunchOutcome.PAUSED;
        }

        if (!this.consumeFuel(level, cannonPos)) {
            this.ticksToNextPlace = 0;
            return LaunchOutcome.PAUSED;
        }

        ItemStack placedStack;
        if (source != null && !hasCreativeCore) {
            ItemStack srcStack = source.container.getItem(source.slot);
            placedStack = srcStack.copyWithCount(1);
            srcStack.shrink(1);
            source.markChanged();
        } else {
            placedStack = new ItemStack(cell.state().getBlock());
            me.tuanzi.util.ModLog.debug("检测到创造核心: 无视材料缺失或不消耗材料直接放置方块: " + cell.state() + " at " + cell.pos());
        }

        me.tuanzi.util.ModLog.debug("准备放置: " + cell.state() + " -> requiredItem: " + requiredItem + " at " + cell.pos() + ", forceKeepShape=" + forceKeepShape);
        this.launchBlock(level, cell.pos(), placedStack, cell.state(), cell.teData(), false, forceKeepShape);
        return LaunchOutcome.LAUNCHED;
    }

    private LaunchOutcome tryLaunchMultiGroup(Level level, BlockPos cannonPos, MultiBlockGroup group, boolean forceKeepShape) {
        Item requiredItem = group.requiredItem();
        boolean hasCreativeCore = ensureCreativeCoreOrRescan(level, cannonPos);
        ContainerSource source = hasCreativeCore ? null : findItemOrRescan(level, cannonPos, requiredItem);

        if (source == null && !hasCreativeCore) {
            this.buildState = 2;
            this.pauseReason = 2;
            this.ticksToNextPlace = 0;
            level.playSound(null, cannonPos, SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.BLOCKS, 1.0f, 0.8f);
            this.setChanged();
            return LaunchOutcome.PAUSED;
        }

        if (!this.consumeFuel(level, cannonPos)) {
            this.ticksToNextPlace = 0;
            return LaunchOutcome.PAUSED;
        }

        ItemStack placedStack;
        if (source != null && !hasCreativeCore) {
            ItemStack srcStack = source.container.getItem(source.slot);
            placedStack = srcStack.copyWithCount(1);
            srcStack.shrink(1);
            source.markChanged();
        } else {
            placedStack = new ItemStack(requiredItem);
            me.tuanzi.util.ModLog.debug("检测到创造核心: 直接整体放置多方块组 (" + group.parts().size() + "格): " + requiredItem);
        }

        me.tuanzi.util.ModLog.debug("准备整体放置多方块: item=" + requiredItem + ", parts=" + group.parts().size() + ", primary=" + group.parts().getFirst().pos());
        this.launchMultiBlock(level, group, placedStack, forceKeepShape);
        return LaunchOutcome.LAUNCHED;
    }

    private boolean isMultiBlockAlreadyComplete(Level level, MultiBlockGroup group) {
        for (BlueprintCell part : group.parts()) {
            BlockState worldState = level.getBlockState(part.pos());
            if (!BlueprintBuildPlanner.isStateMatch(worldState, part.state(), this.exactMatch)) {
                return false;
            }
        }
        return true;
    }

    private boolean isMultiBlockBlocked(Level level, MultiBlockGroup group) {
        for (BlueprintCell part : group.parts()) {
            BlockState worldState = level.getBlockState(part.pos());
            if (!BlueprintBuildPlanner.isStateMatch(worldState, part.state(), this.exactMatch)
                    && !BlueprintBuildPlanner.canOverwriteWorldState(worldState)) {
                return true;
            }
        }
        return false;
    }

    private boolean isDestroyInFlight(BlockPos targetPos) {
        for (LaunchedItem item : this.flyingBlocks) {
            if (item instanceof LaunchedItem.ForBlockState fb && fb.isDestroy && fb.target.equals(targetPos)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasFlyingDestroyBlocks() {
        for (LaunchedItem item : this.flyingBlocks) {
            if (item instanceof LaunchedItem.ForBlockState fb && fb.isDestroy) {
                return true;
            }
        }
        return false;
    }

    private boolean hasFlyingBlockItems() {
        for (LaunchedItem item : this.flyingBlocks) {
            if (item instanceof LaunchedItem.ForBlockState || item instanceof LaunchedItem.ForMultiBlock) {
                return true;
            }
        }
        return false;
    }

    private boolean ensureCreativeCoreOrRescan(Level level, BlockPos cannonPos) {
        if (this.hasCreativeCoreInSources()) {
            return true;
        }
        this.scanNearbyContainers(level, cannonPos);
        return this.hasCreativeCoreInSources();
    }

    private @Nullable ContainerSource findItemOrRescan(Level level, BlockPos cannonPos, Item item) {
        ContainerSource src = this.findItemInSources(item);
        if (src != null) {
            return src;
        }
        this.scanNearbyContainers(level, cannonPos);
        return this.findItemInSources(item);
    }

    private EntityStepOutcome processEntityStep(
            Level level,
            BlockPos pos,
            CompoundTag blueprintTag,
            int width,
            int height,
            int length,
            ListTag entitiesTag,
            int entityIdx
    ) {
        if (entityIdx < 0 || entityIdx >= entitiesTag.size()) {
            return EntityStepOutcome.SKIP;
        }

        CompoundTag entityNbt = entitiesTag.getCompoundOrEmpty(entityIdx);
        String id = entityNbt.getStringOr("id", "");

        if (me.tuanzi.block.cannon.BlueprintEntityHelper.isIgnoredEntity(id)) {
            return EntityStepOutcome.SKIP;
        }

        boolean hasCreativeCore = ensureCreativeCoreOrRescan(level, pos);
        boolean isBuildableNonLiving = me.tuanzi.block.cannon.BlueprintEntityHelper.isBuildableNonLivingEntity(id);

        if (!isBuildableNonLiving && !hasCreativeCore) {
            return EntityStepOutcome.SKIP;
        }

        BlockPos basePos = new BlockPos(blueprintTag.getIntOr("GhostX", 0), blueprintTag.getIntOr("GhostY", 0), blueprintTag.getIntOr("GhostZ", 0));
        BlockPos offset = new BlockPos(blueprintTag.getIntOr("OffsetX", 0), blueprintTag.getIntOr("OffsetY", 0), blueprintTag.getIntOr("OffsetZ", 0));
        int rotationIdx = blueprintTag.getIntOr("Rotation", 0);
        boolean mirrored = blueprintTag.getBooleanOr("Mirrored", false);

        me.tuanzi.block.cannon.BlueprintEntityHelper.TransformedEntity transformed =
                me.tuanzi.block.cannon.BlueprintEntityHelper.transformEntity(
                        entityNbt, width, height, length, basePos, offset, rotationIdx, mirrored, level.registryAccess()
                );
        Vec3 exactPos = transformed.exactPos();
        BlockPos targetBlockPos = transformed.targetBlockPos();
        net.minecraft.core.Direction facing = transformed.facing();
        CompoundTag transformedNbt = transformed.transformedNbt();

        boolean alreadyFlying = this.flyingBlocks.stream().anyMatch(item ->
                item instanceof LaunchedItem.ForEntity forEntity &&
                        forEntity.exactTarget != null &&
                        forEntity.exactTarget.distanceToSqr(exactPos) < 0.05 &&
                        forEntity.entityId.equals(id)
        );
        if (alreadyFlying) {
            return EntityStepOutcome.SKIP;
        }

        net.minecraft.world.entity.EntityType<?> entityType =
                net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(net.minecraft.resources.Identifier.tryParse(id)).orElse(null);
        if (entityType == null) {
            return EntityStepOutcome.SKIP;
        }

        net.minecraft.world.phys.AABB checkBox = new net.minecraft.world.phys.AABB(
                exactPos.x - 0.5, exactPos.y - 0.5, exactPos.z - 0.5,
                exactPos.x + 0.5, exactPos.y + 0.5, exactPos.z + 0.5
        );

        boolean shouldSkip = false;
        boolean needDestroyConflict = false;
        net.minecraft.world.entity.Entity conflictTarget = null;

        if (id.contains("item_frame") || id.equals("minecraft:painting")) {
            List<net.minecraft.world.entity.decoration.HangingEntity> hangings =
                    level.getEntitiesOfClass(net.minecraft.world.entity.decoration.HangingEntity.class, new net.minecraft.world.phys.AABB(targetBlockPos).inflate(0.5));
            for (net.minecraft.world.entity.decoration.HangingEntity hanging : hangings) {
                if (hanging.getDirection() == facing) {
                    if (hanging.getType() == entityType) {
                        shouldSkip = true;
                        break;
                    } else if (this.destroyMode) {
                        needDestroyConflict = true;
                        conflictTarget = hanging;
                    }
                }
            }
        } else {
            List<? extends net.minecraft.world.entity.Entity> existing = level.getEntities(entityType, checkBox, e -> true);
            if (!existing.isEmpty()) {
                shouldSkip = true;
            } else if (this.destroyMode) {
                List<net.minecraft.world.entity.Entity> conflicts = level.getEntities((net.minecraft.world.entity.Entity) null, checkBox,
                        e -> !(e instanceof net.minecraft.world.entity.player.Player) && !(e instanceof net.minecraft.world.entity.LivingEntity living && !(living instanceof net.minecraft.world.entity.decoration.ArmorStand)));
                if (!conflicts.isEmpty()) {
                    needDestroyConflict = true;
                    conflictTarget = conflicts.getFirst();
                }
            }
        }

        if (shouldSkip) {
            return EntityStepOutcome.SKIP;
        }

        if (needDestroyConflict && conflictTarget != null) {
            if (this.consumeFuel(level, pos)) {
                if (level instanceof ServerLevel serverLevel) {
                    conflictTarget.kill(serverLevel);
                } else {
                    conflictTarget.discard();
                }
                return EntityStepOutcome.DESTROYED_CONFLICT;
            }
            return EntityStepOutcome.PAUSED;
        }

        Map<String, ItemStack> consumedItems = new LinkedHashMap<>();
        if (!hasCreativeCore) {
            List<me.tuanzi.block.cannon.BlueprintEntityHelper.EntityRequirement> reqs =
                    me.tuanzi.block.cannon.BlueprintEntityHelper.extractRequirements(id, transformedNbt, level.registryAccess());
            boolean canConsume = me.tuanzi.block.cannon.BlueprintEntityHelper.tryReserveAndConsume(
                    this.cachedSources, reqs, this.exactMatch, consumedItems
            );
            if (!canConsume) {
                this.buildState = 2;
                this.pauseReason = 2;
                this.ticksToNextPlace = 0;
                level.playSound(null, pos, SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.BLOCKS, 1.0f, 0.8f);
                this.setChanged();
                return EntityStepOutcome.PAUSED;
            }
        }

        if (this.consumeFuel(level, pos)) {
            CompoundTag sanitizedNbt = me.tuanzi.block.cannon.BlueprintEntityHelper.buildSanitizedEntityNbt(
                    transformedNbt, id, consumedItems, hasCreativeCore, level.registryAccess()
            );
            ItemStack baseStack = consumedItems.getOrDefault("BASE", ItemStack.EMPTY);
            this.launchEntity(level, exactPos, baseStack, id, sanitizedNbt);
            return EntityStepOutcome.LAUNCHED;
        }
        return EntityStepOutcome.PAUSED;
    }

    private enum BuildPhase {
        DESTROY,
        SINGLE_PRIMARY,
        SINGLE_DEPENDENT,
        MULTI_BLOCK,
        POST_MULTI_DEPENDENT,
        ENTITIES,
        DONE
    }

    private static class ActiveBuildPlan {
        final CompoundTag blueprintSnapshot;
        final boolean destroyMode;
        final boolean exactMatch;
        final ParsedBlueprint parsed;
        final Deque<BlockPos> destroyQueue = new ArrayDeque<>();
        final Deque<BlueprintCell> primarySingleQueue = new ArrayDeque<>();
        final Deque<BlueprintCell> deferredSingleQueue = new ArrayDeque<>();
        final Deque<MultiBlockGroup> multiQueue = new ArrayDeque<>();
        final Deque<MultiBlockGroup> deferredMultiQueue = new ArrayDeque<>();
        final Deque<Integer> entityQueue = new ArrayDeque<>();
        BuildPhase phase;
        int deferredSinglePassRemaining = 0;
        int deferredMultiPassRemaining = 0;
        int destroyTotalInitial = 0;
        int destroyDone = 0;
        int placementTotal = 0;
        int placementDone = 0;

        private ActiveBuildPlan(
                CompoundTag blueprintSnapshot,
                boolean destroyMode,
                boolean exactMatch,
                ParsedBlueprint parsed
        ) {
            this.blueprintSnapshot = blueprintSnapshot;
            this.destroyMode = destroyMode;
            this.exactMatch = exactMatch;
            this.parsed = parsed;
        }

        static ActiveBuildPlan create(CompoundTag blueprintTag, Level level, boolean destroyMode, boolean exactMatch) {
            ParsedBlueprint parsed = BlueprintBuildPlanner.parseBlueprint(blueprintTag, level);
            ActiveBuildPlan plan = new ActiveBuildPlan(blueprintTag.copy(), destroyMode, exactMatch, parsed);

            if (destroyMode) {
                for (BlockPos candidate : parsed.destroyCandidatesTopDown()) {
                    BlockState worldState = level.getBlockState(candidate);
                    if (!BlueprintBuildPlanner.isBreakableBlock(level, candidate, worldState)) {
                        continue;
                    }
                    BlueprintCell cell = parsed.cellMap().get(candidate);
                    if (cell == null || !BlueprintBuildPlanner.isStateMatch(worldState, cell.state(), exactMatch)) {
                        plan.destroyQueue.addLast(candidate);
                    }
                }
                plan.destroyTotalInitial = plan.destroyQueue.size();
            }

            plan.primarySingleQueue.addAll(parsed.singleCellsBottomUp());
            plan.multiQueue.addAll(parsed.multiGroupsBottomUp());
            for (int i = 0; i < parsed.entityCount(); i++) {
                plan.entityQueue.addLast(i);
            }

            int multiPartSum = 0;
            for (MultiBlockGroup g : parsed.multiGroupsBottomUp()) {
                multiPartSum += g.parts().size();
            }
            plan.placementTotal = parsed.singleCellsBottomUp().size() + multiPartSum + parsed.entityCount();

            plan.phase = (destroyMode && !plan.destroyQueue.isEmpty())
                    ? BuildPhase.DESTROY
                    : BuildPhase.SINGLE_PRIMARY;
            return plan;
        }

        boolean matches(CompoundTag blueprintTag, boolean destroyMode, boolean exactMatch) {
            return this.destroyMode == destroyMode
                    && this.exactMatch == exactMatch
                    && this.blueprintSnapshot.equals(blueprintTag);
        }

        int getTotalWork() {
            return Math.max(1, Math.max(destroyTotalInitial, destroyDone) + placementTotal);
        }

        int getCompletedWork() {
            return Math.min(getTotalWork(), destroyDone + placementDone);
        }
    }

    public NonNullList<ItemStack> getItems() {
        return this.items;
    }

    // Container implementation
    @Override
    public int getContainerSize() {
        return this.items.size();
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

    private int getSlurryDurability() {
        return this.fuel;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.tuanzis_mod.blueprint_cannon");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BlueprintCannonMenu(containerId, playerInventory, this, this.dataAccess);
    }

    public void handleAction(String action, boolean destroyMode, boolean exactMatch) {
        boolean settingsChanged = (this.destroyMode != destroyMode) || (this.exactMatch != exactMatch);
        this.destroyMode = destroyMode;
        this.exactMatch = exactMatch;
        this.pauseReason = 0;
        if (settingsChanged) {
            this.activePlan = null;
        }
        switch (action) {
            case "START" -> {
                // 从 IDLE（含任务完成后）重新启动时，必须重置进度
                // 从 PAUSE 恢复时不重置，以继续上次进度
                if (this.buildState == 0) {
                    this.currentIndex = 0;
                    this.activePlan = null;
                    me.tuanzi.util.ModLog.debug("START from IDLE: 重置进度 currentIndex=0");
                }
                this.buildState = 1;
                this.cachedSources = null; // 重新触发扫描
            }
            case "PAUSE" -> this.buildState = 2;
            case "TERMINATE" -> {
                this.buildState = 0;
                this.currentIndex = 0;
                this.cachedSources = null;
                this.activePlan = null;
                this.currentTarget = null;
                this.previousTarget = null;
                if (this.level != null && !this.level.isClientSide()) {
                    BlockState s = this.getBlockState();
                    this.level.sendBlockUpdated(this.worldPosition, s, s, 3);
                }
            }
        }
        this.setChanged();
    }

    /**
     * 服务端：将蓝图材料需求统计写入 Slot 2 中的书与笔，产出成书。
     * 格式：物品名: 现有 / 总需
     * 绿色行（✔）表示材料充足，红色行（✗）表示材料缺失。
     */
    public boolean writeMaterialBook(Level level, BlockPos cannonPos) {
        // Slot 2 必须放有书与笔或已生成的成书
        ItemStack bookStack = this.items.get(2);
        if (!bookStack.is(Items.WRITABLE_BOOK) && !bookStack.is(Items.WRITTEN_BOOK)) {
            return false;
        }

        // 读取蓝图
        ItemStack blueprint = this.items.get(0);
        if (blueprint.isEmpty() || !(blueprint.getItem() instanceof StructureBlueprintItem)) {
            return false;
        }
        CustomData customData = blueprint.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return false;
        CompoundTag blueprintTag = customData.copyTag();
        if (!blueprintTag.contains("Width")) return false;

        int[] blocks = blueprintTag.getIntArray("Blocks").orElse(new int[0]);
        if (blocks.length == 0) return false;

        // 使用与建造一致的多方块分组逻辑统计材料总需量（如床/门每组计为 1 件）
        ParsedBlueprint parsed = BlueprintBuildPlanner.parseBlueprint(blueprintTag, level);
        Map<Item, Integer> totalNeeded = new LinkedHashMap<>();
        for (BlueprintCell cell : parsed.singleCellsBottomUp()) {
            Item item = cell.state().getBlock().asItem();
            if (item == Items.AIR) continue;
            totalNeeded.merge(item, 1, Integer::sum);
        }
        for (MultiBlockGroup group : parsed.multiGroupsBottomUp()) {
            if (group.requiredItem() != Items.AIR) {
                totalNeeded.merge(group.requiredItem(), 1, Integer::sum);
            }
        }

        // 统计非活体建筑实体与挂载物所需物料
        if (blueprintTag.contains("Entities")) {
            ListTag entitiesTag = blueprintTag.getListOrEmpty("Entities");
            for (int i = 0; i < entitiesTag.size(); i++) {
                CompoundTag entityNbt = entitiesTag.getCompoundOrEmpty(i);
                String id = entityNbt.getStringOr("id", "");
                if (me.tuanzi.block.cannon.BlueprintEntityHelper.isIgnoredEntity(id)) continue;
                if (!me.tuanzi.block.cannon.BlueprintEntityHelper.isBuildableNonLivingEntity(id)) continue;

                List<me.tuanzi.block.cannon.BlueprintEntityHelper.EntityRequirement> reqs =
                        me.tuanzi.block.cannon.BlueprintEntityHelper.extractRequirements(id, entityNbt, level.registryAccess());
                for (me.tuanzi.block.cannon.BlueprintEntityHelper.EntityRequirement req : reqs) {
                    if (!req.template().isEmpty()) {
                        totalNeeded.merge(req.template().getItem(), 1, Integer::sum);
                    }
                }
            }
        }

        // 统计周围容器现有数量（服务端扫描，可靠）
        Map<Item, Integer> available = new HashMap<>();
        int radius = 5;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos p = cannonPos.offset(dx, dy, dz);
                    BlockEntity be = level.getBlockEntity(p);
                    if (be instanceof Container container && be != this) {
                        countContainerItems(container, available, 1);
                    }
                }
            }
        }

        // 按是否足量分组，不足的在前，足量的在后
        List<Map.Entry<Item, Integer>> missing = new ArrayList<>();
        List<Map.Entry<Item, Integer>> fulfilled = new ArrayList<>();
        for (Map.Entry<Item, Integer> entry : totalNeeded.entrySet()) {
            int avail = available.getOrDefault(entry.getKey(), 0);
            if (avail >= entry.getValue()) {
                fulfilled.add(entry);
            } else {
                missing.add(entry);
            }
        }

        // 组装书页（每页最多 13 行，每行约 25 字符）
        final int LINES_PER_PAGE = 13;
        List<Component> allComponents = new ArrayList<>();

        if (!missing.isEmpty()) {
            allComponents.add(Component.literal("=== 缺少材料 ==="));
            for (Map.Entry<Item, Integer> e : missing) {
                int avail = available.getOrDefault(e.getKey(), 0);
                allComponents.add(
                        Component.literal("§c✗ ")
                                .append(Component.translatable(e.getKey().getDescriptionId()))
                                .append(Component.literal(": " + avail + "/" + e.getValue()))
                );
            }
        }
        if (!fulfilled.isEmpty()) {
            allComponents.add(Component.literal("=== 足量材料 ==="));
            for (Map.Entry<Item, Integer> e : fulfilled) {
                int avail = available.getOrDefault(e.getKey(), 0);
                allComponents.add(
                        Component.literal("§a✔ ")
                                .append(Component.translatable(e.getKey().getDescriptionId()))
                                .append(Component.literal(": " + avail + "/" + e.getValue()))
                );
            }
        }
        if (allComponents.isEmpty()) {
            allComponents.add(Component.literal("蓝图中无需要的材料。"));
        }

        // 按 LINES_PER_PAGE 切分页面
        List<Filterable<Component>> pages = new ArrayList<>();
        int lineIdx = 0;
        while (lineIdx < allComponents.size()) {
            net.minecraft.network.chat.MutableComponent pageComponent = Component.empty();
            for (int l = 0; l < LINES_PER_PAGE && lineIdx < allComponents.size(); l++, lineIdx++) {
                if (l > 0) {
                    pageComponent.append("\n");
                }
                pageComponent.append(allComponents.get(lineIdx));
            }
            pages.add(Filterable.passThrough(pageComponent));
        }

        // 生成成书并替换 Slot 2
        ItemStack writtenBook = new ItemStack(Items.WRITTEN_BOOK);
        writtenBook.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
                Filterable.passThrough("材料清单"),   // title
                "蓝图大炮",                           // author
                0,                                   // generation
                pages,
                true                                 // resolved
        ));
        this.items.set(2, writtenBook);
        this.setChanged();
        me.tuanzi.util.ModLog.debug("已写入材料清单：" + missing.size() + " 种缺失，" + fulfilled.size() + " 种足量，共 " + pages.size() + " 页，坐标：" + cannonPos);
        return true;
    }

    /**
     * 递归扫描容器，统计各物品数量到 countMap
     */
    private void countContainerItems(Container container, Map<Item, Integer> countMap, int depth) {
        if (depth > 3) return;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) continue;
            if (isNestedContainerItem(stack)) {
                ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
                if (contents != null) {
                    int containerSize = getNestedContainerSize(stack);
                    NonNullList<ItemStack> inner = NonNullList.withSize(containerSize, ItemStack.EMPTY);
                    contents.copyInto(inner);
                    SimpleContainer virtual = new SimpleContainer(inner.toArray(new ItemStack[0]));
                    countContainerItems(virtual, countMap, depth + 1);
                }
            } else {
                countMap.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
    }

    public void tickFlyingBlocks(Level level) {
        if (this.flyingBlocks.isEmpty()) return;
        List<LaunchedItem> toRemove = new LinkedList<>();
        for (LaunchedItem item : this.flyingBlocks) {
            if (item.update(level)) {
                toRemove.add(item);
            }
        }
        if (!toRemove.isEmpty()) {
            this.flyingBlocks.removeAll(toRemove);
            this.setChanged();
        }
    }

    public void launchBlock(Level level, BlockPos target, ItemStack stack, BlockState state, @Nullable CompoundTag data, boolean isDestroy) {
        launchBlock(level, target, stack, state, data, isDestroy, false);
    }

    public void launchBlock(Level level, BlockPos target, ItemStack stack, BlockState state, @Nullable CompoundTag data, boolean isDestroy, boolean forceKeepShape) {
        if (this.currentTarget != null && !this.currentTarget.equals(target)) {
            this.previousTarget = this.currentTarget;
        }
        this.currentTarget = target;
        int totalTicks = LaunchedItem.ticksForDistance(this.worldPosition, target);
        this.flyingBlocks.add(new LaunchedItem.ForBlockState(this.worldPosition, target, stack, state, data, isDestroy, forceKeepShape));

        // 播放大炮轰击发射音效
        level.playSound(null, this.worldPosition, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.75f, 1.4f);
        level.playSound(null, this.worldPosition, net.minecraft.sounds.SoundEvents.DISPENSER_LAUNCH, SoundSource.BLOCKS, 1.0f, 0.8f);

        if (level instanceof ServerLevel serverLevel) {
            BlueprintCannonShootS2CPacket packet = new BlueprintCannonShootS2CPacket(
                    this.worldPosition, target, net.minecraft.world.level.block.Block.getId(state), totalTicks, isDestroy
            );
            for (ServerPlayer player : PlayerLookup.tracking(serverLevel, this.worldPosition)) {
                ServerPlayNetworking.send(player, packet);
            }
        }
        this.setChanged();
    }

    public void launchMultiBlock(Level level, MultiBlockGroup group, ItemStack stack, boolean forceKeepShape) {
        if (group.parts().isEmpty()) return;
        BlueprintCell primary = group.parts().getFirst();
        BlockPos target = primary.pos();
        if (this.currentTarget != null && !this.currentTarget.equals(target)) {
            this.previousTarget = this.currentTarget;
        }
        this.currentTarget = target;
        int totalTicks = LaunchedItem.ticksForDistance(this.worldPosition, target);

        List<LaunchedItem.ForMultiBlock.Part> launchParts = new ArrayList<>(group.parts().size());
        for (BlueprintCell cell : group.parts()) {
            launchParts.add(new LaunchedItem.ForMultiBlock.Part(cell.pos(), cell.state(), cell.teData()));
        }
        this.flyingBlocks.add(new LaunchedItem.ForMultiBlock(this.worldPosition, target, stack, launchParts, forceKeepShape));

        level.playSound(null, this.worldPosition, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.75f, 1.4f);
        level.playSound(null, this.worldPosition, net.minecraft.sounds.SoundEvents.DISPENSER_LAUNCH, SoundSource.BLOCKS, 1.0f, 0.8f);

        if (level instanceof ServerLevel serverLevel) {
            BlueprintCannonShootS2CPacket packet = new BlueprintCannonShootS2CPacket(
                    this.worldPosition, target, net.minecraft.world.level.block.Block.getId(primary.state()), totalTicks, false
            );
            for (ServerPlayer player : PlayerLookup.tracking(serverLevel, this.worldPosition)) {
                ServerPlayNetworking.send(player, packet);
            }
        }
        this.setChanged();
    }

    public void launchEntity(Level level, BlockPos target, ItemStack stack, String entityId, @Nullable CompoundTag entityNbt) {
        launchEntity(level, net.minecraft.world.phys.Vec3.atBottomCenterOf(target), stack, entityId, entityNbt);
    }

    public void launchEntity(Level level, net.minecraft.world.phys.Vec3 exactTarget, ItemStack stack, String entityId, @Nullable CompoundTag entityNbt) {
        BlockPos target = BlockPos.containing(exactTarget);
        if (this.currentTarget != null && !this.currentTarget.equals(target)) {
            this.previousTarget = this.currentTarget;
        }
        this.currentTarget = target;
        int totalTicks = LaunchedItem.ticksForDistance(this.worldPosition, target);
        this.flyingBlocks.add(new LaunchedItem.ForEntity(this.worldPosition, target, exactTarget, stack, entityId, entityNbt));

        level.playSound(null, this.worldPosition, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.75f, 1.4f);
        level.playSound(null, this.worldPosition, net.minecraft.sounds.SoundEvents.DISPENSER_LAUNCH, SoundSource.BLOCKS, 1.0f, 0.8f);

        if (level instanceof ServerLevel serverLevel) {
            BlueprintCannonShootS2CPacket packet = new BlueprintCannonShootS2CPacket(
                    this.worldPosition, target, 0, totalTicks, false
            );
            for (ServerPlayer player : PlayerLookup.tracking(serverLevel, this.worldPosition)) {
                ServerPlayNetworking.send(player, packet);
            }
        }
        this.setChanged();
    }

    private boolean consumeFuel(Level level, BlockPos pos) {
        if (this.fuel > 0) {
            this.fuel--;
            this.setChanged();
            return true;
        }

        ItemStack slurry = this.items.get(1);
        if (slurry.is(ModItems.COMPRESSED_BUILD_SLURRY)) {
            slurry.shrink(1);
            this.fuel = 63;
            this.setChanged();
            return true;
        }

        List<Player> nearbyPlayers = level.getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(pos).inflate(16));
        for (Player p : nearbyPlayers) {
            if (p.isCreative()) {
                return true;
            }
        }

        this.buildState = 2;
        this.pauseReason = 1;
        level.playSound(null, pos, SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.BLOCKS, 1.0f, 0.8f);
        this.setChanged();
        return false;
    }

    public boolean hasCreativeCoreInSources() {
        if (this.cachedSources == null) return false;
        for (ContainerSource src : this.cachedSources) {
            ItemStack stack = src.container.getItem(src.slot);
            if (!stack.isEmpty() && stack.is(ModItems.CREATIVE_CORE)) {
                return true;
            }
        }
        return false;
    }

    private ContainerSource findItemInSources(Item item) {
        if (this.cachedSources == null) return null;
        for (ContainerSource src : this.cachedSources) {
            ItemStack stack = src.container.getItem(src.slot);
            if (!stack.isEmpty() && stack.is(item)) {
                return src;
            }
        }
        return null;
    }

    private void scanNearbyContainers(Level level, BlockPos cannonPos) {
        this.cachedSources = new ArrayList<>();
        int radius = 5;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos p = cannonPos.offset(x, y, z);
                    BlockEntity be = level.getBlockEntity(p);
                    if (be instanceof Container container && be != this) {
                        scanContainerRecursive(container, 1, null, -1);
                    }
                }
            }
        }
    }

    private void scanContainerRecursive(Container container, int depth, ContainerSource parent, int parentSlot) {
        if (depth > 3) return; // 深度防嵌套限制为 3

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) continue;

            if (isNestedContainerItem(stack)) {
                ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
                if (contents != null) {
                    int containerSize = getNestedContainerSize(stack);
                    NonNullList<ItemStack> list = NonNullList.withSize(containerSize, ItemStack.EMPTY);
                    contents.copyInto(list);
                    SimpleContainer virtualContainer = new SimpleContainer(list.toArray(new ItemStack[0]));
                    ContainerSource parentSource = new ContainerSource(container, i, stack, parent);
                    scanContainerRecursive(virtualContainer, depth + 1, parentSource, i);
                }
            } else {
                this.cachedSources.add(new ContainerSource(container, i, null, parent));
            }
        }
    }

    private boolean isNestedContainerItem(ItemStack stack) {
        return isShulkerBox(stack) || stack.is(ModItems.GOLDEN_POCKET);
    }

    private int getNestedContainerSize(ItemStack stack) {
        if (stack.is(ModItems.GOLDEN_POCKET)) {
            return GoldenPocketMenu.CONTAINER_SIZE;
        }
        return 27;
    }

    private boolean isShulkerBox(ItemStack stack) {
        return stack.is(net.minecraft.tags.ItemTags.SHULKER_BOXES);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, this.items);
        this.buildState = input.getIntOr("BuildState", 0);
        this.currentIndex = input.getIntOr("CurrentIndex", 0);
        this.destroyMode = input.getBooleanOr("DestroyMode", false);
        this.exactMatch = input.getBooleanOr("ExactMatch", false);
        this.ticksToNextPlace = input.getIntOr("TicksToNext", 5);
        this.fuel = input.getIntOr("Fuel", 0);
        this.pauseReason = input.getIntOr("PauseReason", 0);

        this.flyingBlocks.clear();
        HolderLookup.Provider registries = this.level != null ? this.level.registryAccess() : net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY);
        for (CompoundTag c : input.listOrEmpty("FlyingBlocks", CompoundTag.CODEC)) {
            this.flyingBlocks.add(LaunchedItem.fromNBT(c, registries, net.minecraft.core.registries.BuiltInRegistries.BLOCK));
        }
        if (input.contains("TargetX")) {
            this.currentTarget = new BlockPos(input.getIntOr("TargetX", 0), input.getIntOr("TargetY", 0), input.getIntOr("TargetZ", 0));
        } else {
            this.currentTarget = null;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        output.putInt("BuildState", this.buildState);
        output.putInt("CurrentIndex", this.currentIndex);
        output.putBoolean("DestroyMode", this.destroyMode);
        output.putBoolean("ExactMatch", this.exactMatch);
        output.putInt("TicksToNext", this.ticksToNextPlace);
        output.putInt("Fuel", this.fuel);
        output.putInt("PauseReason", this.pauseReason);

        ValueOutput.TypedOutputList<CompoundTag> listOutput = output.list("FlyingBlocks", CompoundTag.CODEC);
        HolderLookup.Provider registries = this.level != null ? this.level.registryAccess() : net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY);
        for (LaunchedItem b : this.flyingBlocks) {
            listOutput.add(b.serializeNBT(registries));
        }
        if (this.currentTarget != null) {
            output.putInt("TargetX", this.currentTarget.getX());
            output.putInt("TargetY", this.currentTarget.getY());
            output.putInt("TargetZ", this.currentTarget.getZ());
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        ListTag tagFlyingBlocks = new ListTag();
        for (LaunchedItem b : this.flyingBlocks) {
            tagFlyingBlocks.add(b.serializeNBT(registries));
        }
        tag.put("FlyingBlocks", tagFlyingBlocks);
        if (this.currentTarget != null) {
            tag.putInt("TargetX", this.currentTarget.getX());
            tag.putInt("TargetY", this.currentTarget.getY());
            tag.putInt("TargetZ", this.currentTarget.getZ());
        }
        return tag;
    }

    // 内部类，记录物料池中的源信息
    public static class ContainerSource {
        public final Container container;
        public final int slot;
        public final ItemStack shulkerStack; // 潜影盒物品本身
        public final ContainerSource parent;

        public ContainerSource(Container container, int slot, ItemStack shulkerStack, ContainerSource parent) {
            this.container = container;
            this.slot = slot;
            this.shulkerStack = shulkerStack;
            this.parent = parent;
        }

        /**
         * 标记物品已变更，将修改持久化回父链直至根容器（箱子 BlockEntity）。
         * <p>
         * cachedSources 中所有条目的 shulkerStack 均为 null（只有普通物品被添加）。
         * 因此 markChanged() 的调用者 this.shulkerStack 始终为 null。
         * - 若 parent == null：item 直接在箱子中 → 直接 setChanged() 即可。
         * - 若 parent != null：item 在潜影盒的 virtualContainer 中
         * → 将 this.container（virtualContainer）序列化回 parent.shulkerStack 的 NBT
         * → 再调用 parent.propagateChangedToRoot() 沿父链向上逐层传播，直至根箱子。
         */
        public void markChanged() {
            if (this.parent != null) {
                // 当前物品在潜影盒的虚拟容器中：将虚拟容器内容序列化回直接父层潜影盒的 NBT
                NonNullList<ItemStack> list = NonNullList.withSize(this.container.getContainerSize(), ItemStack.EMPTY);
                for (int i = 0; i < list.size(); i++) {
                    list.set(i, this.container.getItem(i));
                }
                this.parent.shulkerStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(list));
                me.tuanzi.util.ModLog.debug("markChanged: virtualContainer 已序列化回父层潜影盒 NBT，继续向上传播");
                // 沿父链向上传播至根容器（箱子）
                this.parent.propagateChangedToRoot();
            } else {
                // 直接在根容器（箱子）中的普通物品，标记箱子改变
                this.container.setChanged();
            }
        }

        /**
         * 沿父链向上逐层传播变更，将每一层的 container（virtualContainer）序列化
         * 回上一层的 shulkerStack，直至到达根节点（无父节点的箱子 ContainerSource），
         * 调用 container.setChanged() 标记箱子 BlockEntity 需要保存。
         * <p>
         * 仅由 markChanged() 内部调用，不应从外部直接调用。
         * <p>
         * 节点结构（以嵌套示例）：
         * 根节点: { container=chest,            shulkerStack=outerShulker, parent=null     }
         * 中间层: { container=outerVirtual,      shulkerStack=innerShulker, parent=根节点  }
         * 叶节点: { container=innerVirtual,      shulkerStack=null,         parent=中间层  }
         */
        private void propagateChangedToRoot() {
            if (this.parent != null) {
                // 当前节点是中间层（container 是某层虚拟容器），继续向上传播
                // 将 this.container（本层 virtualContainer，含已更新的内层数据）
                // 序列化回 this.parent.shulkerStack（上层潜影盒 ItemStack）的 NBT
                NonNullList<ItemStack> list = NonNullList.withSize(this.container.getContainerSize(), ItemStack.EMPTY);
                for (int i = 0; i < list.size(); i++) {
                    list.set(i, this.container.getItem(i));
                }
                this.parent.shulkerStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(list));
                me.tuanzi.util.ModLog.debug("propagateChangedToRoot: 中间层 virtualContainer 序列化完成，继续向上");
                this.parent.propagateChangedToRoot();
            } else {
                // 已到达根节点：this.container 是真实的箱子容器，标记其 BE 需要保存
                me.tuanzi.util.ModLog.debug("propagateChangedToRoot: 到达根容器（箱子），调用 setChanged()");
                this.container.setChanged();
            }
        }
    }
}
