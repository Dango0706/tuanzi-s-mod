package me.tuanzi.network;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 拾荒回廊组队数据 S2C 同步数据包
 * 用于向客户端同步自身队伍状态、高亮发光颜色与同队队友 UUID 列表。
 */
public record ScavengerTeamSyncPacket(boolean inTeam, int colorRgb,
                                      List<UUID> memberUuids) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ScavengerTeamSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "scavenger_team_sync"));

    public static final StreamCodec<FriendlyByteBuf, ScavengerTeamSyncPacket> CODEC = CustomPacketPayload.codec(
            ScavengerTeamSyncPacket::write,
            ScavengerTeamSyncPacket::new
    );

    public ScavengerTeamSyncPacket(FriendlyByteBuf buf) {
        this(buf.readBoolean(), buf.readInt(), readUuids(buf));
    }

    private static List<UUID> readUuids(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<UUID> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(buf.readUUID());
        }
        return list;
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeBoolean(this.inTeam);
        buf.writeInt(this.colorRgb);
        buf.writeVarInt(this.memberUuids.size());
        for (UUID uuid : this.memberUuids) {
            buf.writeUUID(uuid);
        }
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
