package me.tuanzi.mixin;

import me.tuanzi.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.*;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(AnvilMenu.class)
public abstract class AnvilMixin extends ItemCombinerMenu {
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("#[0-9a-fA-F]{6}");
    private static final Pattern FORMAT_PATTERN = Pattern.compile("&[0-9a-fk-o]");
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("&q(\\d+)(#[0-9a-fA-F]{6})(?:-(#[0-9a-fA-F]{6}))?");
    private final Random random = new Random();
    @Shadow
    @Final
    private DataSlot cost;
    @Shadow
    private String itemName;
    @Shadow
    private int repairItemCountCost;
    public AnvilMixin(MenuType<?> type, int syncId, Inventory playerInventory, ContainerLevelAccess access, ItemCombinerMenuSlotDefinition slotDefinition) {
        super(type, syncId, playerInventory, access, slotDefinition);
    }

    /**
     * 移除服务端铁砧名称验证的 50 字符限制。
     * 在 1.21.2+ 中，validateName 负责截断字符串。
     */
    @Inject(method = "validateName", at = @At("HEAD"), cancellable = true)
    private static void tuanzis_mod$removeNameValidationLimit(String string, CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(string);
    }

    @ModifyConstant(method = "createResult", constant = @org.spongepowered.asm.mixin.injection.Constant(intValue = 40))
    private int tuanzis_mod$removeLevelLimit(int constant) {
        return Integer.MAX_VALUE;
    }

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$alwaysMayPickup(Player player, boolean present, CallbackInfoReturnable<Boolean> cir) {
        ItemStack result = this.resultSlots.getItem(0);
        if (!result.isEmpty()) {
            net.minecraft.world.item.component.CustomData customData = result.get(DataComponents.CUSTOM_DATA);
            if (customData != null) {
                net.minecraft.nbt.CompoundTag tag = customData.copyTag();
                if (tag.getBooleanOr("ink_incompatible", false)) {
                    cir.setReturnValue(false);
                    return;
                }
            }
        }
        if (!player.getAbilities().instabuild) {
            cir.setReturnValue(player.experienceLevel >= this.cost.get());
        }
    }

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$handleColorSpongeCustom(CallbackInfo ci) {
        AnvilMenu menu = (AnvilMenu) (Object) this;
        // 采用原生的 inputSlots 影子字段获取物品，解决受保护访问权限问题
        ItemStack left = this.inputSlots.getItem(0);
        ItemStack right = this.inputSlots.getItem(1);

        // 4. 墨水不相容提示：左侧为非塑世之笔，右侧为虚空墨锭，输出槽显示红石粉“墨不相容”红字提示且不可拾取
        if (!left.isEmpty() && !left.is(ModItems.WORLD_SCULPTORS_PEN) && !right.isEmpty() && right.is(ModItems.VOID_INK_INGOT)) {
            ItemStack incompatibleTip = new ItemStack(net.minecraft.world.item.Items.REDSTONE);
            incompatibleTip.set(DataComponents.CUSTOM_NAME, Component.translatable("tooltip.tuanzis_mod.void_ink_ingot.incompatible").withStyle(ChatFormatting.RED));
            
            net.minecraft.world.item.component.CustomData customData = net.minecraft.world.item.component.CustomData.of(new net.minecraft.nbt.CompoundTag());
            net.minecraft.nbt.CompoundTag tag = customData.copyTag();
            tag.putBoolean("ink_incompatible", true);
            incompatibleTip.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));

