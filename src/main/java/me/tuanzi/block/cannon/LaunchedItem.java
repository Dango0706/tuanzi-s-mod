package me.tuanzi.block.cannon;

import me.tuanzi.util.ItemStackNbtHelper;
import me.tuanzi.util.ModLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 蓝图大炮飞行方块对象（参考 Create Schematicannon 架构）
 * 非实体轻量化 POJO，由大炮方块实体维护并在每 tick 扣减倒计时，到达目标后落地放置。
 */
public abstract class LaunchedItem {

    public int totalTicks;
    public int ticksRemaining;
    public BlockPos target;
    public ItemStack stack;

    public LaunchedItem() {
        this.target = BlockPos.ZERO;
        this.stack = ItemStack.EMPTY;
    }

    public LaunchedItem(BlockPos start, BlockPos target, ItemStack stack) {
        this(target, stack, ticksForDistance(start, target), ticksForDistance(start, target));
    }

    public LaunchedItem(BlockPos target, ItemStack stack, int ticksLeft, int total) {
        this.target = target;
        this.stack = stack;
        this.totalTicks = total;
        this.ticksRemaining = ticksLeft;
    }

    /**
     * 参考 Create 官方双重平方根距离公式计算飞行时长
     */
    public static int ticksForDistance(BlockPos start, BlockPos target) {
        return (int) Math.max(10, Math.sqrt(Math.sqrt(target.distSqr(start))) * 4.0);
    }

    /**
     * 每 tick 更新飞行状态
     * @return true 若已到达目标并完成落地操作，可从飞行列表中移除
     */
    public boolean update(Level world) {
        if (ticksRemaining > 0) {
            ticksRemaining--;
            return false;
        }
        if (world.isClientSide()) {
            return false;
        }

        place(world);
        return true;
    }

    public CompoundTag serializeNBT(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("TotalTicks", totalTicks);
        tag.putInt("TicksLeft", ticksRemaining);
        tag.putInt("TargetX", target.getX());
        tag.putInt("TargetY", target.getY());
        tag.putInt("TargetZ", target.getZ());
        if (!stack.isEmpty()) {
            tag.put("Stack", ItemStackNbtHelper.write(registries, stack));
        }
        return tag;
    }

    public void readNBT(CompoundTag tag, HolderLookup.Provider registries, HolderGetter<Block> holderGetter) {
        this.totalTicks = tag.getIntOr("TotalTicks", 10);
        this.ticksRemaining = tag.getIntOr("TicksLeft", 10);
        int tx = tag.getIntOr("TargetX", 0);
        int ty = tag.getIntOr("TargetY", 0);
        int tz = tag.getIntOr("TargetZ", 0);
        this.target = new BlockPos(tx, ty, tz);
        if (tag.contains("Stack")) {
            this.stack = ItemStackNbtHelper.read(registries, tag.getCompoundOrEmpty("Stack"));
        } else {
            this.stack = ItemStack.EMPTY;
        }
    }

    public static LaunchedItem fromNBT(CompoundTag tag, HolderLookup.Provider registries, HolderGetter<Block> holderGetter) {
        LaunchedItem launched;
        if (tag.contains("EntityId")) {
            launched = new ForEntity();
        } else {
            launched = new ForBlockState();
        }
        launched.readNBT(tag, registries, holderGetter);
        return launched;
    }

    public abstract void place(Level world);

    /**
     * 方块放置子类
     */
    public static class ForBlockState extends LaunchedItem {
        public BlockState state = Blocks.AIR.defaultBlockState();
        public @Nullable CompoundTag data;
        public boolean isDestroy = false;

        public ForBlockState() {
            super();
        }

        public ForBlockState(BlockPos start, BlockPos target, ItemStack stack, BlockState state, @Nullable CompoundTag data, boolean isDestroy) {
            super(start, target, stack);
            this.state = state;
            this.data = data;
            this.isDestroy = isDestroy;
        }

        @Override
        public CompoundTag serializeNBT(HolderLookup.Provider registries) {
            CompoundTag tag = super.serializeNBT(registries);
            tag.put("BlockState", NbtUtils.writeBlockState(state));
            tag.putBoolean("IsDestroy", isDestroy);
            if (data != null) {
                CompoundTag copy = data.copy();
                copy.remove("x");
                copy.remove("y");
                copy.remove("z");
                copy.remove("id");
                tag.put("Data", copy);
            }
            return tag;
        }

        @Override
        public void readNBT(CompoundTag tag, HolderLookup.Provider registries, HolderGetter<Block> holderGetter) {
            super.readNBT(tag, registries, holderGetter);
            if (tag.contains("BlockState")) {
                this.state = NbtUtils.readBlockState(holderGetter, tag.getCompoundOrEmpty("BlockState"));
            }
            this.isDestroy = tag.getBooleanOr("IsDestroy", false);
            if (tag.contains("Data")) {
                this.data = tag.getCompoundOrEmpty("Data").copy();
            } else {
                this.data = null;
            }
        }

