package me.tuanzi.mixin;

import me.tuanzi.init.ModDimensions;
import me.tuanzi.util.ModLog;
import me.tuanzi.world.scavenger.ScavengerPlayerTracker;
import me.tuanzi.world.scavenger.profile.PlayerCorridorProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Player.class)
public abstract class ScavengerDeathDropMixin {

    @Inject(method = "dropEquipment", at = @At("TAIL"))
    private void tuanzis_mod$forceDropInCorridor(ServerLevel level, CallbackInfo ci) {
        if (level.dimension() == ModDimensions.SCAVENGER_CORRIDOR_LEVEL) {
            Player player = (Player) (Object) this;
            if (level.getGameRules().get(GameRules.KEEP_INVENTORY)) {
                player.getInventory().dropAll();
                ModLog.debug(player, null, "【拾荒回廊】强制死亡掉落触发：即使服务器开启 keepInventory，回廊内仍强制爆出快捷栏与护甲装备。");
            }

            // 拆解战术背包并散落内部全部战利品与空背包本体（安全箱及其内部物品、基地仓库与成长等级 100% 保留）
            if (player instanceof ScavengerPlayerTracker tracker) {
                PlayerCorridorProfile profile = tracker.tuanzis_mod$getCorridorProfile();
                List<ItemStack> backpackDrops = profile.unpackAndClearBackpackForDeath(player.registryAccess());
                for (ItemStack dropStack : backpackDrops) {
                    if (!dropStack.isEmpty()) {
                        ItemEntity entity = player.createItemStackToDrop(dropStack, true, false);
                        if (entity != null) {
                            level.addFreshEntity(entity);
                        }
                    }
                }
            }
        }
    }
}
