package me.tuanzi.block.entity;

import me.tuanzi.init.ModBlocks;
import me.tuanzi.init.ModItems;
import me.tuanzi.network.canvas.Printer3DActionC2SPacket;
import me.tuanzi.util.ModLog;
import me.tuanzi.world.canvas.CanvasDataCodec;
import me.tuanzi.world.canvas.CanvasFileStorage;
import me.tuanzi.world.canvas.CanvasMetadata;
import me.tuanzi.world.canvas.CanvasPrintCalculator;
import me.tuanzi.world.canvas.CanvasType;
import me.tuanzi.world.canvas.CanvasVoxelData;
import me.tuanzi.world.inventory.canvas.Printer3DMenu;
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

import java.util.UUID;

/**
 * 3D 打印机方块实体：校验画布尺寸、体素上限与打印原料消耗，生成静态打印模型方块。
 */
public class Printer3DBlockEntity extends BlockEntity implements MenuProvider, Container {
    public static final int SLOT_CANVAS = 0;
    public static final int SLOT_MATERIAL = 1;
    public static final int SLOT_OUTPUT = 2;

    public static final int STATUS_READY = 0;
    public static final int STATUS_NO_CANVAS = 1;
    public static final int STATUS_UNSET_TYPE = 2;
    public static final int STATUS_EMPTY_CANVAS = 3;
    public static final int STATUS_EXCEEDS_LIMIT = 4;
    public static final int STATUS_NO_MATERIAL = 5;
    public static final int STATUS_OUTPUT_FULL = 6;
    public static final int STATUS_CORRUPTED = 7;

    private final NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    private int selectedScale = 1;

