package me.tuanzi.entity;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public class SeatEntity extends Entity {
    public SeatEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            // 如果没有乘客，说明玩家已经主动按下下车键，自动销毁实体以防残留
            if (this.getPassengers().isEmpty()) {
                this.discard();
                return;
            }

            // 检查当前方块是否被破坏或改变，若不再是楼梯，则销毁实体让玩家自动下车
            if (!(this.level().getBlockState(this.blockPosition()).getBlock() instanceof StairBlock)) {
                this.discard();
            }
        }
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        // SeatEntity 位于 Y = pos.getY() + 0.3，将其加上 0.7 即可让玩家脱离坐下时平稳落在楼梯顶部 (pos.getY() + 1.0)
        return new Vec3(this.getX(), this.getY() + 0.7, this.getZ());
    }

    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        return new ClientboundAddEntityPacket(this, serverEntity);
    }
}
