package me.tuanzi.block.entity;

import me.tuanzi.block.ElevatorBlock;
import me.tuanzi.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * 电梯方块实体：持久化并向客户端同步当前伪装的方块状态。
 */
public class ElevatorBlockEntity extends BlockEntity {
    @Nullable
    private BlockState disguiseState;

    public ElevatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.ELEVATOR_BLOCK_ENTITY, pos, state);
    }

    @Nullable
    public BlockState getDisguiseState() {
        return this.disguiseState;
    }

    public boolean hasDisguise() {
        return this.disguiseState != null
                && !this.disguiseState.isAir()
                && !(this.disguiseState.getBlock() instanceof ElevatorBlock);
    }

    public void setDisguiseState(@Nullable BlockState newDisguise) {
        BlockState sanitized = (newDisguise != null && !newDisguise.isAir() && !(newDisguise.getBlock() instanceof ElevatorBlock))
                ? newDisguise
                : null;
        if (Objects.equals(this.disguiseState, sanitized)) {
            return;
        }
        this.disguiseState = sanitized;
        this.setChanged();

        if (this.level != null && !this.level.isClientSide()) {
            BlockState current = this.getBlockState();
            boolean shouldBeDisguised = sanitized != null;
            BlockState targetBlockState = current;
            if (current.getBlock() instanceof ElevatorBlock && current.getValue(ElevatorBlock.DISGUISED) != shouldBeDisguised) {
                targetBlockState = current.setValue(ElevatorBlock.DISGUISED, shouldBeDisguised);
                this.level.setBlock(this.worldPosition, targetBlockState, Block.UPDATE_ALL);
            }
            this.level.sendBlockUpdated(this.worldPosition, current, targetBlockState, Block.UPDATE_ALL);
        }
    }

    public void clearDisguise() {
        this.setDisguiseState(null);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        BlockState oldState = this.disguiseState;
        BlockState loaded = input.read("DisguiseState", BlockState.CODEC).orElse(null);
        this.disguiseState = (loaded != null && !loaded.isAir() && !(loaded.getBlock() instanceof ElevatorBlock)) ? loaded : null;
        if (this.level != null && this.level.isClientSide() && !Objects.equals(oldState, this.disguiseState)) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.storeNullable("DisguiseState", BlockState.CODEC, this.disguiseState);
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
