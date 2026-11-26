package me.tuanzi.block.cannon;

import me.tuanzi.block.entity.BlueprintCannonBlockEntity.ContainerSource;
import me.tuanzi.init.ModItems;
import me.tuanzi.util.ModLog;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 蓝图大炮彩色方块材料匹配与油漆桶染色消耗逻辑：
 * 1. 优先消耗容器中对应精确 RGB 颜色的同类彩色方块/半砖/楼梯；
 * 2. 若缺少对应颜色，检查容器中是否存在可用耐久的油漆桶（tuanzis_mod:paint_bucket）与其他颜色的同类彩色方块；
 * 3. 每染色打印 1 个方块，消耗 1 个其他颜色的同类方块 + 1 点油漆桶耐久（耐久耗尽时变为空铁桶 Items.BUCKET，与合成配方 ColorBlockDyeRecipe 完全一致）。
 */
public final class BlueprintColorMaterialHelper {

    private BlueprintColorMaterialHelper() {
    }

    public static int getItemColor(ItemStack stack) {
        return DyedItemColor.getOrDefault(stack, 0xFFFFFF) & 0xFFFFFF;
    }

    public static ItemStack createColoredDisplayStack(Item item, int color) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.DYED_COLOR, new DyedItemColor(color & 0xFFFFFF));
        return stack;
    }

    public static int getPaintBucketRemainingDurability(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(ModItems.PAINT_BUCKET)) {
            return 0;
        }
        int maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) {
            return 0;
        }
        return Math.max(0, maxDamage - stack.getDamageValue());
    }

    /**
     * 统计容器源池中全部油漆桶剩余总可用耐久点数。
     */
    public static int countTotalPaintBucketDurability(@Nullable List<ContainerSource> sources) {
        if (sources == null || sources.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (ContainerSource src : sources) {
            ItemStack stack = src.container.getItem(src.slot);
            total += getPaintBucketRemainingDurability(stack);
        }
        return total;
    }

    /**
     * 无副作用校验容器池是否能满足本次打印所需的全部目标颜色（支持双层彩色半砖一次需求 2 个不同颜色）。
     */
    public static boolean canFulfillColorRequirements(
            @Nullable List<ContainerSource> sources,
            Item requiredItem,
            int[] requiredColors
    ) {
        if (requiredColors == null || requiredColors.length == 0) {
            return true;
        }
        if (sources == null || sources.isEmpty()) {
            return false;
        }

        Map<Integer, Integer> colorCounts = new HashMap<>();
        int totalItemsOfType = 0;
        int totalBucketDurability = 0;

        for (ContainerSource src : sources) {
            ItemStack stack = src.container.getItem(src.slot);
            if (stack.isEmpty()) continue;
            if (stack.is(requiredItem)) {
                int col = getItemColor(stack);
                colorCounts.merge(col, stack.getCount(), Integer::sum);
                totalItemsOfType += stack.getCount();
            } else if (stack.is(ModItems.PAINT_BUCKET)) {
                totalBucketDurability += getPaintBucketRemainingDurability(stack);
            }
        }

        if (totalItemsOfType < requiredColors.length) {
            return false;
        }

        int exactMatched = 0;
        for (int target : requiredColors) {
            int normalized = target & 0xFFFFFF;
            int avail = colorCounts.getOrDefault(normalized, 0);
            if (avail > 0) {
                colorCounts.put(normalized, avail - 1);
                exactMatched++;
            }
        }

        int needDye = requiredColors.length - exactMatched;
        if (needDye <= 0) {
            return true;
        }
        int remainingFallbackItems = totalItemsOfType - exactMatched;
        boolean ok = remainingFallbackItems >= needDye && totalBucketDurability >= needDye;
        ModLog.debug("[BlueprintColorMaterialHelper] 染色替代校验: item=" + requiredItem
                + ", exactMatched=" + exactMatched + ", needDye=" + needDye
                + ", fallbackItems=" + remainingFallbackItems
                + ", bucketDurability=" + totalBucketDurability + ", ok=" + ok);
        return ok;
    }

    /**
     * 按规则消耗对应颜色的彩色方块；若无对应颜色则消耗其他颜色同类方块 + 1 点油漆桶耐久（耗尽变空桶）。
     * 调用前需先通过 {@link #canFulfillColorRequirements} 校验。
     */
    public static ItemStack consumeColorRequirements(
            @Nullable List<ContainerSource> sources,
            Item requiredItem,
            int[] requiredColors
    ) {
        if (requiredColors == null || requiredColors.length == 0) {
            return new ItemStack(requiredItem);
        }
        int primaryColor = requiredColors[0] & 0xFFFFFF;
        ItemStack displayStack = createColoredDisplayStack(requiredItem, primaryColor);
        if (sources == null || sources.isEmpty()) {
            return displayStack;
        }

        boolean[] fulfilled = new boolean[requiredColors.length];

        // Pass 1: 优先精确消耗对应颜色的彩色方块
        for (int i = 0; i < requiredColors.length; i++) {
            int targetColor = requiredColors[i] & 0xFFFFFF;
            if (consumeOneExactColor(sources, requiredItem, targetColor)) {
                fulfilled[i] = true;
                ModLog.debug("[BlueprintColorMaterialHelper] 精确消耗对应颜色方块: "
                        + requiredItem + " #" + String.format("%06X", targetColor));
            }
        }

        // Pass 2: 未匹配项使用其他颜色同类方块 + 油漆桶 1 点耐久染色
        for (int i = 0; i < requiredColors.length; i++) {
            if (fulfilled[i]) continue;
            int targetColor = requiredColors[i] & 0xFFFFFF;
            Integer consumedSrcColor = consumeOneAnyColor(sources, requiredItem);
            if (consumedSrcColor != null) {
                boolean bucketUsed = consumeOnePaintBucketDurability(sources);
                ModLog.debug("[BlueprintColorMaterialHelper] 油漆桶染色消耗: "
                        + requiredItem + " #" + String.format("%06X", consumedSrcColor)
                        + " -> #" + String.format("%06X", targetColor)
                        + ", bucketUsed=" + bucketUsed);
            }
        }

        return displayStack;
    }

    private static boolean consumeOneExactColor(List<ContainerSource> sources, Item requiredItem, int targetColor) {
        for (ContainerSource src : sources) {
            ItemStack stack = src.container.getItem(src.slot);
            if (!stack.isEmpty() && stack.is(requiredItem) && getItemColor(stack) == targetColor) {
                stack.shrink(1);
                src.markChanged();
                return true;
            }
        }
        return false;
    }

    private static @Nullable Integer consumeOneAnyColor(List<ContainerSource> sources, Item requiredItem) {
        for (ContainerSource src : sources) {
            ItemStack stack = src.container.getItem(src.slot);
            if (!stack.isEmpty() && stack.is(requiredItem)) {
                int srcColor = getItemColor(stack);
                stack.shrink(1);
                src.markChanged();
                return srcColor;
            }
        }
        return null;
    }

    /**
     * 从容器池中消耗 1 点油漆桶耐久；若耐久耗尽（damage + 1 >= maxDamage）则将该槽位替换为空铁桶 Items.BUCKET。
     */
    public static boolean consumeOnePaintBucketDurability(List<ContainerSource> sources) {
        for (ContainerSource src : sources) {
            ItemStack stack = src.container.getItem(src.slot);
            if (!stack.isEmpty() && stack.is(ModItems.PAINT_BUCKET)) {
                int maxDamage = stack.getMaxDamage();
                int currentDamage = stack.getDamageValue();
                if (maxDamage > 0 && currentDamage < maxDamage) {
                    if (currentDamage + 1 >= maxDamage) {
                        src.container.setItem(src.slot, new ItemStack(Items.BUCKET));
                        ModLog.debug("[BlueprintColorMaterialHelper] 油漆桶耐久耗尽，变为空铁桶 (slot=" + src.slot + ")");
                    } else {
                        stack.setDamageValue(currentDamage + 1);
                    }
                    src.markChanged();
                    return true;
                }
            }
        }
        return false;
    }
}
