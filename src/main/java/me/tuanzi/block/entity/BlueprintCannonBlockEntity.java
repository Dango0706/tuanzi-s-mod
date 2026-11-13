package me.tuanzi.block.entity;

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
    // 建造状态：0=IDLE, 1=RUNNING, 2=PAUSED
    private int buildState = 0;
    private int currentIndex = 0; // 已处理的蓝图格子索引（进度分子）
    private int totalBlocks = 0;  // 蓝图 blocks 数组总长度（进度分母）——不持久化，每 tick 从蓝图重新读取
    private boolean destroyMode = false;
    private boolean exactMatch = false;
    private int fuel = 0;
    private int pauseReason = 0; // 0=none, 1=no fuel, 2=no blocks
    private int ticksToNextPlace = 5;
    private List<ContainerSource> cachedSources = null;

    public BlueprintCannonBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.BLUEPRINT_CANNON_BLOCK_ENTITY, pos, state);
    }

    public static Item getRequiredItemForEntity(String entityId, CompoundTag nbt) {
        ItemStack base = me.tuanzi.block.cannon.BlueprintEntityHelper.getBaseItemStack(entityId, nbt, null);
        return base.getItem();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, BlueprintCannonBlockEntity be) {
        be.tickFlyingBlocks(level);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BlueprintCannonBlockEntity be) {
        // 每 tick 推动空中所有已发射的方块向目标飞行并在到达时落地筑造/破坏
        be.tickFlyingBlocks(level);

        if (be.buildState != 1) return; // 只在 RUNNING 时工作

        ItemStack blueprint = be.items.get(0);
        if (blueprint.isEmpty() || !(blueprint.getItem() instanceof StructureBlueprintItem)) {
            // 蓝图被拿走：立即重置进度，确保客户端 GUI 清空显示
            be.buildState = 0;
            be.currentIndex = 0;
            be.totalBlocks = 0;
            be.setChanged();
            return;
        }

        CustomData customData = blueprint.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            be.buildState = 0;
            be.currentIndex = 0;
            be.totalBlocks = 0;
            be.setChanged();
            return;
        }

        CompoundTag blueprintTag = customData.copyTag();
        if (!blueprintTag.contains("Width")) {
            be.buildState = 0;
            be.currentIndex = 0;
            be.totalBlocks = 0;
            be.setChanged();
            return;
        }

        int width = blueprintTag.getIntOr("Width", 0);
        int height = blueprintTag.getIntOr("Height", 0);
        int length = blueprintTag.getIntOr("Length", 0);
        int[] blocks = blueprintTag.getIntArray("Blocks").orElse(new int[0]);

        if (blocks.length == 0) {
            be.buildState = 0;
            be.currentIndex = 0;
            be.totalBlocks = 0;
            be.setChanged();
            return;
        }

        ListTag entitiesTag = blueprintTag.getListOrEmpty("Entities");
        int expectedTotal = blocks.length + entitiesTag.size();

        // 每 tick 更新 totalBlocks，确保客户端能实时计算进度
        if (be.totalBlocks != expectedTotal) {
            be.totalBlocks = expectedTotal;
            be.setChanged();
        }

        // 检查燃料槽（创造模式可不耗浆料）
        boolean isCreative = false;
        ItemStack slurryStack = be.items.get(1);

        be.ticksToNextPlace--;
        if (be.ticksToNextPlace <= 0) {
            be.ticksToNextPlace = 0;

            // 惰性扫描容器物料池
            if (be.cachedSources == null) {
                be.scanNearbyContainers(level, pos);
            }

            // 在一个 tick 内最多跳过 512 个空气或已建好的方块，防卡顿与挂起
            int checksThisTick = 0;
            while (checksThisTick < 512 && be.currentIndex < expectedTotal) {
                checksThisTick++;

                // 实体放置分支
                if (be.currentIndex >= blocks.length) {
                    // 1. 等待所有空中飞行方块全部落地，确保墙面、地板与铁轨支撑到位
                    boolean hasFlyingBlocks = be.flyingBlocks.stream().anyMatch(item -> item instanceof LaunchedItem.ForBlockState);
                    if (hasFlyingBlocks) {
                        return;
                    }

                    int entityIdx = be.currentIndex - blocks.length;
                    if (entityIdx < 0 || entityIdx >= entitiesTag.size()) {
                        be.currentIndex++;
                        continue;
                    }

                    CompoundTag entityNbt = entitiesTag.getCompoundOrEmpty(entityIdx);
                    String id = entityNbt.getStringOr("id", "");

                    // 2. 忽略环境与临时实体
                    if (me.tuanzi.block.cannon.BlueprintEntityHelper.isIgnoredEntity(id)) {
                        be.currentIndex++;
                        continue;
                    }

                    boolean hasCreativeCore = be.hasCreativeCoreInSources();
                    boolean isBuildableNonLiving = me.tuanzi.block.cannon.BlueprintEntityHelper.isBuildableNonLivingEntity(id);

                    // 生存模式下非建筑装饰实体（如活体生物：村民、牛羊、怪物）直接跳过，仅在有创造核心时打印
                    if (!isBuildableNonLiving && !hasCreativeCore) {
                        be.currentIndex++;
                        continue;
                    }

                    // 3. 虚影放置信息与亚像素几何变换
                    BlockPos basePos = new BlockPos(blueprintTag.getIntOr("GhostX", 0), blueprintTag.getIntOr("GhostY", 0), blueprintTag.getIntOr("GhostZ", 0));
                    BlockPos offset = new BlockPos(blueprintTag.getIntOr("OffsetX", 0), blueprintTag.getIntOr("OffsetY", 0), blueprintTag.getIntOr("OffsetZ", 0));
                    int rotationIdx = blueprintTag.getIntOr("Rotation", 0);
                    boolean mirrored = blueprintTag.getBooleanOr("Mirrored", false);

                    me.tuanzi.block.cannon.BlueprintEntityHelper.TransformedEntity transformed =
                            me.tuanzi.block.cannon.BlueprintEntityHelper.transformEntity(
                                    entityNbt, width, height, length, basePos, offset, rotationIdx, mirrored, level.registryAccess()
                            );
                    net.minecraft.world.phys.Vec3 exactPos = transformed.exactPos();
                    BlockPos targetBlockPos = transformed.targetBlockPos();
                    net.minecraft.core.Direction facing = transformed.facing();
                    CompoundTag transformedNbt = transformed.transformedNbt();

                    // 4. 空中飞行队列查重
                    boolean alreadyFlying = be.flyingBlocks.stream().anyMatch(item ->
                            item instanceof LaunchedItem.ForEntity forEntity &&
                                    forEntity.exactTarget != null &&
                                    forEntity.exactTarget.distanceToSqr(exactPos) < 0.05 &&
                                    forEntity.entityId.equals(id)
                    );
                    if (alreadyFlying) {
                        be.currentIndex++;
                        continue;
                    }

                    // 5. 世界目标位置实体检测与拆除模式联动
                    net.minecraft.world.entity.EntityType<?> entityType =
                            net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(net.minecraft.resources.Identifier.tryParse(id)).orElse(null);
                    if (entityType == null) {
                        be.currentIndex++;
                        continue;
                    }

                    net.minecraft.world.phys.AABB checkBox = new net.minecraft.world.phys.AABB(
                            exactPos.x - 0.5, exactPos.y - 0.5, exactPos.z - 0.5,
                            exactPos.x + 0.5, exactPos.y + 0.5, exactPos.z + 0.5
                    );

                    boolean shouldSkip = false;
                    boolean needDestroyConflict = false;
                    net.minecraft.world.entity.Entity conflictTarget = null;

                    if (id.contains("item_frame") || id.equals("minecraft:painting")) {
                        // 挂饰类：精准匹配所在锚点方块与朝向
                        List<net.minecraft.world.entity.decoration.HangingEntity> hangings =
                                level.getEntitiesOfClass(net.minecraft.world.entity.decoration.HangingEntity.class, new net.minecraft.world.phys.AABB(targetBlockPos).inflate(0.5));
                        for (net.minecraft.world.entity.decoration.HangingEntity hanging : hangings) {
                            if (hanging.getDirection() == facing) {
                                if (hanging.getType() == entityType) {
                                    shouldSkip = true;
                                    break;
                                } else if (be.destroyMode && !shouldSkip) {
                                    needDestroyConflict = true;
                                    conflictTarget = hanging;
                                }
                            }
                        }
                    } else {
                        // 常规非活体实体（盔甲架、坐垫、车船等）
                        List<? extends net.minecraft.world.entity.Entity> existing = level.getEntities(entityType, checkBox, e -> true);
                        if (!existing.isEmpty()) {
                            shouldSkip = true;
                        } else if (be.destroyMode) {
                            // 检查是否有其他冲突的非活体建筑实体
                            List<net.minecraft.world.entity.Entity> conflicts = level.getEntities((net.minecraft.world.entity.Entity) null, checkBox,
                                    e -> !(e instanceof net.minecraft.world.entity.player.Player) && !(e instanceof net.minecraft.world.entity.LivingEntity living && !(living instanceof net.minecraft.world.entity.decoration.ArmorStand)));
                            if (!conflicts.isEmpty()) {
                                needDestroyConflict = true;
                                conflictTarget = conflicts.getFirst();
                            }
                        }
                    }

                    if (shouldSkip) {
                        be.currentIndex++;
                        continue;
                    }

                    if (needDestroyConflict && conflictTarget != null) {
                        if (be.consumeFuel(level, pos)) {
                            if (level instanceof ServerLevel serverLevel) {
                                conflictTarget.kill(serverLevel);
                            } else {
                                conflictTarget.discard();
                            }
                            be.ticksToNextPlace = 5;
                            be.setChanged();
                            return;
                        }
                        return;
                    }

                    // 6. 材料需求检查与原子化扣料
                    Map<String, ItemStack> consumedItems = new LinkedHashMap<>();
                    if (!hasCreativeCore) {
                        List<me.tuanzi.block.cannon.BlueprintEntityHelper.EntityRequirement> reqs =
                                me.tuanzi.block.cannon.BlueprintEntityHelper.extractRequirements(id, transformedNbt, level.registryAccess());
                        boolean canConsume = me.tuanzi.block.cannon.BlueprintEntityHelper.tryReserveAndConsume(
                                be.cachedSources, reqs, be.exactMatch, consumedItems
                        );
                        if (!canConsume) {
                            // 材料不足，自动暂停
                            be.buildState = 2;
                            be.pauseReason = 2;
                            be.ticksToNextPlace = 0;
                            level.playSound(null, pos, SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.BLOCKS, 1.0f, 0.8f);
                            be.setChanged();
                            return;
                        }
                    }

                    // 7. 消耗燃料并精准发射实体
                    if (be.consumeFuel(level, pos)) {
                        CompoundTag sanitizedNbt = me.tuanzi.block.cannon.BlueprintEntityHelper.buildSanitizedEntityNbt(
                                transformedNbt, id, consumedItems, hasCreativeCore, level.registryAccess()
                        );
                        ItemStack baseStack = consumedItems.getOrDefault("BASE", ItemStack.EMPTY);
                        be.launchEntity(level, exactPos, baseStack, id, sanitizedNbt);
                        be.ticksToNextPlace = 5;

                        be.currentIndex++;
                        be.setChanged();
                        return;
                    }
                    return;
                }

                // 计算当前蓝图一维索引对应的相对三维坐标
                int y = be.currentIndex / (width * length);
                int rem = be.currentIndex % (width * length);
                int z = rem / width;
                int x = rem % width;

                // 虚影放置信息
                BlockPos basePos = new BlockPos(blueprintTag.getIntOr("GhostX", 0), blueprintTag.getIntOr("GhostY", 0), blueprintTag.getIntOr("GhostZ", 0));
                BlockPos offset = new BlockPos(blueprintTag.getIntOr("OffsetX", 0), blueprintTag.getIntOr("OffsetY", 0), blueprintTag.getIntOr("OffsetZ", 0));
                int rotationIdx = blueprintTag.getIntOr("Rotation", 0);
                boolean mirrored = blueprintTag.getBooleanOr("Mirrored", false);

                Rotation rot = Rotation.values()[rotationIdx % 4];
                Mirror mir = mirrored ? Mirror.LEFT_RIGHT : Mirror.NONE;

                // 1. 旋转相对坐标
                int rx = x;
                int rz = z;
                int wRot = width;
                if (rot == Rotation.CLOCKWISE_90) {
                    rx = length - 1 - z;
                    rz = x;
                    wRot = length;
                } else if (rot == Rotation.CLOCKWISE_180) {
                    rx = width - 1 - x;
                    rz = length - 1 - z;
                    wRot = width;
                } else if (rot == Rotation.COUNTERCLOCKWISE_90) {
                    rx = z;
                    rz = width - 1 - x;
                    wRot = length;
                }

                // 2. 镜像相对坐标
                if (mirrored) {
                    rx = wRot - 1 - rx;
                }

                BlockPos targetPos = basePos.offset(offset).offset(rx, y, rz);

                // 解码蓝图里的 BlockState
                ListTag paletteNbt = blueprintTag.getListOrEmpty("Palette");
                int paletteIdx = blocks[be.currentIndex];
                if (paletteIdx < 0 || paletteIdx >= paletteNbt.size()) {
                    be.currentIndex++;
                    continue;
                }

                net.minecraft.nbt.Tag stateTag = paletteNbt.get(paletteIdx);
                BlockState blueprintState = BlockState.CODEC.parse(NbtOps.INSTANCE, stateTag)
                        .result().orElseGet(() -> me.tuanzi.util.LitematicaParser.parseBlockStateFromTag(stateTag));
                // 应用旋转和镜像转换
                BlockState transformedState = blueprintState.rotate(rot).mirror(mir);

                // 核心：无视蓝图中的空气方块与占位/结构空位/屏障方块，不打印日志，不发射空气飞弹
                boolean isBlueprintAir = transformedState.isAir() || transformedState.is(Blocks.STRUCTURE_VOID) || transformedState.is(Blocks.BARRIER);
                if (isBlueprintAir) {
                    // 仅当开启拆除模式且世界上存在需要清空的实体方块时，才发射破坏飞弹
                    if (be.destroyMode) {
                        BlockState currentWorldState = level.getBlockState(targetPos);
                        if (!currentWorldState.isAir() && currentWorldState.getFluidState().isEmpty() && currentWorldState.getDestroySpeed(level, targetPos) >= 0) {
                            if (be.consumeFuel(level, pos)) {
                                be.launchBlock(level, targetPos, ItemStack.EMPTY, Blocks.AIR.defaultBlockState(), null, true);
                                be.ticksToNextPlace = 5;
                                be.currentIndex++;
                                be.setChanged();
                                return;
                            }
                        }
                    }
                    // 正常建造模式或世界已经是空气：彻底无视并跳过
                    be.currentIndex++;
                    continue;
                }

                // 蓝图为实体方块，获取世界当前方块状态
                BlockState currentWorldState = level.getBlockState(targetPos);

                // 检查是否已经匹配
                boolean isMatch = false;
                if (be.exactMatch) {
                    isMatch = currentWorldState == transformedState;
                } else {
                    isMatch = currentWorldState.getBlock() == transformedState.getBlock();
                }

                if (isMatch) {
                    // 已是正确方块，跳过
                    be.currentIndex++;
                    continue;
                }

                // 世界目标位置有方块且不匹配
                if (!currentWorldState.isAir()) {
                    if (be.destroyMode && currentWorldState.getFluidState().isEmpty() && currentWorldState.getDestroySpeed(level, targetPos) >= 0) {
                        // 开启拆除模式：发射破坏飞弹清空障碍，等待清空后下一次放置实体方块
                        if (be.consumeFuel(level, pos)) {
                            be.launchBlock(level, targetPos, ItemStack.EMPTY, Blocks.AIR.defaultBlockState(), null, true);
                            be.ticksToNextPlace = 5;
                            be.setChanged();
                            return;
                        }
                    } else {
                        // 未开启拆除模式：禁止破坏世界原有方块，直接跳过该位置
                        be.currentIndex++;
                        continue;
                    }
                }

                // 目标位置已为空气，准备扣料放置真实方块
                Item requiredItem = transformedState.getBlock().asItem();
                if (requiredItem == Items.AIR) {
                    // 特殊无物品形态方块（如某些技术方块），直接跳过
                    be.currentIndex++;
                    continue;
                }

                me.tuanzi.util.ModLog.debug("准备放置: " + transformedState + " -> requiredItem: " + requiredItem + " at " + targetPos);

                boolean hasCreativeCore = be.hasCreativeCoreInSources();
                ContainerSource source = be.findItemInSources(requiredItem);
                if (source != null || hasCreativeCore) {
                    // 扣除燃料
                    if (be.consumeFuel(level, pos)) {
                        if (source != null && !hasCreativeCore) {
                            // 扣除物品
                            ItemStack srcStack = source.container.getItem(source.slot);
                            srcStack.shrink(1);
                            source.markChanged();
                        } else if (hasCreativeCore) {
                            me.tuanzi.util.ModLog.debug("检测到创造核心: 无视材料缺失或不消耗材料直接放置方块: " + transformedState + " at " + targetPos);
                        }

                        // 提取方块实体 NBT
                        CompoundTag teData = null;
                        ListTag tileEntities = blueprintTag.getListOrEmpty("BlockEntities");
                        for (int i = 0; i < tileEntities.size(); i++) {
                            CompoundTag te = tileEntities.getCompoundOrEmpty(i);
                            int[] tpos = te.getIntArray("pos").orElse(new int[0]);
                            if (tpos.length == 3 && tpos[0] == x && tpos[1] == y && tpos[2] == z) {
                                teData = te.getCompoundOrEmpty("nbt").copy();
                                break;
                            }
                        }

                        ItemStack placedStack = (source != null && !hasCreativeCore) ? source.container.getItem(source.slot).copyWithCount(1) : new ItemStack(transformedState.getBlock());
                        be.launchBlock(level, targetPos, placedStack, transformedState, teData, false);
                        be.ticksToNextPlace = 5;

                        be.currentIndex++;
                        be.setChanged();
                        return;
                    }
                } else {
                    // 缺料，自动暂停
                    be.buildState = 2;
                    be.pauseReason = 2;
                    be.ticksToNextPlace = 0;
                    level.playSound(null, pos, SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.BLOCKS, 1.0f, 0.8f);
                    be.setChanged();
                    return;
                }
            }

            // 如果全部跑完
            if (be.currentIndex >= expectedTotal) {
                be.totalBlocks = expectedTotal;
                be.buildState = 0;
                be.ticksToNextPlace = 0;
                me.tuanzi.util.ModLog.debug("蓝图建造完成，currentIndex=" + be.currentIndex + " totalBlocks=" + be.totalBlocks);
                level.playSound(null, pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 1.0f, 1.2f);
                be.setChanged();
            }
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
        this.destroyMode = destroyMode;
        this.exactMatch = exactMatch;
        this.pauseReason = 0;
        switch (action) {
            case "START" -> {
                // 从 IDLE（含任务完成后）重新启动时，必须重置进度
                // 从 PAUSE 恢复时不重置，以继续上次进度
                if (this.buildState == 0) {
                    this.currentIndex = 0;
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

        int width = blueprintTag.getIntOr("Width", 0);
        int height = blueprintTag.getIntOr("Height", 0);
        int length = blueprintTag.getIntOr("Length", 0);
        int[] blocks = blueprintTag.getIntArray("Blocks").orElse(new int[0]);
        if (blocks.length == 0) return false;

        // 解码调色板
        ListTag paletteNbt = blueprintTag.getListOrEmpty("Palette");
        BlockState[] blockStates = new BlockState[paletteNbt.size()];
        for (int i = 0; i < paletteNbt.size(); i++) {
            net.minecraft.nbt.Tag stateTag = paletteNbt.get(i);
            blockStates[i] = BlockState.CODEC.parse(NbtOps.INSTANCE, stateTag)
                    .result().orElseGet(() -> me.tuanzi.util.LitematicaParser.parseBlockStateFromTag(stateTag));
        }

        // 统计蓝图总需量
        Map<Item, Integer> totalNeeded = new LinkedHashMap<>();
        for (int idx = 0; idx < blocks.length; idx++) {
            int paletteIdx = blocks[idx];
            if (paletteIdx < 0 || paletteIdx >= blockStates.length) continue;
            BlockState state = blockStates[paletteIdx];
            if (state.isAir() || state.is(Blocks.BARRIER)) continue;
            Item item = state.getBlock().asItem();
            if (item == Blocks.AIR.asItem()) continue;
            totalNeeded.merge(item, 1, Integer::sum);
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
        if (this.currentTarget != null && !this.currentTarget.equals(target)) {
            this.previousTarget = this.currentTarget;
        }
        this.currentTarget = target;
        int totalTicks = LaunchedItem.ticksForDistance(this.worldPosition, target);
        this.flyingBlocks.add(new LaunchedItem.ForBlockState(this.worldPosition, target, stack, state, data, isDestroy));

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