    private int cachedStatus = STATUS_NO_CANVAS;
    private int cachedCost = 0;
    private int cachedLimit = 65536;
    private int cachedNonEmpty = 0;
    private int cachedCanvasSize = 16;
    private int cachedCanvasTypeOrd = 0;

    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            recomputePreviewStats();
            return switch (index) {
                case 0 -> worldPosition.getX() & 0xFFFF;
                case 1 -> worldPosition.getX() >>> 16;
                case 2 -> worldPosition.getY();
                case 3 -> worldPosition.getZ() & 0xFFFF;
                case 4 -> worldPosition.getZ() >>> 16;
                case 5 -> selectedScale;
                case 6 -> cachedStatus;
                case 7 -> cachedCost & 0xFFFF;
                case 8 -> cachedCost >>> 16;
                case 9 -> cachedLimit & 0xFFFF;
                case 10 -> cachedLimit >>> 16;
                case 11 -> cachedNonEmpty & 0xFFFF;
                case 12 -> cachedNonEmpty >>> 16;
                case 13 -> cachedCanvasSize;
                case 14 -> cachedCanvasTypeOrd;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 5) {
                selectedScale = Math.max(1, value);
            }
        }

        @Override
        public int getCount() {
            return 15;
        }
    };

    public Printer3DBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.PRINTER_3D_BLOCK_ENTITY, pos, state);
    }

    private void recomputePreviewStats() {
        if (this.level != null && this.level.isClientSide()) {
            return;
        }
        ItemStack canvas = items.get(SLOT_CANVAS);
        if (canvas.isEmpty() || !canvas.is(ModItems.CANVAS)) {
            cachedStatus = STATUS_NO_CANVAS;
            cachedCost = 0;
            cachedNonEmpty = 0;
            cachedCanvasSize = 16;
            cachedCanvasTypeOrd = CanvasType.UNSET.ordinal();
            cachedLimit = CanvasPrintCalculator.getVoxelLimitForType(CanvasType.FLAT_2D);
            return;
        }

        CanvasMetadata meta = CanvasFileStorage.ensureMaterialized(canvas);
        cachedCanvasSize = meta.size();
        cachedCanvasTypeOrd = meta.type().ordinal();
        cachedLimit = CanvasPrintCalculator.getVoxelLimitForType(meta.type());
        selectedScale = CanvasPrintCalculator.clampScale(selectedScale, meta.size(), meta.type());

        if (meta.corrupted()) {
            cachedStatus = STATUS_CORRUPTED;
            cachedCost = 0;
            cachedNonEmpty = meta.nonEmptyVoxels();
            return;
        }
        if (meta.type() == CanvasType.UNSET) {
            cachedStatus = STATUS_UNSET_TYPE;
            cachedCost = 0;
            cachedNonEmpty = 0;
            return;
        }
        if (meta.uuid() == null || !CanvasFileStorage.hasSavedFile(meta.uuid())) {
            cachedStatus = STATUS_EMPTY_CANVAS;
            cachedCost = 0;
            cachedNonEmpty = 0;
            return;
        }

        CanvasDataCodec.DecodeResult res = CanvasFileStorage.loadSaved(meta.uuid());
        if (res.corrupted() || res.data() == null) {
            cachedStatus = STATUS_CORRUPTED;
            cachedCost = 0;
            cachedNonEmpty = meta.nonEmptyVoxels();
            return;
        }

        CanvasVoxelData data = res.data();
        CanvasPrintCalculator.PrintCostReport report = CanvasPrintCalculator.calculate(data, selectedScale);
        cachedNonEmpty = report.nonEmptyVoxels();
        cachedCost = report.materialCost();
        cachedLimit = report.voxelLimit();

        if (report.nonEmptyVoxels() <= 0) {
            cachedStatus = STATUS_EMPTY_CANVAS;
        } else if (report.exceedsLimit()) {
            cachedStatus = STATUS_EXCEEDS_LIMIT;
        } else if (!canOutputAccept(1)) {
            cachedStatus = STATUS_OUTPUT_FULL;
        } else if (getMaterialCount() < report.materialCost()) {
            cachedStatus = STATUS_NO_MATERIAL;
        } else {
            cachedStatus = STATUS_READY;
        }
    }

    private int getMaterialCount() {
        ItemStack mat = items.get(SLOT_MATERIAL);
        return (!mat.isEmpty() && mat.is(ModItems.PRINT_MATERIAL)) ? mat.getCount() : 0;
    }

    private boolean canOutputAccept(int count) {
        ItemStack out = items.get(SLOT_OUTPUT);
        return out.isEmpty();
    }

    public void handleAction(ServerPlayer player, int action, int scale) {
        if (action == Printer3DActionC2SPacket.ACTION_SELECT_SCALE) {
            ItemStack canvas = items.get(SLOT_CANVAS);
            if (!canvas.isEmpty()) {
                CanvasMetadata meta = CanvasMetadata.read(canvas);
                this.selectedScale = CanvasPrintCalculator.clampScale(scale, meta.size(), meta.type());
            } else {
                this.selectedScale = Math.max(1, Math.min(16, scale));
            }
            recomputePreviewStats();
            setChanged();
            player.containerMenu.broadcastChanges();
            return;
        }

        if (action == Printer3DActionC2SPacket.ACTION_PRINT) {
            recomputePreviewStats();
            if (cachedStatus != STATUS_READY) {
                String msgKey = switch (cachedStatus) {
                    case STATUS_NO_CANVAS -> "message.tuanzis_mod.printer.err_no_canvas";
                    case STATUS_UNSET_TYPE -> "message.tuanzis_mod.printer.err_unset_type";
                    case STATUS_EMPTY_CANVAS -> "message.tuanzis_mod.printer.err_empty_canvas";
                    case STATUS_EXCEEDS_LIMIT -> "message.tuanzis_mod.printer.err_exceeds_limit";
                    case STATUS_NO_MATERIAL -> "message.tuanzis_mod.printer.err_no_material";
                    case STATUS_OUTPUT_FULL -> "message.tuanzis_mod.printer.err_output_full";
                    case STATUS_CORRUPTED -> "message.tuanzis_mod.printer.err_corrupted";
                    default -> "message.tuanzis_mod.printer.err_no_canvas";
                };
                player.sendSystemMessage(Component.translatable(msgKey).withStyle(ChatFormatting.RED));
                return;
            }

            ItemStack canvas = items.get(SLOT_CANVAS);
            CanvasMetadata meta = CanvasFileStorage.ensureMaterialized(canvas);
            if (meta.uuid() == null) {
                return;
            }
            int cost = cachedCost;
            ItemStack material = items.get(SLOT_MATERIAL);
            if (material.isEmpty() || !material.is(ModItems.PRINT_MATERIAL) || material.getCount() < cost) {
                player.sendSystemMessage(Component.translatable("message.tuanzis_mod.printer.err_no_material").withStyle(ChatFormatting.RED));
                return;
            }
            if (!items.get(SLOT_OUTPUT).isEmpty()) {
                player.sendSystemMessage(Component.translatable("message.tuanzis_mod.printer.err_output_full").withStyle(ChatFormatting.RED));
                return;
            }

            UUID modelUuid = UUID.randomUUID();
            if (!CanvasFileStorage.createImmutablePrintSnapshot(meta.uuid(), modelUuid)) {
                player.sendSystemMessage(Component.translatable("message.tuanzis_mod.printer.err_corrupted").withStyle(ChatFormatting.RED));
                return;
            }

            material.shrink(cost);
            if (material.isEmpty()) {
                items.set(SLOT_MATERIAL, ItemStack.EMPTY);
            }

            ItemStack outputModel = new ItemStack(ModBlocks.PRINTED_MODEL);
            CanvasMetadata modelMeta = meta.withPrintSnapshot(modelUuid, selectedScale);
            CanvasMetadata.write(outputModel, modelMeta);
            items.set(SLOT_OUTPUT, outputModel);

            recomputePreviewStats();
            setChanged();
            player.containerMenu.broadcastChanges();
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.printer.print_success", cost)
                    .withStyle(ChatFormatting.GREEN));
            ModLog.debug(player, "【3D打印机】打印成功: sourceCanvas=" + meta.uuid() + ", modelUuid=" + modelUuid
                    + ", scale=" + selectedScale + ", cost=" + cost);
        }
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
        if (slot == SLOT_CANVAS && !stack.isEmpty() && stack.is(ModItems.CANVAS)) {
            CanvasFileStorage.ensureMaterialized(stack);
        }
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_CANVAS -> stack.is(ModItems.CANVAS);
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
        return Component.translatable("container.tuanzis_mod.printer_3d");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new Printer3DMenu(containerId, playerInventory, this, this.data);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items.clear();
        ContainerHelper.loadAllItems(input, items);
        selectedScale = Math.max(1, input.getIntOr("SelectedScale", 1));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("SelectedScale", selectedScale);
    }
}
