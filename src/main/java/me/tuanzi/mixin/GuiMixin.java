package me.tuanzi.mixin;

import me.tuanzi.client.TuanzisModClient;
import me.tuanzi.init.ModEnchantments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {

    @Shadow @Final private Minecraft minecraft;

    @Shadow public abstract Font getFont();

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void tuanzis_mod$onExtractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (this.minecraft.player == null) return;
        if (this.minecraft.options.hideGui) return;

        // 1. 判断是否正在按住连锁采掘键
        if (!TuanzisModClient.isHoldingChainMiningKey()) return;

        // 2. 检查主手工具是否附魔了连锁采掘
        LocalPlayer player = this.minecraft.player;
        ItemStack tool = player.getMainHandItem();
        if (tool.isEmpty()) return;

        var enchantmentRegistry = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var chainMiningEnch = enchantmentRegistry.getOrThrow(ModEnchantments.CHAIN_MINING);
        int levelVal = EnchantmentHelper.getItemEnchantmentLevel(chainMiningEnch, tool);

        if (levelVal > 0) {
            // 3. 渲染 HUD 提示
            Font font = this.getFont();
            Component text = Component.translatable("hud.tuanzis_mod.chain_mining.active");
            int width = font.width(text);
            int height = 9;

            int left = 2;
            int top = 2;

            // 渲染类似 F3 界面效果的灰色半透明背景
            graphics.fill(left - 1, top - 1, left + width + 1, top + height - 1, -1873784752);
            // 渲染白色/浅灰色提示文字，无阴影
            graphics.text(font, text, left, top, -2039584, false);
        }
    }
}
