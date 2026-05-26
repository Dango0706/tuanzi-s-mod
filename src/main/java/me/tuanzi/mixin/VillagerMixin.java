package me.tuanzi.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Villager.class)
public class VillagerMixin {

    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void onMobInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack stack = player.getItemInHand(hand);
        // 判定手持的是否为缚灵笼，且该缚灵笼尚未装有村民
        if (stack.is(me.tuanzi.init.ModItems.VILLAGER_CAGE) && !me.tuanzi.item.VillagerCageItem.isVillagerStored(stack)) {
            // 直接调用缚灵笼的 interactLivingEntity 并截获交互
            InteractionResult result = stack.interactLivingEntity(player, (Villager) (Object) this, hand);
            if (result.consumesAction()) {
                cir.setReturnValue(result);
            }
        }
    }
}
