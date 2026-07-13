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

public class ColorSlabBlockEntity extends BlockEntity {
    private int bottomColor = 0xFFFFFF;
    private int topColor = 0xFFFFFF;
    private boolean bottomSet = false;
    private boolean topSet = false;

    public ColorSlabBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.COLOR_SLAB_BLOCK_ENTITY, pos, state);
    }

    public int getBottomColor() {
        return this.bottomColor;
    }

    public void setBottomColor(int color) {
        this.bottomColor = color;
        this.bottomSet = true;
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public int getTopColor() {
        return this.topColor;
    }

    public void setTopColor(int color) {
        this.topColor = color;
        this.topSet = true;
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public boolean isBottomSet() {
        return this.bottomSet;
    }

    public boolean isTopSet() {
        return this.topSet;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int oldBottom = this.bottomColor;
        int oldTop = this.topColor;
        this.bottomColor = input.getIntOr("BottomColor", 0xFFFFFF);
        this.topColor = input.getIntOr("TopColor", 0xFFFFFF);
        this.bottomSet = input.getBooleanOr("BottomSet", false);
        this.topSet = input.getBooleanOr("TopSet", false);
        if ((this.bottomColor != oldBottom || this.topColor != oldTop) && this.level != null && this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("BottomColor", this.bottomColor);
        output.putInt("TopColor", this.topColor);
        output.putBoolean("BottomSet", this.bottomSet);
        output.putBoolean("TopSet", this.topSet);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("BottomColor", this.bottomColor);
        tag.putInt("TopColor", this.topColor);
        tag.putBoolean("BottomSet", this.bottomSet);
        tag.putBoolean("TopSet", this.topSet);
        return tag;
    }
}
