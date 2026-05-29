package me.tuanzi.mixin;

import me.tuanzi.init.ModPotions;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PotionBrewing.class)
public abstract class BrewingRecipeMixin {
    /**
     * 在 1.21.2 中，addVanillaMixes 是 PotionBrewing 类中的一个静态方法。
     * 它接收一个 PotionBrewing.Builder 参数。
     */
    @Inject(method = "addVanillaMixes", at = @At("TAIL"))
    private static void tuanzis_mod$addBrewingRecipes(PotionBrewing.Builder builder, CallbackInfo ci) {
        builder.addMix(Potions.MUNDANE, Items.TOTEM_OF_UNDYING, ModPotions.UNDYING_POTION);
        builder.addMix(ModPotions.UNDYING_POTION, Items.REDSTONE, ModPotions.LONG_UNDYING_POTION);
        
        // 肾上腺素药水酿造配方
        builder.addMix(Potions.WEAKNESS, Items.GOLDEN_CARROT, ModPotions.ADRENALINE_POTION);
        builder.addMix(ModPotions.ADRENALINE_POTION, Items.GLOWSTONE_DUST, ModPotions.ADRENALINE_POTION_II);
        builder.addMix(ModPotions.ADRENALINE_POTION, Items.REDSTONE, ModPotions.LONG_ADRENALINE_POTION);
    }
}
