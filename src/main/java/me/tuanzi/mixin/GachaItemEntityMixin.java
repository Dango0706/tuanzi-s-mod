package me.tuanzi.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.UUID;
import me.tuanzi.util.ModLog;

@Mixin(ItemEntity.class)
public abstract class GachaItemEntityMixin {
    @Shadow public abstract void setTarget(UUID uuid);

    @Inject(method = "tick", at = @At("HEAD"))
    private void tuanzis_mod$onGachaItemTick(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (!self.level().isClientSide() && self.entityTags().contains("tuanzis_gacha_drop")) {
            // 30秒 = 30 * 20 = 600 ticks
            if (self.tickCount >= 600) {
                this.setTarget(null);
                self.removeTag("tuanzis_gacha_drop");
                ModLog.debug("[抽卡系统] 抽卡掉落物 " + self.getItem().getHoverName().getString() + " 已在世界中保留满 30 秒本人未拾取，已自动清空专属锁定，开放给所有玩家领取！");
            }
        }
    }
}
