package fr.minevegas.casino.platform;

import fr.minevegas.casinosound.MusicTimeline;
import fr.minevegas.casinosound.client.SynchronizedMusic;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public interface CasinoClientPlatform {
   SynchronizedMusic music(MusicTimeline.Phase var1, long var2, long var4, float var6);

   void sendToServer(CustomPacketPayload var1);
}
