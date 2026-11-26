package me.tuanzi.mixin;

import me.tuanzi.entity.StationVillager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.behavior.YieldJobSite;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(YieldJobSite.class)
public class YieldJobSiteMixin {

    @Inject(method = "nearbyWantsJobsite", at = @At("HEAD"), cancellable = true)
    private static void tuanzis_mod$ignoreStationOrNoAiVillager(
            Holder<PoiType> type,
            Villager nearbyVillager,
            BlockPos poiPos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (nearbyVillager instanceof StationVillager || nearbyVillager.isNoAi()) {
            cir.setReturnValue(false);
        }
    }
}