            this.resultSlots.setItem(0, incompatibleTip);
            this.cost.set(0);
            this.repairItemCountCost = 0;
            menu.broadcastChanges();
            ci.cancel();
            return;
        }

        // 输出逻辑调试日志，方便在控制台排查未成功命中配方的真正条件原因
        if (!left.isEmpty() && left.is(ModItems.WORLD_SCULPTORS_PEN)) {
            me.tuanzi.util.ModLog.debug("AnvilMenu", "createResult", "[塑世之笔铁砧] 触发 createResult! 左侧: " + left + ", 右侧: " + right + ", 损坏耐久值: " + left.getDamageValue() + ", 是否为虚空墨锭: " + right.is(ModItems.VOID_INK_INGOT));
        }

        // 塑世之笔特殊修复与防附魔限制
        if (!left.isEmpty() && left.is(ModItems.WORLD_SCULPTORS_PEN)) {
            // 1. 防附魔：拒绝跟附魔书或任何带有附魔的物品合并（排除合法的修复物虚空墨锭以及彩色海绵，防止其因默认存在或附带的其它特殊组件被误判为附魔物品）
            if (!right.is(ModItems.VOID_INK_INGOT)
                    && !right.is(ModItems.RAINBOW_SPONGE)
                    && (right.is(net.minecraft.world.item.Items.ENCHANTED_BOOK) || !EnchantmentHelper.getEnchantmentsForCrafting(right).isEmpty())) {
                me.tuanzi.util.ModLog.debug("AnvilMenu", "createResult", "[塑世之笔铁砧] 检测到附魔行为，强制清空输出槽。");
                this.resultSlots.setItem(0, ItemStack.EMPTY);
                this.cost.set(0);
                menu.broadcastChanges();
                ci.cancel();
                return;
            }

            // 2. 修复：仅能使用虚空墨锭进行定额 1000 耐久修复
            if (right.is(ModItems.VOID_INK_INGOT) && left.getDamageValue() > 0) {
                int damage = left.getDamageValue();
                int neededIngots = (int) Math.ceil(damage / 1000.0);
                int usedIngots = Math.min(right.getCount(), neededIngots);

                ItemStack result = left.copy();
                result.setDamageValue(Math.max(0, damage - usedIngots * 1000));

                this.cost.set(usedIngots * 2); // 每次修复消耗虚空墨锭数 * 2 的经验等级
                this.repairItemCountCost = usedIngots;

                me.tuanzi.util.ModLog.debug("AnvilMenu", "createResult", "[塑世之笔铁砧] 成功匹配虚空墨锭修复！需要墨锭数: " + neededIngots + ", 实际使用墨锭数: " + usedIngots + ", 修复后耐久损伤: " + result.getDamageValue() + ", 经验消耗: " + (usedIngots * 2));

                this.resultSlots.setItem(0, result);
                menu.broadcastChanges();
                ci.cancel();
                return;
            }

            // 3. 安全屏蔽：拒绝其它任何合并逻辑（改名除外，当右侧为空时可改名）
            if (!right.isEmpty() && !right.is(ModItems.VOID_INK_INGOT) && !right.is(ModItems.RAINBOW_SPONGE)) {
                me.tuanzi.util.ModLog.debug("AnvilMenu", "createResult", "[塑世之笔铁砧] 尝试与其它无用物品合并，进行合成阻断屏蔽。");
                this.resultSlots.setItem(0, ItemStack.EMPTY);
                this.cost.set(0);
                menu.broadcastChanges();
                ci.cancel();
                return;
            }
        }

        // 场景1：仅重命名彩虹海绵
        if (left.is(ModItems.RAINBOW_SPONGE) && right.isEmpty()) {
            if (this.itemName != null && !this.itemName.isBlank()) {
                ItemStack result = left.copy();

                // 检查是否为渐变色指令
                Matcher gradM = GRADIENT_PATTERN.matcher(this.itemName);
                if (gradM.find()) {
                    // 仅渐变色不改变海绵名字颜色（保持纯文本预览）
                    result.set(DataComponents.CUSTOM_NAME, Component.literal(this.itemName));
                    this.cost.set(calculateRenamingCost(this.itemName));
                } else {
                    // 其他写法均改变海绵颜色名字作为示范
                    if (this.itemName.contains(",")) {
                        // 分割系统预览：将海绵名字按逗号分段染色
                        String[] parts = this.itemName.split(",");
                        MutableComponent preview = Component.empty();
                        for (int i = 0; i < parts.length; i++) {
                            if (i > 0) preview.append(Component.literal(",").withStyle(ChatFormatting.GRAY));
                            preview.append(Component.literal(parts[i].trim()).withStyle(parseStyle(parts[i].trim())));
                        }
                        result.set(DataComponents.CUSTOM_NAME, preview);
                    } else {
                        // 普通样式或十六进制预览
                        result.set(DataComponents.CUSTOM_NAME, Component.literal(this.itemName).withStyle(parseStyle(this.itemName)));
                    }
                    this.cost.set(calculateRenamingCost(this.itemName));
                }

                this.resultSlots.setItem(0, result);
                menu.broadcastChanges();
                ci.cancel();
            }
        }

        // 场景2：给物品应用样式 (此处逻辑保持不变)
        if (!left.isEmpty() && right.is(ModItems.RAINBOW_SPONGE)) {
            Component spongeNameComp = right.get(DataComponents.CUSTOM_NAME);
            if (spongeNameComp != null) {
                String spongeText = spongeNameComp.getString();
                ItemStack result = left.copy();
                String itemText = result.getHoverName().getString();
                MutableComponent finalComp = Component.empty();

                // 1. 渐变色
                Matcher gradM = GRADIENT_PATTERN.matcher(spongeText);
                if (gradM.find()) {
                    int n = Integer.parseInt(gradM.group(1));
                    TextColor startColor = TextColor.parseColor(gradM.group(2)).result().orElse(TextColor.fromRgb(0xFFFFFF));
                    TextColor endColor = gradM.group(3) != null ?
                            TextColor.parseColor(gradM.group(3)).result().orElse(TextColor.fromRgb(0xFFFFFF)) :
                            generateRandomSimilarColor(startColor);

                    for (int i = 0; i < itemText.length(); i++) {
                        if (i < n) {
                            TextColor color = interpolate(startColor, endColor, i, n);
                            finalComp.append(Component.literal(String.valueOf(itemText.charAt(i))).withStyle(Style.EMPTY.withColor(color)));
                        } else {
                            finalComp.append(Component.literal(String.valueOf(itemText.charAt(i))));
                        }
                    }
                    result.set(DataComponents.CUSTOM_NAME, finalComp);
                    this.cost.set(n * 32);
                    this.resultSlots.setItem(0, result);
                    menu.broadcastChanges();
                    ci.cancel();
                    return;
                }

                // 2. 分割系统
                if (spongeText.contains(",")) {
                    String[] styleParts = spongeText.split(",");
                    for (int i = 0; i < itemText.length(); i++) {
                        String part = styleParts[i % styleParts.length].trim();
                        finalComp.append(Component.literal(String.valueOf(itemText.charAt(i))).withStyle(parseStyle(part)));
                    }
                    result.set(DataComponents.CUSTOM_NAME, finalComp);
                    this.cost.set(calculateMergingCost(spongeText));
                    this.resultSlots.setItem(0, result);
                    menu.broadcastChanges();
                    ci.cancel();
                    return;
                }

                // 3. 普通样式
                Style style = parseStyle(spongeText);
                if (style != Style.EMPTY || spongeText.contains("&r")) {
                    finalComp.append(Component.literal(itemText).withStyle(style));
                    result.set(DataComponents.CUSTOM_NAME, finalComp);
                    this.cost.set(calculateMergingCost(spongeText));
                    this.resultSlots.setItem(0, result);
                    menu.broadcastChanges();
                    ci.cancel();
                }
            }
        }
    }

    private TextColor interpolate(TextColor start, TextColor end, int step, int total) {
        if (total <= 1) return start;
        float ratio = (float) step / (total - 1);
        float jitter = (random.nextFloat() * 0.1f - 0.05f);
        ratio = Math.clamp(ratio + jitter, 0.0f, 1.0f);
        Color c1 = new Color(start.getValue());
        Color c2 = new Color(end.getValue());
        return TextColor.fromRgb(new Color(
                (int) (c1.getRed() + ratio * (c2.getRed() - c1.getRed())),
                (int) (c1.getGreen() + ratio * (c2.getGreen() - c1.getGreen())),
                (int) (c1.getBlue() + ratio * (c2.getBlue() - c1.getBlue()))
        ).getRGB());
    }

    private TextColor generateRandomSimilarColor(TextColor base) {
        Color c = new Color(base.getValue());
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        float newHue = (hsb[0] + (random.nextFloat() * 0.2f - 0.1f)) % 1.0f;
        return TextColor.fromRgb(Color.HSBtoRGB(newHue, 0.8f, 0.9f));
    }

    private int calculateRenamingCost(String text) {
        Matcher m = GRADIENT_PATTERN.matcher(text);
        if (m.find()) return Integer.parseInt(m.group(1)) * 8;
        int commas = 0;
        for (char c : text.toCharArray()) if (c == ',') commas++;
        int styles = 0;
        Matcher sm = FORMAT_PATTERN.matcher(text);
        while (sm.find()) styles++;
        boolean hasHex = HEX_COLOR_PATTERN.matcher(text).find();
        return Math.max(1, (hasHex ? 8 : 0) + (styles * 4) + (commas * 2));
    }

    private int calculateMergingCost(String text) {
        int commas = 0;
        for (char c : text.toCharArray()) if (c == ',') commas++;
        int styles = 0;
        Matcher sm = FORMAT_PATTERN.matcher(text);
        while (sm.find()) styles++;
        boolean hasHex = HEX_COLOR_PATTERN.matcher(text).find();
        int costValue = (hasHex ? 32 : 0) + (styles * 16) + (commas * 8);
        return (costValue == 0 && text.contains("&r")) ? 16 : costValue;
    }

    private Style parseStyle(String text) {
        Style style = Style.EMPTY;
        Matcher hexM = HEX_COLOR_PATTERN.matcher(text);
        if (hexM.find()) {
            var color = TextColor.parseColor(hexM.group()).result();
            if (color.isPresent()) style = style.withColor(color.get());
        }
        Matcher fmtM = FORMAT_PATTERN.matcher(text);
        while (fmtM.find()) {
            char code = fmtM.group().charAt(1);
            ChatFormatting f = ChatFormatting.getByCode(code);
            if (f != null && f != ChatFormatting.RESET) style = style.applyFormat(f);
        }
        return style;
    }
}
