package me.tuanzi.block.entity;

import me.tuanzi.init.ModBlocks;
import me.tuanzi.init.ModItems;
import me.tuanzi.network.canvas.EaselActionC2SPacket;
import me.tuanzi.util.ModLog;
import me.tuanzi.world.canvas.CanvasDataCodec;
import me.tuanzi.world.canvas.CanvasFileStorage;
import me.tuanzi.world.canvas.CanvasMetadata;
import me.tuanzi.world.canvas.CanvasPermission;
import me.tuanzi.world.canvas.CanvasPermissionHelper;
import me.tuanzi.world.canvas.CanvasSpaceManager;
import me.tuanzi.world.canvas.CanvasType;
import me.tuanzi.world.canvas.CanvasVoxelData;
import me.tuanzi.world.inventory.canvas.EaselMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

/**
 * 画板方块实体：承载 1 格画布槽位，处理画布定型、进入编辑空间、提交保存与权限切换。
 */
public class EaselBlockEntity extends BlockEntity implements MenuProvider, Container {
    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);

    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> worldPosition.getX() & 0xFFFF;
                case 1 -> worldPosition.getX() >>> 16;
                case 2 -> worldPosition.getY();
                case 3 -> worldPosition.getZ() & 0xFFFF;
                case 4 -> worldPosition.getZ() >>> 16;
                case 5 -> computeFlagBitmask();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    public EaselBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.EASEL_BLOCK_ENTITY, pos, state);
    }

    private int computeFlagBitmask() {
        int mask = 0;
        ItemStack stack = getCanvasStack();
        if (!stack.isEmpty()) {
            CanvasMetadata meta = CanvasMetadata.read(stack);
            if (meta.uuid() != null && CanvasFileStorage.hasActiveDraft(meta.uuid())) {
                mask |= 1;
            }
            if (level instanceof ServerLevel sl && CanvasSpaceManager.isEaselOccupied(sl, worldPosition)) {
                mask |= 2;
            }
        }
        return mask;
    }

    public ItemStack getCanvasStack() {
        return items.get(0);
    }

    public @Nullable UUID getCanvasUuid() {
        ItemStack stack = getCanvasStack();
        if (stack.isEmpty()) {
            return null;
        }
        return CanvasMetadata.read(stack).uuid();
    }

    public void handleAction(ServerPlayer player, int action) {
        ItemStack stack = getCanvasStack();
        if (stack.isEmpty() || !stack.is(ModItems.CANVAS)) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.canvas.no_canvas").withStyle(ChatFormatting.RED));
            return;
        }
        CanvasMetadata meta = CanvasFileStorage.ensureMaterialized(stack);
        if (this.level instanceof ServerLevel sl && CanvasSpaceManager.isEaselOccupied(sl, worldPosition)
                && action != EaselActionC2SPacket.ACTION_ENTER_EDIT
                && action != EaselActionC2SPacket.ACTION_SAVE_CANVAS) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.easel.occupied_action_denied").withStyle(ChatFormatting.RED));
            return;
        }

        switch (action) {
            case EaselActionC2SPacket.ACTION_SET_2D -> setCanvasType(player, stack, meta, CanvasType.FLAT_2D);
            case EaselActionC2SPacket.ACTION_SET_3D -> setCanvasType(player, stack, meta, CanvasType.VOXEL_3D);
            case EaselActionC2SPacket.ACTION_ENTER_EDIT -> {
                if (this.level instanceof ServerLevel sl) {
                    CanvasSpaceManager.enterCanvasSpace(player, sl, worldPosition, this);
                }
            }
            case EaselActionC2SPacket.ACTION_SAVE_CANVAS -> {
                if (this.level != null) {
                    CanvasSpaceManager.commitSaveFromEasel(player, this);
                    player.containerMenu.broadcastChanges();
                }
            }
            case EaselActionC2SPacket.ACTION_CYCLE_PERMISSION -> cyclePermission(player, stack, meta);
            case EaselActionC2SPacket.ACTION_RESET_CANVAS -> resetCorruptedCanvas(player, stack, meta);
            case EaselActionC2SPacket.ACTION_EXPORT_CORRUPTED -> exportCorruptedCanvas(player, meta);
            default -> {
            }
        }
    }

    private void setCanvasType(ServerPlayer player, ItemStack stack, CanvasMetadata meta, CanvasType targetType) {
        if (meta.type() != CanvasType.UNSET) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.canvas.type_locked").withStyle(ChatFormatting.RED));
            return;
        }
        if (targetType == CanvasType.VOXEL_3D && meta.size() > CanvasType.MAX_SIZE_3D) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.canvas.3d_size_limit").withStyle(ChatFormatting.RED));
            return;
        }
        UUID ownerUuid = meta.ownerUuid() != null ? meta.ownerUuid() : player.getUUID();
        String ownerName = !meta.ownerName().isEmpty() ? meta.ownerName() : player.getName().getString();
        CanvasMetadata updated = meta.withTypeAndOwner(targetType, ownerUuid, ownerName);
        if (!updated.hasAssignedUuid()) {
            updated = updated.withUuid(UUID.randomUUID());
        }
        CanvasMetadata.write(stack, updated);
        try {
            CanvasVoxelData empty = CanvasVoxelData.createEmpty(updated.size(), targetType);
            CanvasFileStorage.writeSaved(updated.uuid(), empty);
            updated = updated.withStats(0, System.currentTimeMillis(), empty.computeSha256Hex(), false);
            CanvasMetadata.write(stack, updated);
        } catch (IOException e) {
            ModLog.error("[EaselBlockEntity] 初始化画布文件失败: " + updated.uuid(), e);
        }
        this.setChanged();
        player.containerMenu.broadcastChanges();
        ModLog.debug(player, "【画板】设置画布类型: uuid=" + updated.uuid() + ", type=" + targetType + ", size=" + updated.size());
    }

    private void cyclePermission(ServerPlayer player, ItemStack stack, CanvasMetadata meta) {
        boolean canManage = player.canUseGameMasterBlocks()
                || meta.ownerUuid() == null
                || CanvasPermissionHelper.isOwner(meta.ownerUuid(), player.getUUID());
        if (!canManage) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.canvas.only_owner_perm").withStyle(ChatFormatting.RED));
            return;
        }
        CanvasPermission next = meta.permission().next();
        UUID ownerUuid = meta.ownerUuid() != null ? meta.ownerUuid() : player.getUUID();
        String ownerName = !meta.ownerName().isEmpty() ? meta.ownerName() : player.getName().getString();
        CanvasMetadata updated = meta.withTypeAndOwner(meta.type(), ownerUuid, ownerName).withPermission(next);
        CanvasMetadata.write(stack, updated);
        this.setChanged();
        player.containerMenu.broadcastChanges();
        ModLog.debug(player, "【画板】切换访问权限: uuid=" + updated.uuid() + ", perm=" + next);
    }

    private void resetCorruptedCanvas(ServerPlayer player, ItemStack stack, CanvasMetadata meta) {
        boolean canManage = player.canUseGameMasterBlocks()
                || meta.ownerUuid() == null
                || CanvasPermissionHelper.isOwner(meta.ownerUuid(), player.getUUID());
        if (!canManage) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.canvas.only_owner_perm").withStyle(ChatFormatting.RED));
            return;
        }
        UUID uuid = meta.uuid() != null ? meta.uuid() : UUID.randomUUID();
        CanvasType type = meta.type() == CanvasType.UNSET ? CanvasType.FLAT_2D : meta.type();
        int size = CanvasType.normalizeSize(meta.size(), type);
        CanvasVoxelData fresh = CanvasVoxelData.createEmpty(size, type);
        try {
            CanvasFileStorage.writeSaved(uuid, fresh);
            CanvasFileStorage.clearDraftMarker(uuid);
            CanvasMetadata updated = new CanvasMetadata(
                    uuid,
                    CanvasDataCodec.CURRENT_VERSION,
                    size,
                    type,
                    meta.ownerUuid() != null ? meta.ownerUuid() : player.getUUID(),
                    !meta.ownerName().isEmpty() ? meta.ownerName() : player.getName().getString(),
                    meta.permission(),
                    0,
                    System.currentTimeMillis(),
                    fresh.computeSha256Hex(),
                    false,
                    1,
                    null,
                    0
            );
            CanvasMetadata.write(stack, updated);
            this.setChanged();
            player.containerMenu.broadcastChanges();
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.canvas.reset_success").withStyle(ChatFormatting.GREEN));
        } catch (IOException e) {
            ModLog.error("[EaselBlockEntity] 重置画布失败: " + uuid, e);
        }
    }

    private void exportCorruptedCanvas(ServerPlayer player, CanvasMetadata meta) {
        if (meta.uuid() == null) {
            return;
        }
        Path out = CanvasFileStorage.exportCorruptedCopy(meta.uuid());
        if (out != null) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.canvas.export_success",
                    out.getFileName().toString()).withStyle(ChatFormatting.GREEN));
        } else {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.canvas.export_fail").withStyle(ChatFormatting.RED));
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level instanceof ServerLevel sl) {
            CanvasSpaceManager.onEaselRemoved(sl, pos, getCanvasUuid());
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return items.get(0).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (this.level instanceof ServerLevel sl && CanvasSpaceManager.isEaselOccupied(sl, worldPosition)) {
            return ItemStack.EMPTY;
        }
        ItemStack res = ContainerHelper.removeItem(items, slot, amount);
        if (!res.isEmpty()) {
            setChanged();
            syncUpdate();
        }
        return res;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!stack.isEmpty() && stack.is(ModItems.CANVAS)) {
            CanvasFileStorage.ensureMaterialized(stack);
        }
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
        syncUpdate();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.is(ModItems.CANVAS);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.tuanzis_mod.easel");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new EaselMenu(containerId, playerInventory, this, this.data);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items.clear();
        ContainerHelper.loadAllItems(input, items);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
    }

    private void syncUpdate() {
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }
}
