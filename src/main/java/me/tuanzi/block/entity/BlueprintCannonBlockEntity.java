package me.tuanzi.block.entity;

import me.tuanzi.init.ModBlocks;
import me.tuanzi.init.ModItems;
import me.tuanzi.item.StructureBlueprintItem;
import me.tuanzi.world.inventory.BlueprintCannonMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
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
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class BlueprintCannonBlockEntity extends BlockEntity implements MenuProvider, Container {

    // 3 个物品槽：Slot 0 为蓝图，Slot 1 为压缩浆料燃料，Slot 2 为书与笔（材料清单输出）
    private final NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);

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

    public BlueprintCannonBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.BLUEPRINT_CANNON_BLOCK_ENTITY, pos, state);
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
    public static Item getRequiredItemForEntity(String entityId, CompoundTag nbt) {
        if (entityId == null) return Items.AIR;
        switch (entityId) {
            case "minecraft:armor_stand": return Items.ARMOR_STAND;
            case "minecraft:item_frame": return Items.ITEM_FRAME;
            case "minecraft:glow_item_frame": return Items.GLOW_ITEM_FRAME;
            case "minecraft:painting": return Items.PAINTING;
            case "minecraft:boat": {
                String type = nbt.getStringOr("Type", "");
                if ("spruce".equals(type)) return Items.SPRUCE_BOAT;
                if ("birch".equals(type)) return Items.BIRCH_BOAT;
                if ("jungle".equals(type)) return Items.JUNGLE_BOAT;
                if ("acacia".equals(type)) return Items.ACACIA_BOAT;
                if ("dark_oak".equals(type)) return Items.DARK_OAK_BOAT;
                if ("mangrove".equals(type)) return Items.MANGROVE_BOAT;
                if ("cherry".equals(type)) return Items.CHERRY_BOAT;
                if ("pale_oak".equals(type)) return Items.PALE_OAK_BOAT;
                if ("bamboo".equals(type)) return Items.BAMBOO_RAFT;
                return Items.OAK_BOAT;
            }
            case "minecraft:chest_boat": {
                String type = nbt.getStringOr("Type", "");
                if ("spruce".equals(type)) return Items.SPRUCE_CHEST_BOAT;
                if ("birch".equals(type)) return Items.BIRCH_CHEST_BOAT;
                if ("jungle".equals(type)) return Items.JUNGLE_CHEST_BOAT;
                if ("acacia".equals(type)) return Items.ACACIA_CHEST_BOAT;
                if ("dark_oak".equals(type)) return Items.DARK_OAK_CHEST_BOAT;
                if ("mangrove".equals(type)) return Items.MANGROVE_CHEST_BOAT;
                if ("cherry".equals(type)) return Items.CHERRY_CHEST_BOAT;
                if ("pale_oak".equals(type)) return Items.PALE_OAK_CHEST_BOAT;
                if ("bamboo".equals(type)) return Items.BAMBOO_CHEST_RAFT;
                return Items.OAK_CHEST_BOAT;
            }
            case "minecraft:minecart": return Items.MINECART;
            case "minecraft:chest_minecart": return Items.CHEST_MINECART;
            case "minecraft:furnace_minecart": return Items.FURNACE_MINECART;
            case "minecraft:tnt_minecart": return Items.TNT_MINECART;
            case "minecraft:hopper_minecart": return Items.HOPPER_MINECART;
            case "minecraft:end_crystal": return Items.END_CRYSTAL;
            
            // 常见生物的 Spawn Egg
            case "minecraft:cow": return Items.COW_SPAWN_EGG;
            case "minecraft:sheep": return Items.SHEEP_SPAWN_EGG;
            case "minecraft:pig": return Items.PIG_SPAWN_EGG;
            case "minecraft:chicken": return Items.CHICKEN_SPAWN_EGG;
            case "minecraft:villager": return Items.VILLAGER_SPAWN_EGG;
            case "minecraft:zombie": return Items.ZOMBIE_SPAWN_EGG;
            case "minecraft:skeleton": return Items.SKELETON_SPAWN_EGG;
            case "minecraft:creeper": return Items.CREEPER_SPAWN_EGG;
            case "minecraft:spider": return Items.SPIDER_SPAWN_EGG;
        }
        return Items.AIR;
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

        int width  = blueprintTag.getIntOr("Width",  0);
        int height = blueprintTag.getIntOr("Height", 0);
        int length = blueprintTag.getIntOr("Length", 0);
        int[] blocks = blueprintTag.getIntArray("Blocks").orElse(new int[0]);
        if (blocks.length == 0) return false;

        // 解码调色板
        ListTag paletteNbt = blueprintTag.getListOrEmpty("Palette");
        BlockState[] blockStates = new BlockState[paletteNbt.size()];
        for (int i = 0; i < paletteNbt.size(); i++) {
            CompoundTag stateTag = paletteNbt.getCompoundOrEmpty(i);
            blockStates[i] = BlockState.CODEC.parse(NbtOps.INSTANCE, stateTag)
                .result().orElse(Blocks.AIR.defaultBlockState());
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

        // 统计实体所需物料
        if (blueprintTag.contains("Entities")) {
            ListTag entitiesTag = blueprintTag.getListOrEmpty("Entities");
            for (int i = 0; i < entitiesTag.size(); i++) {
                CompoundTag entityNbt = entitiesTag.getCompoundOrEmpty(i);
                String id = entityNbt.getStringOr("id", "");
                Item item = getRequiredItemForEntity(id, entityNbt);
                if (item != Items.AIR) {
                    totalNeeded.merge(item, 1, Integer::sum);
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
        List<Map.Entry<Item, Integer>> missing   = new ArrayList<>();
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

    /** 递归扫描容器，统计各物品数量到 countMap */
    private void countContainerItems(Container container, Map<Item, Integer> countMap, int depth) {
        if (depth > 3) return;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) continue;
            if (isShulkerBox(stack)) {
                ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
                if (contents != null) {
                    NonNullList<ItemStack> inner = NonNullList.withSize(27, ItemStack.EMPTY);
                    contents.copyInto(inner);
                    SimpleContainer virtual = new SimpleContainer(inner.toArray(new ItemStack[0]));
                    countContainerItems(virtual, countMap, depth + 1);
                }
            } else {
                countMap.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BlueprintCannonBlockEntity be) {
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
            be.ticksToNextPlace = 5;

            // 惰性扫描容器物料池
            if (be.cachedSources == null) {
                be.scanNearbyContainers(level, pos);
            }

            // 在一个 tick 内最多跳过 32 个空气或已建好的方块，防卡顿
            int checksThisTick = 0;
            while (checksThisTick < 32 && be.currentIndex < expectedTotal) {
                checksThisTick++;

                // 实体放置分支
                if (be.currentIndex >= blocks.length) {
                    int entityIdx = be.currentIndex - blocks.length;
                    if (entityIdx < 0 || entityIdx >= entitiesTag.size()) {
                        be.currentIndex++;
                        continue;
                    }

                    CompoundTag entityNbt = entitiesTag.getCompoundOrEmpty(entityIdx);
                    String id = entityNbt.getStringOr("id", "");

                    // 1. 获取实体在蓝图中的相对坐标
                    ListTag posList = entityNbt.getListOrEmpty("Pos");
                    if (posList.size() < 3) {
                        be.currentIndex++;
                        continue;
                    }
                    double ex = posList.getDoubleOr(0, 0.0);
                    double ey = posList.getDoubleOr(1, 0.0);
                    double ez = posList.getDoubleOr(2, 0.0);

                    // 2. 虚影放置信息
                    BlockPos basePos = new BlockPos(blueprintTag.getIntOr("GhostX", 0), blueprintTag.getIntOr("GhostY", 0), blueprintTag.getIntOr("GhostZ", 0));
                    BlockPos offset = new BlockPos(blueprintTag.getIntOr("OffsetX", 0), blueprintTag.getIntOr("OffsetY", 0), blueprintTag.getIntOr("OffsetZ", 0));
                    int rotationIdx = blueprintTag.getIntOr("Rotation", 0);
                    boolean mirrored = blueprintTag.getBooleanOr("Mirrored", false);

                    Rotation rot = Rotation.values()[rotationIdx % 4];
                    Mirror mir = mirrored ? Mirror.LEFT_RIGHT : Mirror.NONE;

                    // 3. 计算旋转/镜像后的相对坐标
                    double rx = ex;
                    double rz = ez;
                    double wRot = width;
                    if (rot == Rotation.CLOCKWISE_90) {
                        rx = length - ez;
                        rz = ex;
                        wRot = length;
                    } else if (rot == Rotation.CLOCKWISE_180) {
                        rx = width - ex;
                        rz = length - ez;
                        wRot = width;
                    } else if (rot == Rotation.COUNTERCLOCKWISE_90) {
                        rx = ez;
                        rz = width - ex;
                        wRot = length;
                    }

                    if (mirrored) {
                        rx = wRot - rx;
                    }

                    double finalX = basePos.getX() + offset.getX() + rx;
                    double finalY = basePos.getY() + offset.getY() + ey;
                    double finalZ = basePos.getZ() + offset.getZ() + rz;

                    // 4. 检查是否已经在目标位置有同种实体
                    net.minecraft.world.phys.AABB checkBox = new net.minecraft.world.phys.AABB(finalX - 0.5, finalY - 0.5, finalZ - 0.5, finalX + 0.5, finalY + 0.5, finalZ + 0.5);
                    net.minecraft.world.entity.EntityType<?> entityType = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(net.minecraft.resources.Identifier.tryParse(id)).orElse(null);
                    if (entityType != null) {
                        List<? extends net.minecraft.world.entity.Entity> existing = level.getEntities(entityType, checkBox, e -> true);
                        if (!existing.isEmpty()) {
                            // 已经有这个实体了，直接跳过
                            be.currentIndex++;
                            continue;
                        }
                    } else {
                        be.currentIndex++;
                        continue;
                    }

                    // 5. 计算消耗的材料
                    Item requiredItem = getRequiredItemForEntity(id, entityNbt);
                    boolean needsItem = requiredItem != Items.AIR;
                    ContainerSource itemSource = null;
                    if (needsItem) {
                        itemSource = be.findItemInSources(requiredItem);
                        if (itemSource == null) {
                            // 缺少材料：自动暂停
                            be.buildState = 2;
                            be.pauseReason = 2;
                            level.playSound(null, pos, SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.BLOCKS, 1.0f, 0.8f);
                            be.setChanged();
                            return;
                        }
                    }

                    // 6. 消耗燃料并生成
                    if (be.consumeFuel(level, pos)) {
                        if (needsItem && itemSource != null) {
                            ItemStack srcStack = itemSource.container.getItem(itemSource.slot);
                            srcStack.shrink(1);
                            itemSource.markChanged();
                        }

                        net.minecraft.world.entity.Entity entity = entityType.create(level, net.minecraft.world.entity.EntitySpawnReason.STRUCTURE);
                        if (entity != null) {
                            CompoundTag loadNbt = entityNbt.copy();
                            loadNbt.remove("UUID");
                            loadNbt.remove("Pos");
                            loadNbt.remove("Motion");
                            entity.load(TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), loadNbt));

                            entity.setPos(finalX, finalY, finalZ);
                            entity.rotate(rot);
                            entity.mirror(mir);

                            level.addFreshEntity(entity);
                            level.playSound(null, new BlockPos((int)finalX, (int)finalY, (int)finalZ), SoundEvents.ITEM_FRAME_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
                        }

                        be.currentIndex++;
                        be.setChanged();
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

                CompoundTag stateTag = paletteNbt.getCompoundOrEmpty(paletteIdx);
                BlockState blueprintState = BlockState.CODEC.parse(NbtOps.INSTANCE, stateTag).result().orElse(Blocks.AIR.defaultBlockState());
                // 应用旋转和镜像转换
                BlockState transformedState = blueprintState.rotate(rot).mirror(mir);

                BlockState currentWorldState = level.getBlockState(targetPos);

                // 检查是否完全一致
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

                // 如果不一致
                if (transformedState.isAir() || transformedState.is(Blocks.BARRIER)) {
                    // 蓝图里是空气/屏障，世界上不是：执行移除
                    if (!currentWorldState.isAir() && currentWorldState.getFluidState().isEmpty() && currentWorldState.getDestroySpeed(level, targetPos) >= 0) {
                        if (be.consumeFuel(level, pos)) {
                            if (be.destroyMode) {
                                level.setBlock(targetPos, Blocks.AIR.defaultBlockState(), 3);
                            } else {
                                level.destroyBlock(targetPos, true);
                            }
                            level.playSound(null, pos, SoundEvents.COPPER_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
                            be.setChanged();
                        }
                        return; // 本次 5 ticks 完成移除，等待下次放置
                    } else {
                        be.currentIndex++;
                        continue;
                    }
                }

                // 蓝图里是非空气，世界上不符：
                // 如果世界上不是空气，先移除它
                if (!currentWorldState.isAir() && currentWorldState.getFluidState().isEmpty() && currentWorldState.getDestroySpeed(level, targetPos) >= 0) {
                    if (be.consumeFuel(level, pos)) {
                        if (be.destroyMode) {
                            level.setBlock(targetPos, Blocks.AIR.defaultBlockState(), 3);
                        } else {
                            level.destroyBlock(targetPos, true);
                        }
                        level.playSound(null, pos, SoundEvents.COPPER_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);
                        be.setChanged();
                    }
                    return; // 等待下次放置
                }

                // 世界上是空气（或已清理），开始扣料放置
                // 检查并扣除物品
                Item requiredItem = transformedState.getBlock().asItem();
                me.tuanzi.util.ModLog.debug("准备放置: " + transformedState + " -> requiredItem: " + requiredItem + " at " + targetPos);

                // 防御性屏障检查：屏障方块无论何种情况都视为空气跳过，不要求材料
                if (transformedState.is(Blocks.BARRIER)) {
                    me.tuanzi.util.ModLog.debug("防御性屏障跳过: 蓝图位置为屏障，直接推进 index");
                    be.currentIndex++;
                    continue;
                }

                if (requiredItem == Blocks.AIR.asItem()) {
                    // 如果该方块没有对应物品，直接放置
                    level.setBlock(targetPos, transformedState, 3);
                    be.currentIndex++;
                    be.setChanged();
                    return;
                }

                ContainerSource source = be.findItemInSources(requiredItem);
                if (source != null) {
                    // 扣除燃料
                    if (be.consumeFuel(level, pos)) {
                        // 扣除物品
                        ItemStack srcStack = source.container.getItem(source.slot);
                        srcStack.shrink(1);
                        source.markChanged();

                        // 放置方块
                        level.setBlock(targetPos, transformedState, 3);

                        // 恢复方块实体 NBT
                        ListTag tileEntities = blueprintTag.getListOrEmpty("BlockEntities");
                        for (int i = 0; i < tileEntities.size(); i++) {
                            CompoundTag te = tileEntities.getCompoundOrEmpty(i);
                            int[] tpos = te.getIntArray("pos").orElse(new int[0]);
                            if (tpos.length == 3 && tpos[0] == x && tpos[1] == y && tpos[2] == z) {
                                BlockEntity newBe = level.getBlockEntity(targetPos);
                                if (newBe != null) {
                                    CompoundTag nbt = te.getCompoundOrEmpty("nbt").copy();
                                    nbt.putInt("x", targetPos.getX());
                                    nbt.putInt("y", targetPos.getY());
                                    nbt.putInt("z", targetPos.getZ());
                                    newBe.loadWithComponents(TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), nbt));
                                    newBe.setChanged();
                                    level.sendBlockUpdated(targetPos, transformedState, transformedState, 3);
                                }
                                break;
                            }
                        }

                        level.playSound(null, targetPos, transformedState.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0f, 1.0f);
                        be.currentIndex++;
                        be.setChanged();
                    }
                    return;
                } else {
                    // 缺料，自动暂停
                    be.buildState = 2;
                    be.pauseReason = 2;
                    level.playSound(null, pos, SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.BLOCKS, 1.0f, 0.8f);
                    be.setChanged();
                    return;
                }
            }

            // 如果全部跑完
            if (be.currentIndex >= expectedTotal) {
                be.totalBlocks = expectedTotal;
                be.buildState = 0;
                me.tuanzi.util.ModLog.debug("蓝图建造完成，currentIndex=" + be.currentIndex + " totalBlocks=" + be.totalBlocks);
                level.playSound(null, pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 1.0f, 1.2f);
                be.setChanged();
            }
        }
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

            if (isShulkerBox(stack)) {
                ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
                if (contents != null) {
                    NonNullList<ItemStack> list = NonNullList.withSize(27, ItemStack.EMPTY);
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

    private boolean isShulkerBox(ItemStack stack) {
        return stack.is(net.minecraft.tags.ItemTags.SHULKER_BOXES);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, this.items);
        this.buildState = input.getIntOr("BuildState", 0);
        // 注：旧存档的 PlacedBlocks 已废弃，运行时由 totalBlocks 和 currentIndex 计算进度
        this.currentIndex = input.getIntOr("CurrentIndex", 0);
        this.destroyMode = input.getBooleanOr("DestroyMode", false);
        this.exactMatch = input.getBooleanOr("ExactMatch", false);
        this.ticksToNextPlace = input.getIntOr("TicksToNext", 5);
        this.fuel = input.getIntOr("Fuel", 0);
        this.pauseReason = input.getIntOr("PauseReason", 0);
        // totalBlocks 不持久化，每次运行时由 serverTick 从蓝图重新读取
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
         *
         * cachedSources 中所有条目的 shulkerStack 均为 null（只有普通物品被添加）。
         * 因此 markChanged() 的调用者 this.shulkerStack 始终为 null。
         * - 若 parent == null：item 直接在箱子中 → 直接 setChanged() 即可。
         * - 若 parent != null：item 在潜影盒的 virtualContainer 中
         *   → 将 this.container（virtualContainer）序列化回 parent.shulkerStack 的 NBT
         *   → 再调用 parent.propagateChangedToRoot() 沿父链向上逐层传播，直至根箱子。
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
         *
         * 仅由 markChanged() 内部调用，不应从外部直接调用。
         *
         * 节点结构（以嵌套示例）：
         *   根节点: { container=chest,            shulkerStack=outerShulker, parent=null     }
         *   中间层: { container=outerVirtual,      shulkerStack=innerShulker, parent=根节点  }
         *   叶节点: { container=innerVirtual,      shulkerStack=null,         parent=中间层  }
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
