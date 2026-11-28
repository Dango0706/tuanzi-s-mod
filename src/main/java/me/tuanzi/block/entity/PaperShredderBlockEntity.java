package me.tuanzi.block.entity;

import me.tuanzi.block.PaperShredderBlock;
import me.tuanzi.init.ModBlocks;
import me.tuanzi.init.ModItems;
import me.tuanzi.util.ModLog;
import me.tuanzi.world.canvas.CanvasFileStorage;
import me.tuanzi.world.canvas.CanvasMetadata;
import me.tuanzi.world.canvas.CanvasShredderHelper;
import me.tuanzi.world.canvas.CanvasSlotSavedData;
import me.tuanzi.world.canvas.CanvasSpaceManager;
import me.tuanzi.world.inventory.canvas.PaperShredderMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
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

import java.util.List;
import java.util.UUID;

/**
 * 碎纸机方块实体：承载 1 格画布槽位，严格校验画布本人权限并经二次确认后彻底损毁画布、
 * 归档磁盘数据为 .bak、释放创建配额并返还画布本身的合成材料。
 */
public class PaperShredderBlockEntity extends BlockEntity implements MenuProvider, Container {
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

    public PaperShredderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.PAPER_SHREDDER_BLOCK_ENTITY, pos, state);
    }

    private int computeFlagBitmask() {
        int mask = 0;
        ItemStack stack = getCanvasStack();
        if (!stack.isEmpty() && stack.is(ModItems.CANVAS)) {
            CanvasMetadata meta = CanvasMetadata.read(stack);
            if (meta.uuid() != null && CanvasSpaceManager.getSessionByCanvas(meta.uuid()) != null) {
                mask |= 1;
            }
        }
        return mask;
    }

    public ItemStack getCanvasStack() {
        return items.get(0);
    }

    public boolean handleShred(ServerPlayer player, boolean confirmed) {
        if (!confirmed) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.paper_shredder.confirm_required")
                    .withStyle(ChatFormatting.YELLOW));
            return false;
        }
        ItemStack stack = getCanvasStack();
        if (stack.isEmpty() || !stack.is(ModItems.CANVAS)) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.paper_shredder.no_canvas")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        CanvasMetadata meta = CanvasMetadata.read(stack);
        if (!CanvasShredderHelper.canPlayerShred(meta, player.getUUID())) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.paper_shredder.not_owner")
                    .withStyle(ChatFormatting.RED));
            ModLog.debug(player, "【碎纸机】拒绝非本人损毁画布: owner=" + meta.ownerUuid());
            return false;
        }

        UUID canvasUuid = meta.uuid();
        if (canvasUuid != null && CanvasSpaceManager.getSessionByCanvas(canvasUuid) != null) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.paper_shredder.in_use")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        // 清空画布槽位并同步
        items.set(0, ItemStack.EMPTY);
        setChanged();
        syncUpdate();
        player.containerMenu.broadcastChanges();

        // 归档磁盘 .painted 与 .tmp.painted 为 .bak，并返还创建名额
        if (canvasUuid != null) {
            CanvasFileStorage.archiveToBak(canvasUuid);
            if (meta.ownerUuid() != null && this.level instanceof ServerLevel sl) {
                CanvasSlotSavedData.get(sl).decrementOwnerCount(meta.ownerUuid());
            }
        }

        // 返还画布合成材料（绝不返还画布内建筑消耗的任何材料）
        List<ItemStack> refunds = CanvasShredderHelper.calculateRefundMaterials(meta);
        int paperCount = CanvasShredderHelper.calculatePaperRefundCount(meta);
        for (ItemStack refund : refunds) {
            ItemStack toGive = refund.copy();
            if (!player.getInventory().add(toGive) && !toGive.isEmpty() && this.level != null) {
                spawnRefundInFront(toGive);
            }
        }

        if (this.level != null) {
            this.level.playSound(null, worldPosition, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 0.9F, 0.85F);
            this.level.playSound(null, worldPosition, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 0.7F, 1.15F);
        }

        player.sendSystemMessage(Component.translatable("message.tuanzis_mod.paper_shredder.success", paperCount)
                .withStyle(ChatFormatting.GREEN));
        ModLog.debug(player, "【碎纸机】彻底损毁画布成功: uuid=" + canvasUuid + ", size=" + meta.size()
                + ", refundPaper=" + paperCount);
        return true;
    }

    private void spawnRefundInFront(ItemStack stack) {
        if (this.level == null || stack.isEmpty()) {
            return;
        }
        Direction facing = getBlockState().hasProperty(PaperShredderBlock.FACING)
                ? getBlockState().getValue(PaperShredderBlock.FACING)
                : Direction.NORTH;
        double sx = worldPosition.getX() + 0.5 + facing.getStepX() * 0.7;
        double sy = worldPosition.getY() + 0.6;
        double sz = worldPosition.getZ() + 0.5 + facing.getStepZ() * 0.7;
        ItemEntity drop = new ItemEntity(this.level, sx, sy, sz, stack);
        drop.setDeltaMovement(facing.getStepX() * 0.08, 0.12, facing.getStepZ() * 0.08);
        this.level.addFreshEntity(drop);
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
        return Component.translatable("container.tuanzis_mod.paper_shredder");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new PaperShredderMenu(containerId, playerInventory, this, this.data);
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
