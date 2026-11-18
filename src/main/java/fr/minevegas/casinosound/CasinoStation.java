package fr.minevegas.casinosound;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.LongSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class CasinoStation extends SavedData {
   static final Codec<CasinoStation> CODEC = RecordCodecBuilder.<CasinoStation>create((i) -> i.group(Codec.LONG.optionalFieldOf("epoch", -1L).forGetter((s) -> s.epoch), Codec.LONG.optionalFieldOf("seed", 0L).forGetter((s) -> s.seed), Codec.LONG.listOf().optionalFieldOf("speakers", List.of()).forGetter((s) -> new ArrayList(s.speakers))).apply(i, CasinoStation::new));
   private static final SavedDataType<CasinoStation> TYPE;
   private final Set<Long> speakers = new HashSet();
   private long epoch = -1L;
   private long seed;

   public CasinoStation() {
   }

   private CasinoStation(long epoch, long seed, List<Long> positions) {
      this.epoch = epoch;
      this.seed = seed;
      this.speakers.addAll(positions);
   }

   public static CasinoStation get(ServerLevel level) {
      return (CasinoStation)level.getDataStorage().computeIfAbsent(TYPE);
   }

   public long epoch() {
      return this.epoch;
   }

   public long seed() {
      return this.seed;
   }

   public void enable(BlockPos pos, ServerLevel level) {
      this.enable(pos, level.getGameTime(), () -> level.getRandom().nextLong());
   }

   void enable(BlockPos pos, long time, LongSupplier randomSeed) {
      if (this.speakers.isEmpty() || this.epoch < 0L) {
         this.epoch = time;
         this.seed = randomSeed.getAsLong();
         this.setDirty();
      }

      if (this.speakers.add(pos.asLong())) {
         this.setDirty();
      }

   }

   public void disable(BlockPos pos) {
      if (this.speakers.remove(pos.asLong())) {
         if (this.speakers.isEmpty()) {
            this.epoch = -1L;
         }

         this.setDirty();
      }

   }

   public void pruneLoaded(ServerLevel level) {
      boolean changed = this.speakers.removeIf((p) -> {
         BlockPos pos = BlockPos.of(p);
         if (!level.hasChunkAt(pos)) {
            return false;
         } else {
            BlockState state = level.getBlockState(pos);
            return !state.is(CasinoSoundMod.SPEAKER) || !(Boolean)state.getValue(SpeakerBlock.ON);
         }
      });
      if (changed) {
         if (this.speakers.isEmpty()) {
            this.epoch = -1L;
         }

         this.setDirty();
      }

   }

   static {
      TYPE = new SavedDataType<CasinoStation>(Identifier.fromNamespaceAndPath("casinosound", "station"), CasinoStation::new, CODEC, DataFixTypes.LEVEL);
   }
}
