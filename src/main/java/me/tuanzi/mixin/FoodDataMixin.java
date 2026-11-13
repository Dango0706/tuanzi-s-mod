package me.tuanzi.mixin;

import me.tuanzi.init.ModEnchantments;
import me.tuanzi.util.ModLog;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public abstract class FoodDataMixin {
    @Shadow
    private int foodLevel;
    @Shadow
    private float saturationLevel;
    @Shadow
    private float exhaustionLevel;
    @Shadow
    private int tickTimer;

    @Shadow
    public abstract void addExhaustion(float amount);

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$applyMetabolismRegen(ServerPlayer player, CallbackInfo ci) {
        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chestplate.isEmpty()) return;

        var enchRegistry = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var metabolismHolder = enchRegistry.getOrThrow(ModEnchantments.METABOLISM);
        int levelEnch = EnchantmentHelper.getItemEnchantmentLevel(metabolismHolder, chestplate);
        if (levelEnch <= 0) return;

        // 接管 tick 逻辑以应用代谢附魔的饥饿值门槛和缩短的恢复间隔
        ci.cancel();

        ServerLevel level = player.level();
        Difficulty difficulty = level.getDifficulty();
        if (this.exhaustionLevel > 4.0F) {
            this.exhaustionLevel -= 4.0F;
            if (this.saturationLevel > 0.0F) {
                this.saturationLevel = Math.max(this.saturationLevel - 1.0F, 0.0F);
            } else if (difficulty != Difficulty.PEACEFUL) {
                this.foodLevel = Math.max(this.foodLevel - 1, 0);
            }
        }

        boolean naturalRegen = level.getGameRules().get(GameRules.NATURAL_HEALTH_REGENERATION);
        if (naturalRegen && this.saturationLevel > 0.0F && player.isHurt() && this.foodLevel >= 20) {
            // 原版快速生命恢复（消耗饱和度）
            this.tickTimer++;
            if (this.tickTimer >= 10) {
                float saturationSpent = Math.min(this.saturationLevel, 6.0F);
                player.heal(saturationSpent / 6.0F);
                this.addExhaustion(saturationSpent);
                this.tickTimer = 0;
            }
        } else if (naturalRegen && this.foodLevel >= 14 && player.isHurt()) {
            // 代谢附魔生效：饥饿值 >= 14 即可触发自然慢速恢复，间隔由 80 ticks 缩短为 72/64/56 ticks (3.6/3.2/2.8s)
            this.tickTimer++;
            int threshold = 80 - 8 * levelEnch;
            if (this.tickTimer >= threshold) {
                player.heal(1.0F);
                this.addExhaustion(6.0F);
                this.tickTimer = 0;
                ModLog.debug(player, "【代谢】触发自然生命恢复 1 点 (生命: %.1f/%.1f, 饥饿值: %d, 等级: %d, 间隔: %d ticks)",
                        player.getHealth(), player.getMaxHealth(), this.foodLevel, levelEnch, threshold);
            }
        } else if (this.foodLevel <= 0) {
            // 原版饥饿扣血
            this.tickTimer++;
            if (this.tickTimer >= 80) {
                if (player.getHealth() > 10.0F || difficulty == Difficulty.HARD || player.getHealth() > 1.0F && difficulty == Difficulty.NORMAL) {
                    player.hurtServer(level, player.damageSources().starve(), 1.0F);
                }
                this.tickTimer = 0;
            }
        } else {
            this.tickTimer = 0;
        }
    }
}
