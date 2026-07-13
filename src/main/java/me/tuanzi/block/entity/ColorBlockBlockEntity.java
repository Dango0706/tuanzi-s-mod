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

public class ColorBlockBlockEntity extends BlockEntity {
    private int color = 0xFFFFFF; // 默认白色

    public ColorBlockBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.COLOR_BLOCK_BLOCK_ENTITY, pos, state);
    }

    public int getColor() {
        return this.color;
    }

    public void setColor(int color) {
        me.tuanzi.util.ModLog.debug("[ColorBlockEntity] setColor on " + (this.level != null && this.level.isClientSide() ? "client" : "server") + ": " + String.format("#%06X", color));
        this.color = color;
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int oldColor = this.color;
        this.color = input.getIntOr("Color", 0xFFFFFF);
        me.tuanzi.util.ModLog.debug("[ColorBlockEntity] loadAdditional, loaded color: " + String.format("#%06X", this.color));
        if (this.color != oldColor && this.level != null && this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Color", this.color);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Color", this.color);
        return tag;
    }
}
