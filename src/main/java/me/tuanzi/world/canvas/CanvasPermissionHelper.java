package me.tuanzi.world.canvas;

import me.tuanzi.config.ModConfig;
import me.tuanzi.util.ModLog;
import me.tuanzi.world.scavenger.team.ScavengerTeamManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Team;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 画布访问权限校验辅助工具：
 * 同时支持所有者、管理员(OP)、原版计分板 /team 队伍、拾荒回廊小队，以及 /canvas allow 临时授权。
 */
public final class CanvasPermissionHelper {
    private CanvasPermissionHelper() {
    }

    public record AccessDecision(boolean canEnter, boolean canEdit, boolean canManagePermission, String denyReasonKey) {
    }

    public static boolean isOwner(@Nullable UUID ownerUuid, @Nullable UUID playerUuid) {
        return ownerUuid != null && ownerUuid.equals(playerUuid);
    }

    public static boolean areTeammates(@Nullable ServerPlayer player, @Nullable UUID ownerUuid, @Nullable ServerPlayer onlineOwner) {
        if (player == null || ownerUuid == null) {
            return false;
        }
        if (player.getUUID().equals(ownerUuid)) {
            return true;
        }
        // 1. 检查拾荒回廊队伍
        UUID playerScavTeam = ScavengerTeamManager.getTeamIdByPlayerUuid(player.getUUID());
        UUID ownerScavTeam = ScavengerTeamManager.getTeamIdByPlayerUuid(ownerUuid);
        if (playerScavTeam != null && playerScavTeam.equals(ownerScavTeam)) {
            return true;
        }
        // 2. 检查原版计分板 /team
        Team playerTeam = player.getTeam();
        if (playerTeam != null) {
            if (onlineOwner != null && onlineOwner.getTeam() != null) {
                return playerTeam.isAlliedTo(onlineOwner.getTeam())
                        || Objects.equals(playerTeam.getName(), onlineOwner.getTeam().getName());
            }
            // 若主人离线，检查队伍成员列表是否包含主人名
            if (onlineOwner == null && player.level() != null && player.level().getServer() != null) {
                var profile = player.level().getServer().services().nameToIdCache().get(ownerUuid).orElse(null);
                if (profile != null && playerTeam.getPlayers().contains(profile.name())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static AccessDecision evaluateCanvasAccess(
            CanvasMetadata meta,
            ServerPlayer player,
            @Nullable ServerPlayer onlineOwner,
            @Nullable Set<UUID> grantedEditors
    ) {
        if (meta == null) {
            return new AccessDecision(false, false, false, "message.tuanzis_mod.canvas.no_canvas");
        }
        boolean op = player.canUseGameMasterBlocks();
        boolean owner = isOwner(meta.ownerUuid(), player.getUUID()) || meta.ownerUuid() == null;
        boolean granted = grantedEditors != null && grantedEditors.contains(player.getUUID());

        if (meta.corrupted()) {
            boolean canRead = owner || op || (ModConfig.isAllowPublicCanvas() && meta.permission().isPublic());
            ModLog.debug(player, "【画布权限】损坏画布只读检查: canRead=" + canRead + ", owner=" + owner + ", op=" + op);
            return new AccessDecision(canRead, false, owner || op,
                    canRead ? "" : "message.tuanzis_mod.canvas.no_permission");
        }

        if (owner || op) {
            return new AccessDecision(true, true, true, "");
        }

        if (granted) {
            return new AccessDecision(true, true, false, "");
        }

        CanvasPermission perm = meta.permission();
        if (perm == CanvasPermission.PRIVATE) {
            ModLog.debug(player, "【画布权限】拒绝进入私有画布: uuid=" + meta.uuid());
            return new AccessDecision(false, false, false, "message.tuanzis_mod.canvas.private_denied");
        }

        if (perm == CanvasPermission.TEAM_EDIT) {
            boolean teamOk = areTeammates(player, meta.ownerUuid(), onlineOwner);
            ModLog.debug(player, "【画布权限】队友权限检查: teamOk=" + teamOk + ", owner=" + meta.ownerUuid());
            return teamOk
                    ? new AccessDecision(true, true, false, "")
                    : new AccessDecision(false, false, false, "message.tuanzis_mod.canvas.team_denied");
        }

        // 公开权限受服务器总开关控制
        if (!ModConfig.isAllowPublicCanvas()) {
            ModLog.debug(player, "【画布权限】服务器配置禁用公开画布，拒绝进入: uuid=" + meta.uuid());
            return new AccessDecision(false, false, false, "message.tuanzis_mod.canvas.public_disabled");
        }

        if (perm == CanvasPermission.PUBLIC_READ) {
            return new AccessDecision(true, false, false, "");
        }
        if (perm == CanvasPermission.PUBLIC_EDIT) {
            return new AccessDecision(true, true, false, "");
        }
        return new AccessDecision(false, false, false, "message.tuanzis_mod.canvas.no_permission");
    }

    public static boolean canCopyModel(CanvasMetadata meta, ServerPlayer player, @Nullable ServerPlayer onlineOwner) {
        if (meta == null || !ModConfig.isAllowCanvasCopying()) {
            ModLog.debug(player, "【复印机权限】服务器禁止复印或模型元数据无效");
            return false;
        }
        if (meta.corrupted()) {
            return false;
        }
        if (player.canUseGameMasterBlocks() || isOwner(meta.ownerUuid(), player.getUUID()) || meta.ownerUuid() == null) {
            return true;
        }
        if (meta.permission() == CanvasPermission.PRIVATE) {
            ModLog.debug(player, "【复印机权限】私有模型仅所有者或管理员可复印");
            return false;
        }
        if (meta.permission() == CanvasPermission.TEAM_EDIT) {
            return areTeammates(player, meta.ownerUuid(), onlineOwner);
        }
        return ModConfig.isAllowPublicCanvas() && meta.permission().isPublic();
    }
}
