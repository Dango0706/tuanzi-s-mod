package me.tuanzi.mixin;

import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

@Mixin(Villager.class)
public interface VillagerAccessor {
    @Accessor("lastTradedPlayer")
    Player getLastTradedPlayer();

    @Accessor("lastTradedPlayer")
    void setLastTradedPlayer(Player player);

    @Invoker("increaseMerchantCareer")
    void invokeIncreaseMerchantCareer(ServerLevel level);

    @Invoker("updateSpecialPrices")
    void invokeUpdateSpecialPrices(Player player);
}
