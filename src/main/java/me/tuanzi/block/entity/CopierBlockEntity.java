package me.tuanzi.block.entity;

import me.tuanzi.config.ModConfig;
import me.tuanzi.init.ModBlocks;
import me.tuanzi.init.ModItems;
import me.tuanzi.network.canvas.CopierActionC2SPacket;
import me.tuanzi.util.ModLog;
import me.tuanzi.world.canvas.CanvasDataCodec;
import me.tuanzi.world.canvas.CanvasFileStorage;
import me.tuanzi.world.canvas.CanvasMetadata;
import me.tuanzi.world.canvas.CanvasPermissionHelper;
import me.tuanzi.world.canvas.CanvasPrintCalculator;
import me.tuanzi.world.inventory.canvas.CopierMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
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

/**
 * 复印机方块实体：输入打印模型母版与打印原料，校验权限后输出保留数据的模型复制品。
 */
public class CopierBlockEntity extends BlockEntity implements MenuProvider, Container {
    public static final int SLOT_MODEL = 0;
    public static final int SLOT_MATERIAL = 1;
    public static final int SLOT_OUTPUT = 2;

    public static final int STATUS_READY = 0;
    public static final int STATUS_NO_MODEL = 1;
    public static final int STATUS_NO_MATERIAL = 2;
    public static final int STATUS_OUTPUT_FULL = 3;
    public static final int STATUS_DISABLED = 4;
    public static final int STATUS_CORRUPTED = 5;

    private final NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    private int cachedStatus = STATUS_NO_MODEL;
    private int cachedCost = 0;

    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            recomputeStats();
            return switch (index) {
                case 0 -> worldPosition.getX() & 0xFFFF;
                case 1 -> worldPosition.getX() >>> 16;
                case 2 -> worldPosition.getY();
                case 3 -> worldPosition.getZ() & 0xFFFF;
                case 4 -> worldPosition.getZ() >>> 16;
                case 5 -> cachedStatus;
                case 6 -> cachedCost & 0xFFFF;
                case 7 -> cachedCost >>> 16;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 8;
        }
    };

    public CopierBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.COPIER_BLOCK_ENTITY, pos, state);
    }

    private void recomputeStats() {
        if (this.level != null && this.level.isClientSide()) {
            return;
        }
        if (!ModConfig.isAllowCanvasCopying()) {
            cachedStatus = STATUS_DISABLED;
            cachedCost = 0;
            return;
        }
        ItemStack model = items.get(SLOT_MODEL);
        if (model.isEmpty() || !model.is(ModBlocks.PRINTED_MODEL.asItem())) {
            cachedStatus = STATUS_NO_MODEL;
            cachedCost = 0;
            return;
        }
        CanvasMetadata meta = CanvasMetadata.read(model);
        if (meta.corrupted() || meta.uuid() == null || !CanvasFileStorage.hasSavedFile(meta.uuid())) {
            cachedStatus = STATUS_CORRUPTED;
            cachedCost = 0;
            return;
        }
        CanvasDataCodec.DecodeResult res = CanvasFileStorage.loadSaved(meta.uuid());
        if (res.corrupted() || res.data() == null) {
            cachedStatus = STATUS_CORRUPTED;
            cachedCost = 0;
            return;
        }
        cachedCost = CanvasPrintCalculator.calculate(res.data(), meta.printScale()).materialCost();
        if (cachedCost <= 0) {
            cachedCost = CanvasPrintCalculator.calculateMetadataFallbackCost(meta);
        }

        ItemStack out = items.get(SLOT_OUTPUT);
        ItemStack singleCopy = model.copyWithCount(1);
        boolean canFit = out.isEmpty()
                || (ItemStack.isSameItemSameComponents(out, singleCopy) && out.getCount() < out.getMaxStackSize());
        if (!canFit) {
            cachedStatus = STATUS_OUTPUT_FULL;
            return;
        }
        ItemStack mat = items.get(SLOT_MATERIAL);
        if (mat.isEmpty() || !mat.is(ModItems.PRINT_MATERIAL) || mat.getCount() < cachedCost) {
            cachedStatus = STATUS_NO_MATERIAL;
            return;
        }
        cachedStatus = STATUS_READY;
    }

    public void handleAction(ServerPlayer player, int action) {
        if (action != CopierActionC2SPacket.ACTION_COPY) {
            return;
        }
        recomputeStats();
        if (!ModConfig.isAllowCanvasCopying()) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.copier.disabled").withStyle(ChatFormatting.RED));
            return;
        }
        ItemStack model = items.get(SLOT_MODEL);
        if (model.isEmpty() || !model.is(ModBlocks.PRINTED_MODEL.asItem())) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.copier.err_no_model").withStyle(ChatFormatting.RED));
            return;
        }
        CanvasMetadata meta = CanvasMetadata.read(model);
        ServerPlayer onlineOwner = (meta.ownerUuid() != null && this.level != null && this.level.getServer() != null)
                ? this.level.getServer().getPlayerList().getPlayer(meta.ownerUuid())
                : null;
        if (!CanvasPermissionHelper.canCopyModel(meta, player, onlineOwner)) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.copier.err_no_permission").withStyle(ChatFormatting.RED));
            return;
        }
        if (cachedStatus == STATUS_CORRUPTED) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.copier.err_corrupted").withStyle(ChatFormatting.RED));
            return;
        }
        if (cachedStatus == STATUS_OUTPUT_FULL) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.copier.err_output_full").withStyle(ChatFormatting.RED));
            return;
        }
        if (cachedStatus == STATUS_NO_MATERIAL) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.copier.err_no_material").withStyle(ChatFormatting.RED));
            return;
        }

        ItemStack mat = items.get(SLOT_MATERIAL);
        if (mat.isEmpty() || !mat.is(ModItems.PRINT_MATERIAL) || mat.getCount() < cachedCost) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.copier.err_no_material").withStyle(ChatFormatting.RED));
            return;
        }

        mat.shrink(cachedCost);
        if (mat.isEmpty()) {
            items.set(SLOT_MATERIAL, ItemStack.EMPTY);
        }

        ItemStack copy = model.copyWithCount(1);
        ItemStack out = items.get(SLOT_OUTPUT);
        if (out.isEmpty()) {
            items.set(SLOT_OUTPUT, copy);
        } else if (ItemStack.isSameItemSameComponents(out, copy) && out.getCount() < out.getMaxStackSize()) {
            out.grow(1);
        }

        recomputeStats();
        setChanged();
        player.containerMenu.broadcastChanges();
        player.sendSystemMessage(Component.translatable("message.tuanzis_mod.copier.copy_success", cachedCost)
                .withStyle(ChatFormatting.GREEN));
        ModLog.debug(player, "【复印机】复制打印模型成功: uuid=" + meta.uuid() + ", cost=" + cachedCost);
    }

    @Override
    public int getContainerSize() {
        return 3;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : items) {
            if (!s.isEmpty()) return false;
        }
        return true;
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
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_MODEL -> stack.is(ModBlocks.PRINTED_MODEL.asItem());
            case SLOT_MATERIAL -> stack.is(ModItems.PRINT_MATERIAL);
            default -> false;
        };
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
        return Component.translatable("container.tuanzis_mod.copier");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CopierMenu(containerId, playerInventory, this, this.data);
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
}
