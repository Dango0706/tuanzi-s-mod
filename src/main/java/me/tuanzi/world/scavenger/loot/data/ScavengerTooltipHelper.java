package me.tuanzi.world.scavenger.loot.data;

import me.tuanzi.world.scavenger.loot.meta.EquipmentGridSpec;
import me.tuanzi.world.scavenger.loot.meta.ScavengerItemMeta;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 拾荒回廊物品品质、占地尺寸、重量、标定价值与扩容属性 Tooltip 构建工具（无客户端类依赖，纯双端安全）
 */
public final class ScavengerTooltipHelper {

    private ScavengerTooltipHelper() {
    }

    /**
     * 将拾荒回廊属性追加到给定的 Tooltip 行列表中。
     */
    public static void appendTooltipLines(ItemStack stack, boolean inScavengerCorridor, List<Component> lines) {
        List<Component> extra = buildTooltipLines(stack, inScavengerCorridor);
        if (!extra.isEmpty()) {
            lines.addAll(extra);
        }
    }

    /**
     * 构建拾荒回廊属性 Tooltip 行列表（纯函数，可在无渲染客户端的 JUnit 测试中直接验证）：
     * - 若玩家处于拾荒回廊维度：对所有非空物品展示完整搜刮属性（未注册物品展示 1x1 兜底属性）。
     * - 若玩家不在拾荒回廊维度：仅对显式注册在 scavenger_items 数据包中的专属物品展示属性卡片，不污染主世界原版物品。
     */
    public static List<Component> buildTooltipLines(ItemStack stack, boolean inScavengerCorridor) {
        if (stack == null || stack.isEmpty()) {
            return List.of();
        }
        Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Optional<ScavengerItemMeta> registeredOpt = ScavengerDataRegistry.getRegisteredItemMeta(itemId);
        if (!inScavengerCorridor && registeredOpt.isEmpty()) {
            return List.of();
        }

        ScavengerItemMeta meta = registeredOpt.orElseGet(() -> ScavengerItemMeta.fallback(itemId));
        List<Component> result = new ArrayList<>(5);

        // 1. 品质与类别徽章行：[稀有度 · 类别]
        MutableComponent rarityPart = Component.translatable(meta.rarity().translationKey())
                .withStyle(Style.EMPTY.withColor(meta.rarity().colorRgb()).withBold(true));
        MutableComponent categoryPart = Component.translatable(meta.category().translationKey())
                .withStyle(ChatFormatting.GRAY);
        result.add(Component.literal("[").withStyle(ChatFormatting.DARK_GRAY)
                .append(rarityPart)
                .append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
                .append(categoryPart)
                .append(Component.literal("]").withStyle(ChatFormatting.DARK_GRAY)));

        // 2. 网格占地与单件重量行
        String sizeText = meta.width() + "×" + meta.height();
        String weightText = String.format(Locale.ROOT, "%.2f kg", meta.weightKg());
        result.add(Component.translatable("tooltip.tuanzis_mod.scavenger.size_and_weight", sizeText, weightText)
                .withStyle(ChatFormatting.AQUA));

        // 3. 标定基础价值行（恒等于 baseValue）
        if (meta.baseValue() > 0 || registeredOpt.isPresent()) {
            String valueText = String.format(Locale.ROOT, "%,d", meta.baseValue());
            result.add(Component.translatable("tooltip.tuanzis_mod.scavenger.calibrated_value", valueText)
                    .withStyle(ChatFormatting.GOLD));
        }

        // 4. 若为战术背包或安全箱，展示内部网格扩容规格
        if (meta.equipmentGrid().isPresent()) {
            EquipmentGridSpec spec = meta.equipmentGrid().get();
            String gridCap = spec.columns() + "×" + spec.rows() + " (" + spec.totalCells() + ")";
            String key = spec.slotType() == EquipmentGridSpec.SlotType.SAFEBOX
                    ? "tooltip.tuanzis_mod.scavenger.safebox_capacity"
                    : "tooltip.tuanzis_mod.scavenger.backpack_capacity";
            ChatFormatting color = spec.slotType() == EquipmentGridSpec.SlotType.SAFEBOX
                    ? ChatFormatting.LIGHT_PURPLE
                    : ChatFormatting.GREEN;
            result.add(Component.translatable(key, gridCap).withStyle(color));
        }

        // 5. 可选专属背景描述 Lore
        meta.loreTranslationKey().ifPresent(loreKey ->
                result.add(Component.translatable(loreKey).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC))
        );

        return result;
    }
}
