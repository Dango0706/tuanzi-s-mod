package fr.minevegas.casinosound;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class SpeakerBlockEntity extends BlockEntity {
   private long epoch = -1L;
   private long seed;
   private boolean clockRefreshPending = true;
   private static final Set<SpeakerBlockEntity> CLIENT_LOADED = Collections.newSetFromMap(new IdentityHashMap());

   public static Set<SpeakerBlockEntity> clientLoaded() {
      return CLIENT_LOADED;
   }

   public SpeakerBlockEntity(BlockPos pos, BlockState state) {
      super(CasinoSoundMod.SPEAKER_ENTITY, pos, state);
   }

   public long epoch() {
      return this.epoch;
   }

   public long seed() {
      return this.seed;
   }

   public boolean enabled() {
      return !this.isRemoved() && this.getBlockState().is(CasinoSoundMod.SPEAKER) && (Boolean)this.getBlockState().getValue(SpeakerBlock.ON);
   }

   public void requestClockRefresh() {
      this.clockRefreshPending = true;
   }

   public void clearRemoved() {
      super.clearRemoved();
      if (this.level instanceof ServerLevel) {
         this.requestClockRefresh();
      } else if (this.level != null && this.level.isClientSide()) {
         CLIENT_LOADED.add(this);
      }

   }

   public void setRemoved() {
      super.setRemoved();
      if (this.level != null && this.level.isClientSide()) {
         CLIENT_LOADED.remove(this);
      }

   }

   public void refreshClock() {
      Level var2 = this.level;
      if (var2 instanceof ServerLevel server) {
         this.clockRefreshPending = false;
         CasinoStation var3 = CasinoStation.get(server);
         if (this.enabled()) {
            var3.enable(this.worldPosition, server);
         } else {
            var3.disable(this.worldPosition);
         }

         this.epoch = var3.epoch();
         this.seed = var3.seed();
         this.setChanged();
         server.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
      }
   }

   public static void serverTick(ServerLevel level, SpeakerBlockEntity speaker) {
      if (speaker.clockRefreshPending || speaker.enabled() && Math.floorMod(level.getGameTime() + speaker.worldPosition.asLong(), 100) == 0) {
         speaker.refreshClock();
      }

   }

   protected void loadAdditional(ValueInput input) {
      super.loadAdditional(input);
      this.epoch = input.getLongOr("epoch", -1L);
      this.seed = input.getLongOr("seed", 0L);
   }

   protected void saveAdditional(ValueOutput output) {
      super.saveAdditional(output);
      Level var3 = this.level;
      if (var3 instanceof ServerLevel server) {
         CasinoStation station = CasinoStation.get(server);
         this.epoch = station.epoch();
         this.seed = station.seed();
      }

      output.putLong("epoch", this.epoch);
      output.putLong("seed", this.seed);
   }

   public CompoundTag getUpdateTag(HolderLookup.Provider lookup) {
      return this.saveWithoutMetadata(lookup);
   }

   public Packet<ClientGamePacketListener> getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   public void preRemoveSideEffects(BlockPos pos, BlockState state) {
      Level var4 = this.level;
      if (var4 instanceof ServerLevel server) {
         CasinoStation.get(server).disable(this.worldPosition);
      }

      super.preRemoveSideEffects(pos, state);
   }
}
