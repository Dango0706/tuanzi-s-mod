package me.tuanzi.mixin.client;

import me.tuanzi.client.update.UpdateChecker;
import me.tuanzi.client.update.UpdateConfirmScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void onInit(CallbackInfo ci) {
        // 如果检测到新版本，并且没有忽略，那么自动弹出更新界面
        if (UpdateChecker.shouldNotify()) {
            UpdateChecker.setAlreadyNotified(true);
            Minecraft.getInstance().setScreen(new UpdateConfirmScreen((TitleScreen) (Object) this));
        }
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void onExtractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        // 如果更新失败，且在加载游戏后的主界面，提示获取失败并在 10 秒后自动消失
        if (UpdateChecker.getStatus() == UpdateChecker.Status.FAILED && !UpdateChecker.isHasShownFailedNotification()) {
            if (UpdateChecker.getFailedNotificationStartTime() == -1L) {
                UpdateChecker.setFailedNotificationStartTime(System.currentTimeMillis());
            }
            long elapsed = System.currentTimeMillis() - UpdateChecker.getFailedNotificationStartTime();
            if (elapsed < 10000) {
                // 平滑渐入与渐出过渡动画 (前 500ms 渐入，最后 500ms 渐出)
                float alpha = 1.0f;
                if (elapsed < 500) {
                    alpha = elapsed / 500.0f;
                } else if (elapsed > 9500) {
                    alpha = (10000 - elapsed) / 500.0f;
                }
                int alphaInt = Math.max(0, Math.min(255, (int) (alpha * 255)));

                String text = "获取模组更新失败 (" + UpdateChecker.getErrorMessage() + ")";
                int textWidth = this.font.width(text);
                int x = this.width - textWidth - 10;
                int y = 10;

                // 绘制极富质感的半透明深红警告底色背景
                graphics.fill(x - 5, y - 3, x + textWidth + 5, y + 11, ARGB.color(Math.max(0, Math.min(alphaInt, 150)), 50, 10, 10));
                // 绘制优雅的浅红警告字
                graphics.text(this.font, text, x, y, ARGB.color(alphaInt, 255, 80, 80));
            } else {
                UpdateChecker.setHasShownFailedNotification(true);
            }
        }
    }
}
