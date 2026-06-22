package me.tuanzi.mixin;

import me.tuanzi.client.TuanzisModClient;
import me.tuanzi.init.ModEnchantments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Hud;
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

@Mixin(Hud.class)
public abstract class GuiMixin {

    @Shadow @Final private Minecraft minecraft;
    @Shadow public abstract boolean isHidden();

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void tuanzis_mod$onExtractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (this.minecraft.player == null) return;
        if (this.isHidden()) return;

        // 1. 连锁采掘激活提示
        if (TuanzisModClient.isHoldingChainMiningKey()) {
            LocalPlayer player = this.minecraft.player;
            ItemStack tool = player.getMainHandItem();
            if (!tool.isEmpty()) {
                var enchantmentRegistry = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                var chainMiningEnch = enchantmentRegistry.getOrThrow(ModEnchantments.CHAIN_MINING);
                int levelVal = EnchantmentHelper.getItemEnchantmentLevel(chainMiningEnch, tool);

                if (levelVal > 0) {
                    // 3. 渲染 HUD 提示
                    Font font = this.minecraft.font;
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

        // 2. 狂战士低血量暗红色血丝渲染提示
        LocalPlayer player = this.minecraft.player;
        if (player != null && !player.isSpectator()) {
            var enchantmentRegistry = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var berserkerEnch = enchantmentRegistry.getOrThrow(ModEnchantments.BERSERKER);
            ItemStack chest = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
            int level = EnchantmentHelper.getItemEnchantmentLevel(berserkerEnch, chest);
            
            if (level > 0) {
                float healthPercent = player.getHealth() / player.getMaxHealth();
                if (healthPercent < 0.30f) {
                    // 当血量低于30%时浮现血丝，低于5%时达到最大值
                    float t = (0.30f - healthPercent) / (0.30f - 0.05f);
                    t = net.minecraft.util.Mth.clamp(t, 0.0f, 1.0f);
                    
                    // 平滑插值后的最大透明度设为 0.70f，保持极佳的视觉效果而不过度遮挡视野
                    float alpha = t * 0.70f;
                    
                    // 完美应对 RenderPipelines.VIGNETTE 着色器通道映射反转机制：
                    // 传入的 red 参数控制绿蓝，传入的 green 和 blue 参数控制红色。
                    // 传入 (alpha, 0.0f, 0.5f, 0.5f) 可在屏幕上完美渲染出尊贵沉浸的暗红色血丝特效！
                    int color = net.minecraft.util.ARGB.colorFromFloat(alpha, 0.0f, 0.5f, 0.5f);
                    
                    // 使用专属的 VIGNETTE_LOCATION 进行全屏暗角材质渲染
                    graphics.blit(
                        net.minecraft.client.renderer.RenderPipelines.VIGNETTE,
                        net.minecraft.resources.Identifier.withDefaultNamespace("textures/misc/vignette.png"),
                        0, 0, 0.0F, 0.0F,
                        graphics.guiWidth(), graphics.guiHeight(),
                        graphics.guiWidth(), graphics.guiHeight(),
                        color
                    );
                }
            }
        }
    }
}
