package me.tuanzi.world.scavenger.loot.search;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * 服务端下发给客户端的脱敏防透视（Anti-ESP）单条目网络投影。
 * <p>
 * 核心安全机制：
 * 当条目处于 {@link SearchEntryPhase#IDLE} 或 {@link SearchEntryPhase#SEARCHING} 时，
 * 无论是在内存中还是网络线缆上，{@code stack} 均被强制擦除为 {@link ItemStack#EMPTY}，
 * 杜绝任何客户端利用内存注入或网络抓包提前探知箱内物品类型与稀有度；
 * 仅当服务端裁定当前玩家（或其同小队队友）已将其彻底揭示（{@link SearchEntryPhase#REVEALED}）后，
 * 才会在网络包中携带真实的 {@link ItemStack}。
 */
public record RedactedGridEntryView(
        int entryId,
        int anchorX,
        int anchorY,
        int width,
        int height,
        boolean rotated,
        SearchEntryPhase phase,
        int searchProgressTicks,
        int totalSearchTicks,
        ItemStack stack
) {

    public static final StreamCodec<RegistryFriendlyByteBuf, RedactedGridEntryView> STREAM_CODEC = StreamCodec.of(
            (buf, view) -> {
                buf.writeVarInt(view.entryId());
                buf.writeVarInt(view.anchorX());
                buf.writeVarInt(view.anchorY());
                buf.writeVarInt(view.width());
                buf.writeVarInt(view.height());
                buf.writeBoolean(view.rotated());
                SearchEntryPhase.STREAM_CODEC.encode(buf, view.phase());
                buf.writeVarInt(view.searchProgressTicks());
                buf.writeVarInt(view.totalSearchTicks());
                // 防御性编码：未揭示条目在线缆上传输时恒为 EMPTY
                ItemStack safeStack = view.phase() == SearchEntryPhase.REVEALED
                        ? (view.stack() == null ? ItemStack.EMPTY : view.stack())
                        : ItemStack.EMPTY;
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, safeStack);
            },
            buf -> {
                int id = buf.readVarInt();
                int x = buf.readVarInt();
                int y = buf.readVarInt();
                int w = buf.readVarInt();
                int h = buf.readVarInt();
                boolean rot = buf.readBoolean();
                SearchEntryPhase phase = SearchEntryPhase.STREAM_CODEC.decode(buf);
                int progress = buf.readVarInt();
                int total = buf.readVarInt();
                ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                return new RedactedGridEntryView(id, x, y, w, h, rot, phase, progress, total, stack);
            }
    );

    public RedactedGridEntryView {
        entryId = Math.max(1, entryId);
        anchorX = Math.max(0, anchorX);
        anchorY = Math.max(0, anchorY);
        width = Math.max(1, width);
        height = Math.max(1, height);
        searchProgressTicks = Math.max(0, searchProgressTicks);
        totalSearchTicks = Math.max(1, totalSearchTicks);
        // 服务端脱敏保底：未揭示条目强行清理 stack
        if (phase != SearchEntryPhase.REVEALED) {
            stack = ItemStack.EMPTY;
        } else {
            stack = (stack == null || stack.isEmpty()) ? ItemStack.EMPTY : stack.copyWithCount(1);
        }
    }

    public boolean isRevealed() {
        return this.phase == SearchEntryPhase.REVEALED;
    }

    public int effectiveWidth() {
        return this.rotated ? this.height : this.width;
    }

    public int effectiveHeight() {
        return this.rotated ? this.width : this.height;
    }

    public float progressFraction() {
        if (this.phase == SearchEntryPhase.REVEALED) {
            return 1.0f;
        }
        return Math.min(1.0f, (float) this.searchProgressTicks / (float) this.totalSearchTicks);
    }
}
