package fr.minevegas.casino.fabric;

import fr.minevegas.casino.platform.CasinoClientPlatform;
import fr.minevegas.casinosound.MusicTimeline;
import fr.minevegas.casinosound.client.SynchronizedMusic;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class FabricClientPlatform implements CasinoClientPlatform {
   public SynchronizedMusic music(MusicTimeline.Phase phase, long epoch, long seed, float volume) {
      return new Music(phase, epoch, seed, volume);
   }

   public void sendToServer(CustomPacketPayload payload) {
      ClientPlayNetworking.send(payload);
   }

   private static final class Music extends SynchronizedMusic {
      Music(MusicTimeline.Phase phase, long epoch, long seed, float volume) {
         super(phase, epoch, seed, volume);
      }

      public CompletableFuture<AudioStream> getAudioStream(SoundBufferLibrary library, Identifier file, boolean looping) {
         return this.openStream(file);
      }
   }
}
