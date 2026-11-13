package me.tuanzi.world.scavenger.loot.search;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

import java.util.function.IntFunction;

/**
 * 网格搜刮迷雾条目在客户端呈现的搜索阶段状态
 */
public enum SearchEntryPhase implements StringRepresentable {
    /**
     * 处于迷雾中，尚未开始搜索（未激活）
     */
    IDLE("idle", 0),
    /**
     * 正在搜寻中（客户端展示旋转放大镜与进度圆环）
     */
    SEARCHING("searching", 1),
    /**
     * 已完成搜索并揭示（迷雾驱散，展示真实物品与稀有度光晕）
     */
    REVEALED("revealed", 2);

    private static final IntFunction<SearchEntryPhase> BY_ID = ByIdMap.continuous(
            SearchEntryPhase::id,
            values(),
            ByIdMap.OutOfBoundsStrategy.ZERO
    );

    public static final StreamCodec<ByteBuf, SearchEntryPhase> STREAM_CODEC =
            ByteBufCodecs.idMapper(BY_ID, SearchEntryPhase::id);

    private final String serializedName;
    private final int id;

    SearchEntryPhase(String serializedName, int id) {
        this.serializedName = serializedName;
        this.id = id;
    }

    public int id() {
        return this.id;
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }
}