        @Override
        public void place(Level world) {
            ModLog.debug("LaunchedItem.ForBlockState 落地触发: " + target + ", isDestroy=" + isDestroy + ", state=" + state);
            if (isDestroy) {
                BlockState current = world.getBlockState(target);
                if (!current.isAir()) {
                    world.destroyBlock(target, true);
                }
                return;
            }

            // 放置方块
            world.setBlock(target, state, 3);

            // 恢复方块实体数据
            if (data != null) {
                BlockEntity be = world.getBlockEntity(target);
                if (be != null) {
                    CompoundTag nbt = data.copy();
                    nbt.putInt("x", target.getX());
                    nbt.putInt("y", target.getY());
                    nbt.putInt("z", target.getZ());
                    be.loadWithComponents(TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, world.registryAccess(), nbt));
                    be.setChanged();
                    world.sendBlockUpdated(target, state, state, 3);
                }
            }

            world.playSound(null, target, state.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }

    /**
     * 实体生成子类
     */
    public static class ForEntity extends LaunchedItem {
        public String entityId = "";
        public @Nullable CompoundTag entityNbt;
        public Vec3 exactTarget = Vec3.ZERO;

        public ForEntity() {
            super();
        }

        public ForEntity(BlockPos start, BlockPos target, ItemStack stack, String entityId, @Nullable CompoundTag entityNbt) {
            this(start, target, Vec3.atBottomCenterOf(target), stack, entityId, entityNbt);
        }

        public ForEntity(BlockPos start, BlockPos target, Vec3 exactTarget, ItemStack stack, String entityId, @Nullable CompoundTag entityNbt) {
            super(start, target, stack);
            this.exactTarget = exactTarget != null ? exactTarget : Vec3.atBottomCenterOf(target);
            this.entityId = entityId;
            this.entityNbt = entityNbt;
        }

        @Override
        public CompoundTag serializeNBT(HolderLookup.Provider registries) {
            CompoundTag tag = super.serializeNBT(registries);
            tag.putString("EntityId", entityId);
            if (exactTarget != null) {
                tag.putDouble("ExactX", exactTarget.x);
                tag.putDouble("ExactY", exactTarget.y);
                tag.putDouble("ExactZ", exactTarget.z);
            }
            if (entityNbt != null) {
                tag.put("EntityNbt", entityNbt);
            }
            return tag;
        }

        @Override
        public void readNBT(CompoundTag tag, HolderLookup.Provider registries, HolderGetter<Block> holderGetter) {
            super.readNBT(tag, registries, holderGetter);
            this.entityId = tag.getStringOr("EntityId", "");
            if (tag.contains("ExactX")) {
                this.exactTarget = new Vec3(tag.getDoubleOr("ExactX", 0.0), tag.getDoubleOr("ExactY", 0.0), tag.getDoubleOr("ExactZ", 0.0));
            } else {
                this.exactTarget = Vec3.atBottomCenterOf(this.target);
            }
            if (tag.contains("EntityNbt")) {
                this.entityNbt = tag.getCompoundOrEmpty("EntityNbt").copy();
            } else {
                this.entityNbt = null;
            }
        }

        @Override
        public void place(Level world) {
            ModLog.debug("LaunchedItem.ForEntity 落地触发: target=" + target + ", exactTarget=" + exactTarget + ", entityId=" + entityId);
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.tryParse(entityId)).orElse(null);
            if (type == null) return;

            Entity entity = type.create(world, EntitySpawnReason.STRUCTURE);
            if (entity == null) return;

            Vec3 spawnPos = (exactTarget != null && !exactTarget.equals(Vec3.ZERO)) ? exactTarget : Vec3.atBottomCenterOf(target);

            // 1. 对于 BlockAttachedEntity（展示框、画、坐垫等），先将其预置到目标附近以通过 16 格距离校验
            if (entity instanceof net.minecraft.world.entity.decoration.BlockAttachedEntity attached) {
                attached.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            } else {
                entity.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            }

            // 2. 加载 NBT 数据
            if (entityNbt != null) {
                CompoundTag loadNbt = entityNbt.copy();
                loadNbt.remove("UUID");
                loadNbt.remove("Motion");

                // 确保 Pos 存入的是精确坐标
                net.minecraft.nbt.ListTag exactPosList = new net.minecraft.nbt.ListTag();
                exactPosList.add(net.minecraft.nbt.DoubleTag.valueOf(spawnPos.x));
                exactPosList.add(net.minecraft.nbt.DoubleTag.valueOf(spawnPos.y));
                exactPosList.add(net.minecraft.nbt.DoubleTag.valueOf(spawnPos.z));
                loadNbt.put("Pos", exactPosList);

                entity.load(TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, world.registryAccess(), loadNbt));
            }

            // 3. 针对不同类型实体进行最终定位校准
            if (entity instanceof net.minecraft.world.entity.decoration.Cushion cushion) {
                cushion.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
            } else if (entity instanceof net.minecraft.world.entity.decoration.HangingEntity hanging) {
                if (entityNbt == null || !entityNbt.contains("block_pos")) {
                    hanging.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
                }
            } else {
                entity.snapTo(spawnPos.x, spawnPos.y, spawnPos.z, entity.getYRot(), entity.getXRot());
                entity.setYBodyRot(entity.getYRot());
                entity.setYHeadRot(entity.getYRot());
            }

            world.addFreshEntity(entity);
            world.playSound(null, BlockPos.containing(spawnPos), net.minecraft.sounds.SoundEvents.ARMOR_STAND_PLACE, SoundSource.BLOCKS, 0.75f, 1.0f);
        }
    }
}
