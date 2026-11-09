package me.tuanzi.network;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 拾荒回廊专属信标快捷激活 C2S 数据包
 * slot: 0 为归厅信标，1 为归室信标
 */
public record CorridorBeaconUsePacket(int slot) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CorridorBeaconUsePacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "corridor_beacon_use"));

    public static final StreamCodec<FriendlyByteBuf, CorridorBeaconUsePacket> CODEC = CustomPacketPayload.codec(
            (packet, buf) -> buf.writeVarInt(packet.slot()),
            buf -> new CorridorBeaconUsePacket(buf.readVarInt())
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
