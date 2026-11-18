package fr.minevegas.casino.mixin;

import fr.minevegas.casino.DealerAppearance;
import fr.minevegas.casino.RouletteMod;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ShapedRecipe.class})
public abstract class DealerRecipeMixin {
   @Inject(
      method = {"assemble(Lnet/minecraft/world/item/crafting/CraftingInput;)Lnet/minecraft/world/item/ItemStack;"},
      at = {@At("RETURN")}
   )
   private void minevegas$dealer(CraftingInput input, CallbackInfoReturnable<ItemStack> callback) {
      int size = input.size();
      ItemStack result = callback.getReturnValue();
      if (result.is(RouletteMod.BLACKJACK_TABLE.asItem()) || result.is(RouletteMod.POKER_TABLE.asItem())) {
         for(int n = 0; n < size; ++n) {
            ItemStack ingredient = input.getItem(n);
            if (ingredient.is(RouletteMod.CROUPIER_BADGE) || ingredient.is(RouletteMod.BLACKJACK_TABLE.asItem())) {
               DealerAppearance appearance = DealerAppearance.from(ingredient);
               if (!appearance.isEmpty()) {
                  appearance.putOn(result);
                  return;
               }
            }
         }

      }
   }
}
