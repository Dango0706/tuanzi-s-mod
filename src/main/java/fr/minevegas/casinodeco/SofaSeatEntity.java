package fr.minevegas.casinodeco;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

public final class SofaSeatEntity extends Entity {
   public SofaSeatEntity(EntityType<? extends SofaSeatEntity> type, Level level) {
      super(type, level);
      this.setNoGravity(true);
   }

   protected void defineSynchedData(SynchedEntityData.Builder builder) {
   }

   protected void readAdditionalSaveData(ValueInput input) {
   }

   protected void addAdditionalSaveData(ValueOutput output) {
   }

   public boolean shouldBeSaved() {
      return false;
   }

   public boolean save(ValueOutput output) {
      return false;
   }

   public boolean saveAsPassenger(ValueOutput output) {
      return false;
   }

   public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
      return false;
   }

   protected boolean canAddPassenger(Entity passenger) {
      return passenger instanceof Player && !this.isVehicle();
   }

   protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
      return Vec3.ZERO;
   }

   public void tick() {
      super.tick();
      this.setDeltaMovement(Vec3.ZERO);
      if (!this.level().isClientSide()) {
         for(Entity passenger : List.copyOf(this.getPassengers())) {
            if (passenger.isRemoved() || !passenger.isAlive() || passenger.level() != this.level()) {
               passenger.stopRiding();
            }
         }

         if (!this.isVehicle() || !this.level().getBlockState(this.blockPosition()).is(CasinoDecoMod.SOFA)) {
            this.ejectPassengers();
            this.discard();
         }
      }

   }

   protected void removePassenger(Entity passenger) {
      super.removePassenger(passenger);
      if (!this.level().isClientSide() && !this.isRemoved()) {
         this.playSound(SoundEvents.CUSHION_GET_UP, 0.7F, 1.0F);
      }

   }

   public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
      BlockPos origin = this.blockPosition();
      Direction front = Direction.fromYRot((double)this.getYRot());

      for(Direction direction : new Direction[]{front, front.getClockWise(), front.getCounterClockWise(), front.getOpposite()}) {
         BlockPos candidate = origin.relative(direction);

         for(int dy : new int[]{0, 1, -1}) {
            BlockPos target = candidate.offset(0, dy, 0);
            Vec3 safe = DismountHelper.findSafeDismountLocation(passenger.getType(), this.level(), target, true);
            if (safe != null && this.level().getWorldBorder().isWithinBounds(target)) {
               return safe;
            }
         }
      }

      return new Vec3(this.getX(), (double)origin.getY() + (double)1.5F, this.getZ());
   }
}
