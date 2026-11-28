package me.tuanzi.block.entity;

import me.tuanzi.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * 多格打印模型占位方块实体：记录主方块坐标与相对单元偏移，代理碰撞箱查询与联动破坏。
 */
public class PrintedModelPartBlockEntity extends BlockEntity {
    private BlockPos masterPos = BlockPos.ZERO;
    private int cellX = 0;
    private int cellY = 0;
    private int cellZ = 0;

    public PrintedModelPartBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.PRINTED_MODEL_PART_BLOCK_ENTITY, pos, state);
    }

    public void setPartInfo(BlockPos masterPos, int cellX, int cellY, int cellZ) {
        this.masterPos = masterPos.immutable();
        this.cellX = cellX;
        this.cellY = cellY;
        this.cellZ = cellZ;
        setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public BlockPos getMasterPos() {
        return masterPos;
    }

    public int getCellX() {
        return cellX;
    }

    public int getCellY() {
        return cellY;
    }

    public int getCellZ() {
        return cellZ;
    }

    public @Nullable PrintedModelBlockEntity getMasterEntity() {
        if (this.level == null) {
            return null;
        }
        BlockEntity be = this.level.getBlockEntity(masterPos);
        return be instanceof PrintedModelBlockEntity modelBe ? modelBe : null;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int mx = input.getIntOr("MasterX", worldPosition.getX());
        int my = input.getIntOr("MasterY", worldPosition.getY());
        int mz = input.getIntOr("MasterZ", worldPosition.getZ());
        this.masterPos = new BlockPos(mx, my, mz);
        this.cellX = input.getIntOr("CellX", 0);
        this.cellY = input.getIntOr("CellY", 0);
        this.cellZ = input.getIntOr("CellZ", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("MasterX", masterPos.getX());
        output.putInt("MasterY", masterPos.getY());
        output.putInt("MasterZ", masterPos.getZ());
        output.putInt("CellX", cellX);
        output.putInt("CellY", cellY);
        output.putInt("CellZ", cellZ);
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
