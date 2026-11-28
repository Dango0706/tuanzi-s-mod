package me.tuanzi.block.entity;

import me.tuanzi.block.PrintedModelBlock;
import me.tuanzi.block.PrintedModelMountMode;
import me.tuanzi.init.ModBlocks;
import me.tuanzi.world.canvas.CanvasClientCollisionBridge;
import me.tuanzi.world.canvas.CanvasDataCodec;
import me.tuanzi.world.canvas.CanvasFileStorage;
import me.tuanzi.world.canvas.CanvasMetadata;
import me.tuanzi.world.canvas.CanvasType;
import me.tuanzi.world.canvas.CanvasVoxelData;
import me.tuanzi.world.canvas.VoxelCollisionMerger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 打印模型主方块实体：保存模型元数据、同步至客户端并按体素合并生成精确紧凑的碰撞箱缓存。
 */
public class PrintedModelBlockEntity extends BlockEntity {
    private CanvasMetadata metadata = CanvasMetadata.createDefaultBlank();
    private final Map<Long, VoxelShape> cachedCellShapes = new HashMap<>();
    private @Nullable String cachedShapeHash = null;
    private volatile boolean breakingMasterChain = false;

    public PrintedModelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.PRINTED_MODEL_BLOCK_ENTITY, pos, state);
    }

    public CanvasMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(CanvasMetadata metadata) {
        this.metadata = metadata != null ? metadata : CanvasMetadata.createDefaultBlank();
        this.cachedCellShapes.clear();
        this.cachedShapeHash = null;
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public boolean isBreakingMasterChain() {
        return breakingMasterChain;
    }

    public void setBreakingMasterChain(boolean breakingMasterChain) {
        this.breakingMasterChain = breakingMasterChain;
    }

    public int[] getWorldDimensions(BlockState state) {
        int scale = Math.max(1, metadata.printScale());
        boolean is2d = metadata.type() != CanvasType.VOXEL_3D;
        PrintedModelMountMode mode = state.hasProperty(PrintedModelBlock.MOUNT_MODE)
                ? state.getValue(PrintedModelBlock.MOUNT_MODE)
                : PrintedModelMountMode.FLOOR;
        Direction facing = state.hasProperty(PrintedModelBlock.FACING)
                ? state.getValue(PrintedModelBlock.FACING)
                : Direction.NORTH;
        if (!is2d || mode != PrintedModelMountMode.WALL) {
            return new int[]{scale, is2d ? 1 : scale, scale};
        }
        if (facing == Direction.NORTH || facing == Direction.SOUTH) {
            return new int[]{scale, scale, 1};
        }
        return new int[]{1, scale, scale};
    }

    public VoxelShape getCellShape(BlockState state, int worldCx, int worldCy, int worldCz) {
        UUID uuid = metadata.uuid();
        if (uuid == null) {
            return Shapes.block();
        }
        String keyHash = metadata.dataHash() + ":" + state.getValue(PrintedModelBlock.FACING).getSerializedName()
                + ":" + state.getValue(PrintedModelBlock.MOUNT_MODE).getSerializedName()
                + ":" + metadata.printScale();
        if (!keyHash.equals(cachedShapeHash)) {
            cachedCellShapes.clear();
            cachedShapeHash = keyHash;
        }
        long cellKey = (((long) worldCx & 0xFFL) << 16) | (((long) worldCy & 0xFFL) << 8) | ((long) worldCz & 0xFFL);
        VoxelShape cached = cachedCellShapes.get(cellKey);
        if (cached != null) {
            return cached;
        }

        CanvasVoxelData voxelData = resolveVoxelData(uuid, metadata.dataHash());
        if (voxelData == null) {
            // 客户端尚未拉取完成时先提供占位碰撞箱，不写入持久缓存以便下次重算
            return Shapes.block();
        }

        int scale = Math.max(1, metadata.printScale());
        boolean is2d = voxelData.getType() != CanvasType.VOXEL_3D;
        int canonW = scale;
        int canonH = is2d ? 1 : scale;
        int canonL = scale;

        Direction facing = state.getValue(PrintedModelBlock.FACING);
        PrintedModelMountMode mode = state.getValue(PrintedModelBlock.MOUNT_MODE);

        int[] canonCell = mapWorldCellToCanonical(worldCx, worldCy, worldCz, scale, is2d, facing, mode);
        List<VoxelCollisionMerger.Box16> rawBoxes = VoxelCollisionMerger.computeCellBoxes(
                voxelData, canonW, canonH, canonL,
                canonCell[0], canonCell[1], canonCell[2]
        );
        List<VoxelCollisionMerger.Box16> transformed = new ArrayList<>(rawBoxes.size());
        for (VoxelCollisionMerger.Box16 box : rawBoxes) {
            transformed.add(transformBox16(box, is2d, facing, mode));
        }
        VoxelShape shape = VoxelCollisionMerger.toVoxelShape(transformed);
        if (shape.isEmpty() && worldCx == 0 && worldCy == 0 && worldCz == 0) {
            // 主方块保底保留极薄底部拾取轮廓，避免完全空体素导致无法右键/挖掘
            shape = Shapes.box(0.0, 0.0, 0.0, 1.0, 0.0625, 1.0);
        }
        cachedCellShapes.put(cellKey, shape);
        return shape;
    }

    private @Nullable CanvasVoxelData resolveVoxelData(UUID uuid, String hash) {
        if (this.level != null && this.level.isClientSide()) {
            CanvasVoxelData clientData = CanvasClientCollisionBridge.getClientVoxelData(uuid, hash);
            if (clientData == null) {
                CanvasClientCollisionBridge.requestClientDataIfMissing(uuid, hash);
            }
            return clientData;
        }
        if (CanvasFileStorage.hasSavedFile(uuid)) {
            CanvasDataCodec.DecodeResult res = CanvasFileStorage.loadSaved(uuid);
            if (res.isOk()) {
                return res.data();
            }
        }
        return null;
    }

    public static int[] mapWorldCellToCanonical(
            int wx, int wy, int wz,
            int scale, boolean is2d,
            Direction facing, PrintedModelMountMode mode
    ) {
        if (is2d && mode == PrintedModelMountMode.WALL) {
            int cz = Math.max(0, Math.min(scale - 1, scale - 1 - wy));
            int cx = switch (facing) {
                case SOUTH -> wx;
                case NORTH -> scale - 1 - wx;
                case EAST -> scale - 1 - wz;
                case WEST -> wz;
                default -> wx;
            };
            return new int[]{Math.max(0, Math.min(scale - 1, cx)), 0, cz};
        }
        int cx;
        int cz;
        switch (facing) {
            case EAST -> {
                cx = wz;
                cz = scale - 1 - wx;
            }
            case SOUTH -> {
                cx = scale - 1 - wx;
                cz = scale - 1 - wz;
            }
            case WEST -> {
                cx = scale - 1 - wz;
                cz = wx;
            }
            default -> {
                cx = wx;
                cz = wz;
            }
        }
        return new int[]{
                Math.max(0, Math.min(scale - 1, cx)),
                Math.max(0, Math.min((is2d ? 1 : scale) - 1, wy)),
                Math.max(0, Math.min(scale - 1, cz))
        };
    }

    public static VoxelCollisionMerger.Box16 transformBox16(
            VoxelCollisionMerger.Box16 b,
            boolean is2d,
            Direction facing,
            PrintedModelMountMode mode
    ) {
        if (is2d && mode == PrintedModelMountMode.WALL) {
            int wy0 = 16 - b.maxZ();
            int wy1 = 16 - b.minZ();
            return switch (facing) {
                case SOUTH -> new VoxelCollisionMerger.Box16(b.minX(), wy0, b.minY(), b.maxX(), wy1, b.maxY());
                case NORTH -> new VoxelCollisionMerger.Box16(16 - b.maxX(), wy0, 16 - b.maxY(), 16 - b.minX(), wy1, 16 - b.minY());
                case EAST -> new VoxelCollisionMerger.Box16(b.minY(), wy0, 16 - b.maxX(), b.maxY(), wy1, 16 - b.minX());
                case WEST -> new VoxelCollisionMerger.Box16(16 - b.maxY(), wy0, b.minX(), 16 - b.minY(), wy1, b.maxX());
                default -> b;
            };
        }

        int y0 = (is2d && mode == PrintedModelMountMode.CEILING) ? (16 - b.maxY()) : b.minY();
        int y1 = (is2d && mode == PrintedModelMountMode.CEILING) ? (16 - b.minY()) : b.maxY();

        return switch (facing) {
            case EAST -> new VoxelCollisionMerger.Box16(16 - b.maxZ(), y0, b.minX(), 16 - b.minZ(), y1, b.maxX());
            case SOUTH -> new VoxelCollisionMerger.Box16(16 - b.maxX(), y0, 16 - b.maxZ(), 16 - b.minX(), y1, 16 - b.minZ());
            case WEST -> new VoxelCollisionMerger.Box16(b.minZ(), y0, 16 - b.maxX(), b.maxZ(), y1, 16 - b.minX());
            default -> new VoxelCollisionMerger.Box16(b.minX(), y0, b.minZ(), b.maxX(), y1, b.maxZ());
        };
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        CompoundTag metaTag = input.read("PrintedMeta", CompoundTag.CODEC).orElse(new CompoundTag());
        this.metadata = CanvasMetadata.readFromTag(metaTag);
        this.cachedCellShapes.clear();
        this.cachedShapeHash = null;
        if (this.level != null && this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        CompoundTag metaTag = new CompoundTag();
        CanvasMetadata.writeToTag(metaTag, this.metadata);
        output.store("PrintedMeta", CompoundTag.CODEC, metaTag);
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
