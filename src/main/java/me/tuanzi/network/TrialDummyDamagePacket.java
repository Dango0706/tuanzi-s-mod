package me.tuanzi.network;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TrialDummyDamagePacket(int entityId, float damage) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TrialDummyDamagePacket> TYPE = 
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "trial_dummy_damage"));

    public static final StreamCodec<FriendlyByteBuf, TrialDummyDamagePacket> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        TrialDummyDamagePacket::entityId,
        ByteBufCodecs.FLOAT,
        TrialDummyDamagePacket::damage,
        TrialDummyDamagePacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
