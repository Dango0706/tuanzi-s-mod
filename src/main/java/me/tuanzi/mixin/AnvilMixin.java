package me.tuanzi.mixin;

import me.tuanzi.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.Color;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(AnvilMenu.class)
public abstract class AnvilMixin {
    @Shadow @Final private DataSlot cost;
    @Shadow private String itemName;

    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("#[0-9a-fA-F]{6}");
    private static final Pattern FORMAT_PATTERN = Pattern.compile("&[0-9a-fk-o]");
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("&q(\\d+)(#[0-9a-fA-F]{6})(?:-(#[0-9a-fA-F]{6}))?");
    private final Random random = new Random();

    @ModifyConstant(method = "createResult", constant = @org.spongepowered.asm.mixin.injection.Constant(intValue = 40))
    private int tuanzis_mod$removeLevelLimit(int constant) {
        return Integer.MAX_VALUE;
    }

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$alwaysMayPickup(Player player, boolean present, CallbackInfoReturnable<Boolean> cir) {
        if (!player.getAbilities().instabuild) {
            cir.setReturnValue(player.experienceLevel >= this.cost.get());
        }
    }

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$handleColorSpongeCustom(CallbackInfo ci) {
        AnvilMenu menu = (AnvilMenu) (Object) this;
        ItemStack left = menu.getSlot(0).getItem();
        ItemStack right = menu.getSlot(1).getItem();

        // 场景1：仅重命名颜色海绵
        if (left.is(ModItems.COLOR_SPONGE) && right.isEmpty()) {
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
                
                menu.getSlot(2).set(result);
                ci.cancel();
            }
        }
        
        // 场景2：给物品应用样式 (此处逻辑保持不变)
        if (!left.isEmpty() && right.is(ModItems.COLOR_SPONGE)) {
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
                    menu.getSlot(2).set(result);
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
                    menu.getSlot(2).set(result);
                    ci.cancel();
                    return;
                }

                // 3. 普通样式
                Style style = parseStyle(spongeText);
                if (style != Style.EMPTY || spongeText.contains("&r")) {
                    finalComp.append(Component.literal(itemText).withStyle(style));
                    result.set(DataComponents.CUSTOM_NAME, finalComp);
                    this.cost.set(calculateMergingCost(spongeText));
                    menu.getSlot(2).set(result);
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
