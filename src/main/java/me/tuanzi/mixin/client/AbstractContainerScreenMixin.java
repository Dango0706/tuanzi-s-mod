package me.tuanzi.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin<T extends AbstractContainerMenu> extends Screen {
    
    protected AbstractContainerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "extractSlot", at = @At("HEAD"))
    private void tuanzis_mod$drawRaritySlotBorder(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        // 判断当前界面的标题是否包含“抽卡历史记录”或“卡池预览”来实现对相应界面的精准识别
        if (this.title != null && (this.title.getString().contains("抽卡历史记录") || this.title.getString().contains("卡池预览"))) {
            // 只渲染 Slot 0-44 的抽卡历史物品格子 (排除下排功能翻页栏以及玩家背包)
            if (slot.index < 45 && slot.hasItem()) {
                ItemStack stack = slot.getItem();
                
                // 100% 稳定可靠的稀有度检测方法：优先遍历 Lore，再降级检查 CustomName
                String rarity = null;
                boolean isFateTarget = false;
                ItemLore itemLore = stack.get(DataComponents.LORE);
                if (itemLore != null) {
                    for (Component line : itemLore.lines()) {
                        String plainText = line.getString();
                        if (plainText.contains("Legendary")) {
                            rarity = "legendary";
                        } else if (plainText.contains("Epic")) {
                            rarity = "epic";
                        } else if (plainText.contains("Rare")) {
                            rarity = "rare";
                        } else if (plainText.contains("Uncommon")) {
                            rarity = "uncommon";
                        } else if (plainText.contains("Common")) {
                            rarity = "common";
                        }
                        
                        if (plainText.contains("当前定轨目标")) {
                            isFateTarget = true;
                        }
                    }
                }

                if (rarity == null) {
                    Component customName = stack.get(DataComponents.CUSTOM_NAME);
                    if (customName != null) {
                        String nameStr = customName.getString();
                        if (nameStr.contains("传说") || nameStr.contains("Legendary")) {
                            rarity = "legendary";
                        } else if (nameStr.contains("史诗") || nameStr.contains("Epic")) {
                            rarity = "epic";
                        } else if (nameStr.contains("稀有") || nameStr.contains("Rare")) {
                            rarity = "rare";
                        } else if (nameStr.contains("优秀") || nameStr.contains("Uncommon")) {
                            rarity = "uncommon";
                        } else if (nameStr.contains("普通") || nameStr.contains("Common")) {
                            rarity = "common";
                        }
                    }
                }

                if (rarity == null && isFateTarget) {
                    rarity = "common";
                }

                if (rarity != null) {
                    int bgColor = 0;
                    int borderColor = 0;

                    switch (rarity) {
                        case "legendary" -> {
                            bgColor = 0x40FFAA00;      // 传说: 25% 金黄色
                            borderColor = 0xB0FFAA00;  // 金色高亮描边
                        }
                        case "epic" -> {
                            bgColor = 0x40F53DF5;      // 史诗: 25% 紫粉色
                            borderColor = 0xB0F53DF5;  // 紫色高亮描边
                        }
                        case "rare" -> {
                            bgColor = 0x4000C8FF;      // 稀有: 25% 天蓝色
                            borderColor = 0xB000C8FF;  // 蓝色高亮描边
                        }
                        case "uncommon" -> {
                            bgColor = 0x4000FF66;      // 优秀: 25% 翠绿色
                            borderColor = 0xB000FF66;  // 绿色高亮描边
                        }
                        default -> {
                            bgColor = 0x20E0E0E0;      // 普通: 12% 灰白色
                            borderColor = 0x60E0E0E0;  // 灰色柔和描边
                        }
                    }

                    // 1. 绘制半透明背景填充
                    graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, bgColor);
                    
                    // 2. 绘制槽位外边缘 1 像素高亮描边
                    graphics.outline(slot.x - 1, slot.y - 1, 18, 18, borderColor);

                    // 3. 并在四个角使用红色的指向对应位置的对角线小箭头表示
                    if (isFateTarget) {
                        // ↖ 左上角箭头 (向左上方指)
                        graphics.fill(slot.x - 3, slot.y - 3, slot.x + 1, slot.y - 1, 0xFFFF0000);
                        graphics.fill(slot.x - 3, slot.y - 1, slot.x - 1, slot.y + 3, 0xFFFF0000);
                        graphics.fill(slot.x - 1, slot.y - 1, slot.x + 2, slot.y + 2, 0xFFFF0000);

                        // ↗ 右上角箭头 (向右上方指)
                        graphics.fill(slot.x + 15, slot.y - 3, slot.x + 19, slot.y - 1, 0xFFFF0000);
                        graphics.fill(slot.x + 17, slot.y - 1, slot.x + 19, slot.y + 3, 0xFFFF0000);
                        graphics.fill(slot.x + 14, slot.y - 1, slot.x + 17, slot.y + 2, 0xFFFF0000);

                        // ↙ 左下角箭头 (向左下方指)
                        graphics.fill(slot.x - 3, slot.y + 17, slot.x + 1, slot.y + 19, 0xFFFF0000);
                        graphics.fill(slot.x - 3, slot.y + 13, slot.x - 1, slot.y + 17, 0xFFFF0000);
                        graphics.fill(slot.x - 1, slot.y + 14, slot.x + 2, slot.y + 17, 0xFFFF0000);

                        // ↘ 右下角箭头 (向右下方指)
                        graphics.fill(slot.x + 15, slot.y + 17, slot.x + 19, slot.y + 19, 0xFFFF0000);
                        graphics.fill(slot.x + 17, slot.y + 13, slot.x + 19, slot.y + 17, 0xFFFF0000);
                        graphics.fill(slot.x + 14, slot.y + 14, slot.x + 17, slot.y + 17, 0xFFFF0000);
                    }
                }
            }
        }
    }
}
