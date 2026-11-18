package fr.minevegas.casinosound.client;

import fr.minevegas.casino.platform.CasinoClientPlatform;
import fr.minevegas.casino.platform.Services;
import fr.minevegas.casinosound.MusicTimeline;
import fr.minevegas.casinosound.SpeakerBlock;
import fr.minevegas.casinosound.SpeakerBlockEntity;
import java.util.Iterator;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

public final class CasinoSoundClient {
   public static final double RANGE = (double)48.0F;
   public static final double FULL_VOLUME_RANGE = (double)12.0F;
   private static SynchronizedMusic music;
   private static boolean covered;
   private static long retryAt;

   private CasinoSoundClient() {
   }

   public static boolean suppressVanillaMusic() {
      return covered;
   }

   private static void stop(Minecraft mc) {
      if (music != null) {
         music.finish();
         mc.getSoundManager().stop(music);
         music = null;
      }

   }

   public static void tick(Minecraft mc) {
      Set<SpeakerBlockEntity> loaded = SpeakerBlockEntity.clientLoaded();
      if (mc.level != null && mc.player != null) {
         if (!mc.isPaused()) {
            double distance = Double.POSITIVE_INFINITY;
            SpeakerBlockEntity nearest = null;
            Iterator<SpeakerBlockEntity> iterator = loaded.iterator();

            while(iterator.hasNext()) {
               SpeakerBlockEntity speaker = (SpeakerBlockEntity)iterator.next();
               if (!speaker.isRemoved() && speaker.getLevel() == mc.level) {
                  if (speaker.enabled() && speaker.epoch() >= 0L) {
                     double d = mc.player.position().distanceToSqr(Vec3.atCenterOf(speaker.getBlockPos()));
                     if (d < distance) {
                        distance = d;
                        nearest = speaker;
                     }

                     if (d < (double)1024.0F && Math.floorMod(mc.level.getGameTime() + speaker.getBlockPos().asLong(), 32) == 0) {
                        Direction face = (Direction)speaker.getBlockState().getValue(SpeakerBlock.FACING);
                        Vec3 pos = Vec3.atCenterOf(speaker.getBlockPos());
                        mc.level.addParticle(ParticleTypes.NOTE, pos.x + (double)face.getStepX() * 0.34, pos.y + 0.12, pos.z + (double)face.getStepZ() * 0.34, (double)mc.level.getRandom().nextInt(24) / (double)24.0F, (double)0.0F, (double)0.0F);
                     }
                  }
               } else {
                  iterator.remove();
               }
            }

            covered = nearest != null && distance < (double)2304.0F;
            if (!covered) {
               stop(mc);
               retryAt = 0L;
            } else {
               mc.getMusicManager().stopPlaying();
               double t = Math.clamp((Math.sqrt(distance) - (double)12.0F) / (double)36.0F, (double)0.0F, (double)1.0F);
               float volume = (float)(0.7 * ((double)1.0F - t * t * ((double)3.0F - (double)2.0F * t)));
               MusicTimeline.Phase phase = MusicTimeline.at((mc.level.getGameTime() - nearest.epoch()) * 50L, nearest.seed());
               boolean changed = music == null || music.track != phase.track() || music.cycle != phase.cycle() || music.epoch != nearest.epoch() || music.seed != nearest.seed();
               if (changed) {
                  stop(mc);
                  music = CasinoSoundClient.Platform.INSTANCE.music(phase, nearest.epoch(), nearest.seed(), volume);
                  mc.getSoundManager().play(music);
                  retryAt = System.nanoTime() + 2000000000L;
               } else if (System.nanoTime() > retryAt && !mc.getSoundManager().isActive(music)) {
                  stop(mc);
                  music = CasinoSoundClient.Platform.INSTANCE.music(phase, nearest.epoch(), nearest.seed(), volume);
                  mc.getSoundManager().play(music);
                  retryAt = System.nanoTime() + 2000000000L;
               }

               music.target(volume);
               music.phase(phase);
            }
         }
      } else {
         covered = false;
         stop(mc);
         loaded.clear();
         retryAt = 0L;
      }
   }

   private static final class Platform {
      static final CasinoClientPlatform INSTANCE = (CasinoClientPlatform)Services.load(CasinoClientPlatform.class);
   }
}
