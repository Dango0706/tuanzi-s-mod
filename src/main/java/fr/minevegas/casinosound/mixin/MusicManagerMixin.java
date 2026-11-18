package fr.minevegas.casinosound.mixin;

import fr.minevegas.casinosound.client.CasinoSoundClient;
import net.minecraft.client.sounds.MusicManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({MusicManager.class})
public abstract class MusicManagerMixin {
   @Inject(
      method = {"tick"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void casinoSound$backgroundMusic(CallbackInfo ci) {
      if (CasinoSoundClient.suppressVanillaMusic()) {
         ((MusicManager)(Object)this).stopPlaying();
         ci.cancel();
      }

   }
}

