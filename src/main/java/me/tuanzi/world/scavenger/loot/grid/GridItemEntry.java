package me.tuanzi.world.scavenger.loot.grid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.tuanzi.util.ItemStackNbtHelper;
import me.tuanzi.world.scavenger.loot.data.ScavengerDataRegistry;
import me.tuanzi.world.scavenger.loot.meta.ScavengerItemMeta;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * 二维矩形网格内的单件物品实例条目（不可变值对象）。
 * <p>
 * 核心铁律：所有拾荒网格内物品最大堆叠数恒为 1（构造时自动截断为单件副本），
 * 搜索时长（{@code solidifiedSearchTicks}）在物品首次生成入箱时固化，取消重搜不会重骰。
 */
public record GridItemEntry(
        int entryId,
        int anchorX,
        int anchorY,
        boolean rotated,
        ItemStack stack,
        int solidifiedSearchTicks
) {

    public static final Codec<GridItemEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ExtraCodecs.POSITIVE_INT.fieldOf("entry_id").forGetter(GridItemEntry::entryId),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("x").forGetter(GridItemEntry::anchorX),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("y").forGetter(GridItemEntry::anchorY),
            Codec.BOOL.optionalFieldOf("rotated", false).forGetter(GridItemEntry::rotated),
            ItemStack.CODEC.fieldOf("stack").forGetter(GridItemEntry::stack),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("search_ticks", 20).forGetter(GridItemEntry::solidifiedSearchTicks)
    ).apply(instance, GridItemEntry::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, GridItemEntry> STREAM_CODEC = StreamCodec.of(
            (buf, entry) -> {
                buf.writeVarInt(entry.entryId());
                buf.writeVarInt(entry.anchorX());
                buf.writeVarInt(entry.anchorY());
                buf.writeBoolean(entry.rotated());
                ItemStack.STREAM_CODEC.encode(buf, entry.stack());
                buf.writeVarInt(entry.solidifiedSearchTicks());
            },
            buf -> new GridItemEntry(
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readBoolean(),
                    ItemStack.STREAM_CODEC.decode(buf),
                    buf.readVarInt()
            )
    );

    public GridItemEntry {
        entryId = Math.max(1, entryId);
        anchorX = Math.max(0, anchorX);
        anchorY = Math.max(0, anchorY);
        stack = (stack == null || stack.isEmpty()) ? ItemStack.EMPTY : stack.copyWithCount(1);
        solidifiedSearchTicks = Math.max(1, solidifiedSearchTicks);
    }

    public static Optional<GridItemEntry> fromTag(HolderLookup.Provider registries, CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return Optional.empty();
        }
        ItemStack parsedStack = ItemStackNbtHelper.read(registries, tag.getCompoundOrEmpty("stack"));
        if (parsedStack.isEmpty()) {
            return Optional.empty();
        }
        int id = tag.getIntOr("entry_id", 1);
        int x = tag.getIntOr("x", 0);
        int y = tag.getIntOr("y", 0);
        boolean rot = tag.getBooleanOr("rotated", false);
        int searchTicks = tag.getIntOr("search_ticks", 20);
        return Optional.of(new GridItemEntry(id, x, y, rot, parsedStack, searchTicks));
    }

    /**
     * 获取内部物品栈的安全深拷贝（防止调用方意外篡改网格内物品状态或数量）。
     */
    public ItemStack copyStack() {
        return this.stack.isEmpty() ? ItemStack.EMPTY : this.stack.copyWithCount(1);
    }

    /**
     * 动态解析当前生效的数据包元数据（未注册原版物品自动降级为 1x1 兜底规格）。
     */
    public ScavengerItemMeta resolveMeta() {
        return ScavengerDataRegistry.resolveItemMeta(this.stack);
    }

    /**
     * 获取规范化后的旋转标记（正方形物品如 1x1、2x2 恒为 false）。
     */
    public boolean effectiveRotated() {
        ScavengerItemMeta meta = resolveMeta();
        return this.rotated && meta.canRotate();
    }

    public int effectiveWidth() {
        return resolveMeta().effectiveWidth(this.rotated);
    }

    public int effectiveHeight() {
        return resolveMeta().effectiveHeight(this.rotated);
    }

    /**
     * 判断指定网格坐标 {@code (cellX, cellY)} 是否落在该条目的矩形占地范围内。
     */
    public boolean occupiesCell(int cellX, int cellY) {
        int w = effectiveWidth();
        int h = effectiveHeight();
        return cellX >= this.anchorX && cellX < this.anchorX + w
                && cellY >= this.anchorY && cellY < this.anchorY + h;
    }

    /**
     * 返回更新锚点坐标与旋转朝向后的新不可变条目（保持 entryId 与固化搜索时长不变）。
     */
    public GridItemEntry withPosition(int newAnchorX, int newAnchorY, boolean newRotated) {
        ScavengerItemMeta meta = resolveMeta();
        boolean normRotated = newRotated && meta.canRotate();
        return new GridItemEntry(
                this.entryId,
                newAnchorX,
                newAnchorY,
                normRotated,
                this.stack,
                this.solidifiedSearchTicks
        );
    }

    public CompoundTag toTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("entry_id", this.entryId);
        tag.putInt("x", this.anchorX);
        tag.putInt("y", this.anchorY);
        tag.putBoolean("rotated", this.rotated);
        tag.putInt("search_ticks", this.solidifiedSearchTicks);
        tag.put("stack", ItemStackNbtHelper.write(registries, this.stack));
        return tag;
    }
}
