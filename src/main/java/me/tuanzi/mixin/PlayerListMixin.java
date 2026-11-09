package me.tuanzi.mixin;

import me.tuanzi.init.ModDimensions;
import me.tuanzi.util.ModLog;
import me.tuanzi.world.scavenger.team.ScavengerTeamManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

    @ModifyVariable(method = "respawn", at = @At(value = "STORE"), ordinal = 0)
    private TeleportTransition tuanzis_mod$overrideCorridorRespawn(TeleportTransition respawnInfo, ServerPlayer serverPlayer) {
        if (serverPlayer.level().dimension() == ModDimensions.SCAVENGER_CORRIDOR_LEVEL) {
            ServerLevel corridorLevel = serverPlayer.level().getServer().getLevel(ModDimensions.SCAVENGER_CORRIDOR_LEVEL);
            if (corridorLevel != null) {
                net.minecraft.core.BlockPos spawnPos = me.tuanzi.world.scavenger.CorridorSpawnManager.get(corridorLevel).getOrCreateSpawnPos(serverPlayer, corridorLevel);
                ModLog.debug(serverPlayer, null, "【拾荒回廊】拦截玩家复活：强制定位至专属出生点 (" +
                    (spawnPos.getX() + 0.5) + ", " + (spawnPos.getY() + 1.0) + ", " + (spawnPos.getZ() + 0.5) + ") 安全基台。");
                return new TeleportTransition(
                    corridorLevel,
                    new Vec3(spawnPos.getX() + 0.5, spawnPos.getY() + 1.0, spawnPos.getZ() + 0.5),
                    Vec3.ZERO,
                    0.0f,
                    0.0f,
                    TeleportTransition.DO_NOTHING
                );
            }
        }
        return respawnInfo;
    }

    @Inject(method = "respawn", at = @At("RETURN"))
    private void tuanzis_mod$enforceCorridorAdventureOnRespawn(ServerPlayer serverPlayer, boolean keepAllPlayerData, Entity.RemovalReason removalReason, CallbackInfoReturnable<ServerPlayer> cir) {
        ServerPlayer newPlayer = cir.getReturnValue();
        if (newPlayer != null && newPlayer.level().dimension() == ModDimensions.SCAVENGER_CORRIDOR_LEVEL) {
            newPlayer.setGameMode(GameType.ADVENTURE);
            me.tuanzi.world.scavenger.ScavengerManager.syncCorridorAccessoriesToClient(newPlayer);
            me.tuanzi.network.scavenger.ScavengerServerPacketHandler.syncProfile(newPlayer);
            ModLog.debug(newPlayer, null, "【拾荒回廊】玩家在回廊复活完成，强制保持冒险模式并同步回廊专属饰品。");
        }
    }

    @Inject(method = "placeNewPlayer", at = @At("RETURN"))
    private void tuanzis_mod$enforceCorridorAdventureOnJoin(net.minecraft.network.Connection connection, ServerPlayer player, net.minecraft.server.network.CommonListenerCookie cookie, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (player.level().dimension() == ModDimensions.SCAVENGER_CORRIDOR_LEVEL) {
            player.setGameMode(GameType.ADVENTURE);
            ServerLevel corridorLevel = (ServerLevel) player.level();
            ModDimensions.ensureBasePlatform(corridorLevel);
            me.tuanzi.world.scavenger.CorridorSpawnManager.get(corridorLevel).getOrCreateSpawnPos(player, corridorLevel);
            if (player instanceof me.tuanzi.world.scavenger.ScavengerPlayerTracker tracker) {
                tracker.tuanzis_mod$getCorridorProfile().tryGrantStarterSafeboxIfEligible(player.registryAccess());
                tracker.tuanzis_mod$getCorridorProfile().syncEquipmentAndStashDimensionsWithConfig();
            }
            me.tuanzi.world.scavenger.ScavengerManager.syncCorridorAccessoriesToClient(player);
            me.tuanzi.network.scavenger.ScavengerServerPacketHandler.syncProfile(player);
            ModLog.debug(player, null, "【拾荒回廊】玩家登录于拾荒回廊，确保基台完备并强制维持冒险模式。");
        }
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void tuanzis_mod$onPlayerDisconnect(ServerPlayer player, CallbackInfo ci) {
        ScavengerTeamManager.onPlayerLeaveCorridorOrDisconnect(player);
    }
}
