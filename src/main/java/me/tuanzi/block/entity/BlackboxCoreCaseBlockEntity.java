package me.tuanzi.block.entity;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import me.tuanzi.Tuanzis_mod;
import me.tuanzi.init.ModBlocks;
import me.tuanzi.util.ModLog;
import me.tuanzi.world.scavenger.loot.container.IScavengerLootContainer;
import me.tuanzi.world.scavenger.loot.container.ScavengerLootContainerState;
import me.tuanzi.world.scavenger.loot.grid.GridItemEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 拾荒回廊 - 黑匣子数据核心箱 方块实体
 * <p>
 * 集成：
 * <ul>
 *   <li>GeckoLib 5.5.7 3D 模型与开箱机械滑开/排气动画；</li>
 *   <li>{@link IScavengerLootContainer} 6x6 二维战术网格搜刮容器与防透视迷雾机制；</li>
 *   <li>旧版 27 格平滑迁移与原版方块破坏掉落兼容。</li>
 * </ul>
 */
public class BlackboxCoreCaseBlockEntity extends BlockEntity implements MenuProvider, Container, GeoBlockEntity, IScavengerLootContainer {

    public static final Identifier CONTAINER_DEF_ID =
            Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "blackbox_core_case");

    private static final RawAnimation ANIM_CLOSED = RawAnimation.begin().thenLoop("animation.blackbox_core_case.closed");
    private static final RawAnimation ANIM_OPEN = RawAnimation.begin().thenPlayAndHold("animation.blackbox_core_case.open");
    private static final RawAnimation ANIM_OPENED = RawAnimation.begin().thenLoop("animation.blackbox_core_case.opened");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ScavengerLootContainerState scavengerLootState = new ScavengerLootContainerState(CONTAINER_DEF_ID, 6, 6);
    private boolean opened = false;
    private int openingParticleTicks = 0;

    public BlackboxCoreCaseBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.BLACKBOX_CORE_CASE_BLOCK_ENTITY, pos, state);
    }

    // ==================== IScavengerLootContainer 实现 ====================

    @Override
    public Identifier scavengerContainerDefId() {
        return CONTAINER_DEF_ID;
    }

    @Override
    public ScavengerLootContainerState scavengerLootState() {
        return this.scavengerLootState;
    }

    @Override
    public BlockPos containerWorldPos() {
        return this.worldPosition;
    }

    @Override
    public void markContainerChanged() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    public boolean isContainerRemoved() {
        return this.isRemoved();
    }

    // ==================== 开箱动画与状态 ====================

    public boolean isOpened() {
        return this.opened;
    }

    public void openCase() {
        if (!this.opened) {
            this.opened = true;
            this.openingParticleTicks = 30; // 持续 1.5 秒 (30 ticks) 喷涌数据流与泄压冷气
            if (this.level != null && !this.level.isClientSide()) {
                this.scavengerLootState.ensureGeneratedOnce(this.level.getRandom());
            }
            this.markContainerChanged();
            ModLog.debug("[BlackboxCoreCaseBlockEntity] Box at " + this.worldPosition + " triggered openCase. Opening animation sent.");
            if (this.level != null && !this.level.isClientSide()) {
                this.triggerAnim("main_controller", "open");
            }
        }
    }

    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, BlackboxCoreCaseBlockEntity blockEntity) {
        if (blockEntity.openingParticleTicks > 0) {
            blockEntity.openingParticleTicks--;
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                net.minecraft.util.RandomSource random = level.getRandom();
                // 1. 青色数据粒子喷泉
                for (int i = 0; i < 3; i++) {
                    double ox = (random.nextDouble() - 0.5) * 0.35;
                    double oz = (random.nextDouble() - 0.5) * 0.35;
                    double vx = (random.nextDouble() - 0.5) * 0.08;
                    double vy = 0.22 + random.nextDouble() * 0.12;
                    double vz = (random.nextDouble() - 0.5) * 0.08;
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5 + ox, pos.getY() + 0.75, pos.getZ() + 0.5 + oz, 1, vx, vy, vz, 0.08);
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.GLOW, pos.getX() + 0.5 + ox, pos.getY() + 0.75, pos.getZ() + 0.5 + oz, 1, vx * 0.5, vy * 0.8, vz * 0.5, 0.02);
                }

                // 2. 白色冷气粒子沿箱底四周水平扩散
                for (int i = 0; i < 4; i++) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    double cos = Math.cos(angle);
                    double sin = Math.sin(angle);
                    double dist = 0.45;
                    double px = pos.getX() + 0.5 + cos * dist;
                    double py = pos.getY() + 0.05;
                    double pz = pos.getZ() + 0.5 + sin * dist;
                    double speed = 0.08 + random.nextDouble() * 0.05;
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, px, py, pz, 1, cos * speed, 0.01, sin * speed, 0.01);
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE, px, py, pz, 1, cos * speed * 0.8, 0.01, sin * speed * 0.8, 0.01);
                }
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<BlackboxCoreCaseBlockEntity> controller = new AnimationController<>("main_controller", 0, state -> {
            if (this.opened) {
                return state.setAndContinue(ANIM_OPENED);
            }
            return state.setAndContinue(ANIM_CLOSED);
        });
        controller.triggerableAnim("open", ANIM_OPEN);
        controllers.add(controller);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // ==================== Container 接口兼容（支持破坏方块爆装） ====================

    @Override
    public int getContainerSize() {
        return 36; // 映射 6x6 网格槽位
    }

    @Override
    public boolean isEmpty() {
        return this.scavengerLootState.grid().isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        int x = slot % 6;
        int y = slot / 6;
        return this.scavengerLootState.grid().getEntryAt(x, y)
                .map(GridItemEntry::copyStack)
                .orElse(ItemStack.EMPTY);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        int x = slot % 6;
        int y = slot / 6;
        return this.scavengerLootState.grid().getEntryAt(x, y)
                .flatMap(e -> this.scavengerLootState.grid().removeEntry(e.entryId()))
                .map(GridItemEntry::copyStack)
                .orElse(ItemStack.EMPTY);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return removeItem(slot, 1);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!stack.isEmpty()) {
            this.scavengerLootState.grid().quickInsertFirstFit(stack, 20);
            markContainerChanged();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.scavengerLootState.grid().clearAll();
        markContainerChanged();
    }

    // ==================== 持久化存储与旧存档平滑迁移 ====================

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.opened = input.getBooleanOr("Opened", false);

        // 载入拾荒网格搜刮状态
        input.child("ScavengerLootState").ifPresent(this.scavengerLootState::loadFromValueInput);

        // 兼容旧存档 27 格物品自动迁移
        NonNullList<ItemStack> legacyItems = NonNullList.withSize(27, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, legacyItems);
        if (this.scavengerLootState.grid().isEmpty()) {
            for (ItemStack legacy : legacyItems) {
                if (!legacy.isEmpty()) {
                    this.scavengerLootState.grid().quickInsertFirstFit(legacy, 20);
                }
            }
        }
        ModLog.debug("[BlackboxCoreCaseBlockEntity] loadAdditional at " + this.worldPosition
                + ", opened: " + this.opened + ", gridItems: " + this.scavengerLootState.grid().entryCount());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("Opened", this.opened);
        this.scavengerLootState.saveToValueOutput(output.child("ScavengerLootState"));
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("Opened", this.opened);
        tag.put("ScavengerLootState", this.scavengerLootState.saveToTag(registries));
        return tag;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.tuanzis_mod.blackbox_core_case");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        net.minecraft.world.inventory.SimpleContainerData data = new net.minecraft.world.inventory.SimpleContainerData(5);
        data.set(0, this.worldPosition.getX());
        data.set(1, this.worldPosition.getY());
        data.set(2, this.worldPosition.getZ());
        data.set(3, this.scavengerLootState.columns());
        data.set(4, this.scavengerLootState.rows());
        return new me.tuanzi.world.inventory.scavenger.ScavengerTacticalMenu(containerId, playerInventory, this, data);
    }
}
