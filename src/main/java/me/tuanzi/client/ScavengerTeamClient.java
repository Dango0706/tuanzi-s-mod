package me.tuanzi.client;

import me.tuanzi.init.ModDimensions;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 拾荒回廊客户端队伍状态缓存与高亮判定
 */
@Environment(EnvType.CLIENT)
public class ScavengerTeamClient {
    private static final Set<UUID> teammateUuids = new HashSet<>();
    private static boolean inTeam = false;
    private static int teamColorRgb = 0x55FF55;

    public static void updateTeam(boolean active, int colorRgb, List<UUID> members) {
        inTeam = active;
        teamColorRgb = colorRgb;
        teammateUuids.clear();
        if (members != null) {
            teammateUuids.addAll(members);
        }
    }

    public static void clearTeam() {
        inTeam = false;
        teamColorRgb = 0x55FF55;
        teammateUuids.clear();
    }

    public static boolean isInTeam() {
        return inTeam;
    }

    public static int getTeamColorRgb() {
        return teamColorRgb;
    }

    /**
     * 判断指定玩家是否为本地队伍中的一员（包括自己或队友）
     */
    public static boolean isTeammate(Player player) {
        if (!inTeam || player == null) {
            return false;
        }
        if (!player.level().dimension().equals(ModDimensions.SCAVENGER_CORRIDOR_LEVEL)) {
            return false;
        }
        return teammateUuids.contains(player.getUUID());
    }

    /**
     * 判断目标实体是否应该为本地客户端发光：
     * 条件：
     * 1. 本地玩家处于组队状态
     * 2. 目标实体为玩家，且为同队队友（非自己）
     * 3. 双方处于拾荒回廊维度
     * 4. 双方距离 > 20 米 (distanceSq > 400.0)
     */
    public static boolean shouldGlow(Player clientPlayer, Entity target) {
        if (!inTeam || clientPlayer == null || !(target instanceof Player targetPlayer)) {
            return false;
        }

        if (targetPlayer.getUUID().equals(clientPlayer.getUUID())) {
            return false;
        }

        if (!clientPlayer.level().dimension().equals(ModDimensions.SCAVENGER_CORRIDOR_LEVEL)) {
            return false;
        }

        if (!teammateUuids.contains(targetPlayer.getUUID())) {
            return false;
        }

        return clientPlayer.distanceToSqr(targetPlayer) > 400.0;
    }

    /**
     * 获取队友高亮轮廓颜色
     */
    public static int getTeammateOutlineColor(Player clientPlayer, Entity target) {
        if (shouldGlow(clientPlayer, target)) {
            return teamColorRgb;
        }
        return -1;
    }
}
