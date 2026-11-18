package fr.minevegas.casinosound.client;

import fr.minevegas.casinosound.CasinoSoundMod;
import fr.minevegas.casinosound.MusicTimeline;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public abstract class SynchronizedMusic extends AbstractTickableSoundInstance {
   private static final ExecutorService AUDIO = Executors.newSingleThreadExecutor((r) -> {
      Thread t = new Thread(r, "Casino Sound audio");
      t.setDaemon(true);
      t.setPriority(1);
      return t;
   });
   final int track;
   final long epoch;
   final long seed;
   final long cycle;
   private volatile long offsetMs;
   private volatile boolean cancelled;
   private float target;

   protected SynchronizedMusic(MusicTimeline.Phase phase, long epoch, long seed, float volume) {
      super(CasinoSoundMod.TRACKS[phase.track()], SoundSource.RECORDS, RandomSource.create());
      this.track = phase.track();
      this.cycle = phase.cycle();
      this.epoch = epoch;
      this.seed = seed;
      this.offsetMs = phase.offsetMs();
      this.relative = true;
      this.attenuation = net.minecraft.client.resources.sounds.SoundInstance.Attenuation.NONE;
      this.pitch = 1.0F;
      this.looping = false;
      this.volume = 0.0F;
      this.target = volume;
   }

   public boolean canStartSilent() {
      return true;
   }

   public boolean canPlaySound() {
      return !this.cancelled;
   }

   void target(float value) {
      this.target = value;
   }

   void phase(MusicTimeline.Phase value) {
      this.offsetMs = value.offsetMs();
   }

   void finish() {
      this.cancelled = true;
      this.stop();
   }

   public void tick() {
      this.volume += (this.target - this.volume) * 0.22F;
   }

   protected final CompletableFuture<AudioStream> openStream(Identifier file) {
      ResourceManager resources = Minecraft.getInstance().getResourceManager();
      return CompletableFuture.supplyAsync(() -> {
         NativeOggStream stream = null;

         try {
            InputStream input = resources.open(file);

            NativeOggStream var5;
            try {
               stream = new NativeOggStream(input.readAllBytes());
               stream.seekMs(Math.min(MusicTimeline.LENGTH_MS[this.track] - 1L, this.offsetMs));
               if (this.cancelled) {
                  stream.close();
               }

               var5 = stream;
            } catch (Throwable var8) {
               if (input != null) {
                  try {
                     input.close();
                  } catch (Throwable x2) {
                     var8.addSuppressed(x2);
                  }
               }

               throw var8;
            }

            if (input != null) {
               input.close();
            }

            return var5;
         } catch (Exception e) {
            if (stream != null) {
               stream.close();
            }

            throw new CompletionException(e);
         }
      }, AUDIO);
   }
}
