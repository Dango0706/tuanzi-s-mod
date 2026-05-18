package me.tuanzi.mixin;

import me.tuanzi.init.ModEnchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import me.tuanzi.util.ModLog;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

@Mixin(Warden.class)
public abstract class WardenSoulboundDropMixin extends LivingEntity {
    @Unique
    private final Map<UUID, Float> tuanzis_mod$playerDamageMap = new HashMap<>();
    @Unique
    private boolean tuanzis_mod$isMarkedForSoulbound = false;

    protected WardenSoulboundDropMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "hurtServer", at = @At("TAIL"))
    private void tuanzis_mod$trackDamageForMarking(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        // 只有当伤害确实造成了（返回true）且攻击者是玩家时才记录
        if (cir.getReturnValue() && source.getEntity() instanceof Player player) {
            UUID uuid = player.getUUID();
            float totalDamage = tuanzis_mod$playerDamageMap.getOrDefault(uuid, 0f) + amount;
            tuanzis_mod$playerDamageMap.put(uuid, totalDamage);

            float damagePercentage = totalDamage / this.getMaxHealth();
            ModLog.debug(String.format("Player %s dealt %.2f damage to Warden (Total: %.2f%%)", 
                player.getScoreboardName(), amount, damagePercentage * 100));

            if (totalDamage >= this.getMaxHealth() * 0.5f) {
                if (!tuanzis_mod$isMarkedForSoulbound) {
                    tuanzis_mod$isMarkedForSoulbound = true;
                    this.setGlowingTag(true); // 使其发光
                    ModLog.info("Warden has been MARKED for Soulbound drop!");
                }
            }
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void tuanzis_mod$writeMarkedForSoulbound(ValueOutput output, CallbackInfo ci) {
        output.putBoolean("tuanzis_mod$isMarkedForSoulbound", tuanzis_mod$isMarkedForSoulbound);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void tuanzis_mod$readMarkedForSoulbound(ValueInput input, CallbackInfo ci) {
        tuanzis_mod$isMarkedForSoulbound = input.getBooleanOr("tuanzis_mod$isMarkedForSoulbound", false);
        // 如果加载时标记为 true，确保它在发光
        if (tuanzis_mod$isMarkedForSoulbound) {
            this.setGlowingTag(true);
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);

        if (tuanzis_mod$isMarkedForSoulbound && killedByPlayer && source.getEntity() instanceof Player player) {
            int lootingLevel = EnchantmentHelper.getEnchantmentLevel(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING), player);
            
            // 基础10% + 每级抢夺2.5%
            float chance = 0.10f + (lootingLevel * 0.025f);
            ModLog.debug(String.format("Warden death: Marked=%b, Looting=%d, Drop Chance=%.2f%%", 
                tuanzis_mod$isMarkedForSoulbound, lootingLevel, chance * 100));
            
            if (this.random.nextFloat() < chance) {
                var enchantmentRegistry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                var soulboundHolder = enchantmentRegistry.getOrThrow(ModEnchantments.SOULBOUND);

                ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
                EnchantmentHelper.updateEnchantments(book, mutable -> mutable.set(soulboundHolder, 1));
                
                ModLog.info("Lucky! Warden dropped a Soulbound enchanted book.");
                this.spawnAtLocation(level, book);
            }
        } else if (killedByPlayer) {
            ModLog.debug("Warden killed by player but was NOT marked for Soulbound drop.");
        }
    }
}
