package me.tuanzi.world.scavenger.team;

import me.tuanzi.init.ModDimensions;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * 拾荒回廊小队碰撞检测辅助类（双端兼容）
 */
public class ScavengerTeamCollisionHelper {

    /**
     * 判断两个实体是否为拾荒回廊中的同队队友（互相无碰撞箱、不推挤）
     */
    public static boolean areTeammates(Entity a, Entity b) {
        if (a == null || b == null || a == b) {
            return false;
        }

        if (!(a instanceof Player playerA) || !(b instanceof Player playerB)) {
            return false;
        }

        if (playerA.getUUID().equals(playerB.getUUID())) {
            return false;
        }

        // 必须处于拾荒回廊维度
        if (!playerA.level().dimension().equals(ModDimensions.SCAVENGER_CORRIDOR_LEVEL) ||
            !playerB.level().dimension().equals(ModDimensions.SCAVENGER_CORRIDOR_LEVEL)) {
            return false;
        }

        // 服务端环境
        if (playerA instanceof ServerPlayer serverPlayerA && playerB instanceof ServerPlayer serverPlayerB) {
            return ScavengerTeamManager.areTeammates(serverPlayerA, serverPlayerB);
        }

        // 客户端环境
        if (playerA.level().isClientSide() && FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            return ClientCollisionHelper.isClientTeammate(playerA, playerB);
        }

        return false;
    }

    private static class ClientCollisionHelper {
        private static boolean isClientTeammate(Player a, Player b) {
            return me.tuanzi.client.ScavengerTeamClient.isTeammate(a) &&
                   me.tuanzi.client.ScavengerTeamClient.isTeammate(b);
        }
    }
}
