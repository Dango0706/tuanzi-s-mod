package me.tuanzi.world.canvas;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * 碎纸机纯逻辑辅助类：校验损毁权限（严格仅画布本人）与计算画布对应的合成材料返还数量。
 */
public final class CanvasShredderHelper {
    private CanvasShredderHelper() {
    }

    /**
     * 校验玩家是否有权损毁该画布：
     * 已绑定主人的画布严格仅本人可损毁；未绑定主人的空白画布不限。
     */
    public static boolean canPlayerShred(CanvasMetadata meta, @Nullable UUID playerUuid) {
        if (meta == null) {
            return false;
        }
        if (meta.ownerUuid() == null) {
            return true;
        }
        return meta.ownerUuid().equals(playerUuid);
    }

    /**
     * 计算损毁该画布时应返还的画布合成材料（不含画布内建筑消耗的任何材料）。
     * 基础 16×16 画布消耗 1 纸 + 1 白色染料 + 1 线；每次扩容（16→32→64→128→256）额外消耗 8 纸。
     */
    public static List<ItemStack> calculateRefundMaterials(CanvasMetadata meta) {
        int size = meta != null ? Math.max(16, meta.size()) : 16;
        int upgradeSteps = 0;
        for (int s = 16; s < size; s *= 2) {
            upgradeSteps++;
        }
        int paperCount = 1 + 8 * upgradeSteps;
        return List.of(
                new ItemStack(Items.PAPER, paperCount),
                new ItemStack(Items.DYE.white(), 1),
                new ItemStack(Items.STRING, 1)
        );
    }

    public static int calculatePaperRefundCount(CanvasMetadata meta) {
        int size = meta != null ? Math.max(16, meta.size()) : 16;
        int upgradeSteps = 0;
        for (int s = 16; s < size; s *= 2) {
            upgradeSteps++;
        }
        return 1 + 8 * upgradeSteps;
    }
}
