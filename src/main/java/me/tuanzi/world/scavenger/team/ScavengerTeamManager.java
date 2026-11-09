package me.tuanzi.world.scavenger.team;

import me.tuanzi.init.ModDimensions;
import me.tuanzi.network.ScavengerTeamSyncPacket;
import me.tuanzi.util.ModLog;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.scores.TeamColor;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 拾荒回廊小队服务端管理器
 */
public class ScavengerTeamManager {

    private static final Map<UUID, ScavengerTeam> teamsByTeamId = new ConcurrentHashMap<>();
    private static final Map<UUID, UUID> playerToTeamId = new ConcurrentHashMap<>();

    // 邀请缓存：目标玩家 UUID -> 邀请详情，有效期 60 秒
    private record TeamInvite(UUID inviterUuid, String inviterName, UUID teamId, long expireTimeMs) {}
    private static final Map<UUID, TeamInvite> pendingInvites = new ConcurrentHashMap<>();

    /**
     * 判断两个玩家是否为拾荒回廊同队队友
     */
    public static boolean areTeammates(ServerPlayer a, ServerPlayer b) {
        if (a == null || b == null || a.getUUID().equals(b.getUUID())) {
            return false;
        }
        if (!a.level().dimension().equals(ModDimensions.SCAVENGER_CORRIDOR_LEVEL) ||
            !b.level().dimension().equals(ModDimensions.SCAVENGER_CORRIDOR_LEVEL)) {
            return false;
        }

        UUID teamA = playerToTeamId.get(a.getUUID());
        UUID teamB = playerToTeamId.get(b.getUUID());
        return teamA != null && teamA.equals(teamB);
    }

    /**
     * 判断两个玩家是否处于拾荒回廊同一队伍中（包括自身）
     */
    public static boolean isSameTeam(ServerPlayer a, ServerPlayer b) {
        if (a == null || b == null) {
            return false;
        }
        if (!a.level().dimension().equals(ModDimensions.SCAVENGER_CORRIDOR_LEVEL) ||
            !b.level().dimension().equals(ModDimensions.SCAVENGER_CORRIDOR_LEVEL)) {
            return false;
        }

        UUID teamA = playerToTeamId.get(a.getUUID());
        UUID teamB = playerToTeamId.get(b.getUUID());
        return teamA != null && teamA.equals(teamB);
    }

    /**
     * 创建队伍
     */
    public static boolean createTeam(ServerPlayer player) {
        if (!player.level().dimension().equals(ModDimensions.SCAVENGER_CORRIDOR_LEVEL)) {
            player.sendSystemMessage(Component.literal("§c小队功能仅限在拾荒回廊维度中使用！"));
            return false;
        }

        if (playerToTeamId.containsKey(player.getUUID())) {
            player.sendSystemMessage(Component.literal("§c你已在队伍中，无法创建新队伍！若要创建请先退出当前队伍。"));
            return false;
        }

        UUID teamId = UUID.randomUUID();
        ScavengerTeam team = new ScavengerTeam(teamId, player.getUUID());
        teamsByTeamId.put(teamId, team);
        playerToTeamId.put(player.getUUID(), teamId);

        syncTeamToMembers(team, player.level().getServer());
        player.sendSystemMessage(Component.literal("§a成功创建拾荒回廊小队！你当前为队长，队伍发光颜色为绿色（上限3人）。"));
        ModLog.debug(player, null, "【拾荒回廊组队】玩家创建了队伍: " + teamId);
        return true;
    }

    /**
     * 邀请玩家加入队伍
     */
    public static boolean invitePlayer(ServerPlayer inviter, ServerPlayer target) {
        if (!inviter.level().dimension().equals(ModDimensions.SCAVENGER_CORRIDOR_LEVEL) ||
            !target.level().dimension().equals(ModDimensions.SCAVENGER_CORRIDOR_LEVEL)) {
            inviter.sendSystemMessage(Component.literal("§c只能邀请同在拾荒回廊中的玩家！"));
            return false;
        }

        UUID teamId = playerToTeamId.get(inviter.getUUID());
        if (teamId == null) {
            inviter.sendSystemMessage(Component.literal("§c你当前不在任何队伍中，请先通过 /corridor team create 创建队伍！"));
            return false;
        }

        ScavengerTeam team = teamsByTeamId.get(teamId);
        if (team == null) {
            inviter.sendSystemMessage(Component.literal("§c队伍不存在！"));
            return false;
        }

        if (team.isFull()) {
            inviter.sendSystemMessage(Component.literal("§c当前队伍人数已达上限（3人），无法邀请更多玩家！"));
            return false;
        }

        if (inviter.getUUID().equals(target.getUUID())) {
            inviter.sendSystemMessage(Component.literal("§c不能邀请你自己！"));
            return false;
        }

        if (playerToTeamId.containsKey(target.getUUID())) {
            inviter.sendSystemMessage(Component.literal("§c玩家 " + target.getPlainTextName() + " 已在其它队伍中！"));
            return false;
        }

        long expireTime = System.currentTimeMillis() + 60_000L;
        pendingInvites.put(target.getUUID(), new TeamInvite(inviter.getUUID(), inviter.getPlainTextName(), teamId, expireTime));

        // 发送可交互邀请给目标玩家
        Component acceptBtn = Component.literal("§a§l[接受]")
                .withStyle(style -> style
                        .withClickEvent(new ClickEvent.RunCommand("/corridor team accept"))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("点击立即同意加入小队")))
                );
        Component denyBtn = Component.literal("§c§l[拒绝]")
                .withStyle(style -> style
                        .withClickEvent(new ClickEvent.RunCommand("/corridor team deny"))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("点击拒绝加入小队")))
                );

        Component inviteMsg = Component.empty()
                .append(Component.literal("§e[拾荒回廊组队] §f玩家 §b" + inviter.getPlainTextName() + " §f邀请你加入小队（60秒内有效）： "))
                .append(acceptBtn)
                .append(Component.literal("  "))
                .append(denyBtn);

        target.sendSystemMessage(inviteMsg);
        inviter.sendSystemMessage(Component.literal("§a已向 " + target.getPlainTextName() + " 发送组队邀请。"));
        return true;
    }

    /**
     * 同意加入队伍
     */
    public static boolean acceptInvite(ServerPlayer player) {
        if (!player.level().dimension().equals(ModDimensions.SCAVENGER_CORRIDOR_LEVEL)) {
            player.sendSystemMessage(Component.literal("§c组队功能仅限在拾荒回廊中生效！"));
            return false;
        }

        if (playerToTeamId.containsKey(player.getUUID())) {
            player.sendSystemMessage(Component.literal("§c你已经在队伍中，请先退出当前队伍！"));
            pendingInvites.remove(player.getUUID());
            return false;
        }

        TeamInvite invite = pendingInvites.remove(player.getUUID());
        if (invite == null || System.currentTimeMillis() > invite.expireTimeMs()) {
            player.sendSystemMessage(Component.literal("§c组队邀请已过期或不存在！"));
            return false;
        }

        ScavengerTeam team = teamsByTeamId.get(invite.teamId());
        if (team == null) {
            player.sendSystemMessage(Component.literal("§c该队伍已解散！"));
            return false;
        }

        if (team.isFull()) {
            player.sendSystemMessage(Component.literal("§c该队伍人数已满（3人）！"));
            return false;
        }

        team.addMember(player.getUUID());
        playerToTeamId.put(player.getUUID(), team.getTeamId());

        MinecraftServer server = player.level().getServer();
        broadcastToTeam(team, server, Component.literal("§a玩家 §b" + player.getPlainTextName() + " §a加入了小队！（队伍成员 " + team.getMemberCount() + "/3）"));
        syncTeamToMembers(team, server);

        ModLog.debug(player, null, "【拾荒回廊组队】玩家成功接受邀请加入队伍: " + team.getTeamId());
        return true;
    }

    /**
     * 拒绝组队邀请
     */
    public static boolean denyInvite(ServerPlayer player) {
        TeamInvite invite = pendingInvites.remove(player.getUUID());
        if (invite == null || System.currentTimeMillis() > invite.expireTimeMs()) {
            player.sendSystemMessage(Component.literal("§7当前没有待处理的组队邀请。"));
            return false;
        }

        player.sendSystemMessage(Component.literal("§7你已拒绝了 " + invite.inviterName() + " 的组队邀请。"));
        ServerPlayer inviter = player.level().getServer().getPlayerList().getPlayer(invite.inviterUuid());
        if (inviter != null) {
            inviter.sendSystemMessage(Component.literal("§7玩家 " + player.getPlainTextName() + " 拒绝了你的组队邀请。"));
        }
        return true;
    }

    /**
     * 离开队伍
     */
    public static boolean leaveTeam(ServerPlayer player) {
        UUID teamId = playerToTeamId.remove(player.getUUID());
        if (teamId == null) {
            player.sendSystemMessage(Component.literal("§c你当前不在任何队伍中！"));
            return false;
        }

        ScavengerTeam team = teamsByTeamId.get(teamId);
        clearPlayerClientTeam(player);
        player.sendSystemMessage(Component.literal("§e你已退出小队。"));

        if (team == null) {
            return true;
        }

        MinecraftServer server = player.level().getServer();
        team.removeMember(player.getUUID());

        if (team.getMemberCount() <= 1) {
            // 人数少于2人，自动解散
            disbandTeamInternal(team, server, "队伍人数少于2人，小队已自动解散。");
        } else {
            // 正常广播退出并重新同步队伍
            ServerPlayer leader = server.getPlayerList().getPlayer(team.getLeaderId());
            String leaderName = leader != null ? leader.getPlainTextName() : "未知";
            broadcastToTeam(team, server, Component.literal("§e队员 §b" + player.getPlainTextName() + " §e已离开队伍。当前队长为: §b" + leaderName));
            syncTeamToMembers(team, server);
        }

        ModLog.debug(player, null, "【拾荒回廊组队】玩家退出队伍: " + teamId);
        return true;
    }

    /**
     * 队长踢出队员
     */
    public static boolean kickMember(ServerPlayer leader, ServerPlayer target) {
        UUID teamId = playerToTeamId.get(leader.getUUID());
        if (teamId == null) {
            leader.sendSystemMessage(Component.literal("§c你当前不在队伍中！"));
            return false;
        }

        ScavengerTeam team = teamsByTeamId.get(teamId);
        if (team == null || !team.isLeader(leader.getUUID())) {
            leader.sendSystemMessage(Component.literal("§c只有队长才有权限踢出队员！"));
            return false;
        }

        if (leader.getUUID().equals(target.getUUID())) {
            leader.sendSystemMessage(Component.literal("§c队长不能踢出自己！若要解散队伍请使用 /corridor team disband"));
            return false;
        }

        if (!team.isMember(target.getUUID())) {
            leader.sendSystemMessage(Component.literal("§c该玩家不是你的队伍成员！"));
            return false;
        }

        playerToTeamId.remove(target.getUUID());
        team.removeMember(target.getUUID());
        clearPlayerClientTeam(target);
        target.sendSystemMessage(Component.literal("§c你已被队长移出小队！"));

        MinecraftServer server = leader.level().getServer();
        if (team.getMemberCount() <= 1) {
            disbandTeamInternal(team, server, "队伍人数少于2人，小队已自动解散。");
        } else {
            broadcastToTeam(team, server, Component.literal("§e队员 §b" + target.getPlainTextName() + " §e被队长踢出队伍。"));
            syncTeamToMembers(team, server);
        }

        return true;
    }

    /**
     * 队长解散队伍
     */
    public static boolean disbandTeam(ServerPlayer leader) {
        UUID teamId = playerToTeamId.get(leader.getUUID());
        if (teamId == null) {
            leader.sendSystemMessage(Component.literal("§c你当前不在队伍中！"));
            return false;
        }

        ScavengerTeam team = teamsByTeamId.get(teamId);
        if (team == null || !team.isLeader(leader.getUUID())) {
            leader.sendSystemMessage(Component.literal("§c只有队长才有权限解散队伍！"));
            return false;
        }

        disbandTeamInternal(team, leader.level().getServer(), "小队已被队长解散。");
        return true;
    }

    /**
     * 设置队伍发光颜色
     */
    public static boolean setTeamColor(ServerPlayer leader, TeamColor color) {
        UUID teamId = playerToTeamId.get(leader.getUUID());
        if (teamId == null) {
            leader.sendSystemMessage(Component.literal("§c你当前不在队伍中！"));
            return false;
        }

        ScavengerTeam team = teamsByTeamId.get(teamId);
        if (team == null || !team.isLeader(leader.getUUID())) {
            leader.sendSystemMessage(Component.literal("§c只有队长才有权限设置队伍发光颜色！"));
            return false;
        }

        if (color == null) {
            leader.sendSystemMessage(Component.literal("§c无效的颜色！"));
            return false;
        }

        team.setColor(color);
        MinecraftServer server = leader.level().getServer();
        broadcastToTeam(team, server, Component.literal("§a队长已将队伍远距离高亮颜色设置为: ").append(Component.literal(color.getSerializedName()).withStyle(style -> style.withColor(color.textColor()))));
        syncTeamToMembers(team, server);
        return true;
    }

    /**
     * 查看队伍信息
     */
    public static void showTeamInfo(ServerPlayer player) {
        UUID teamId = playerToTeamId.get(player.getUUID());
        if (teamId == null) {
            player.sendSystemMessage(Component.literal("§7你当前未加入任何拾荒回廊小队。"));
            return;
        }

        ScavengerTeam team = teamsByTeamId.get(teamId);
        if (team == null) {
            player.sendSystemMessage(Component.literal("§c队伍数据异常。"));
            return;
        }

        MinecraftServer server = player.level().getServer();
        ServerPlayer leader = server.getPlayerList().getPlayer(team.getLeaderId());
        String leaderName = leader != null ? leader.getPlainTextName() : "未知";

        player.sendSystemMessage(Component.literal("§6====== 拾荒回廊小队信息 ======"));
        player.sendSystemMessage(Component.literal("§e队长: §b" + leaderName + (team.isLeader(player.getUUID()) ? " §a(你)" : "")));
        player.sendSystemMessage(Component.literal("§e高亮颜色: ").append(Component.literal(team.getColor().getSerializedName()).withStyle(style -> style.withColor(team.getColor().textColor()))));
        player.sendSystemMessage(Component.literal("§e队伍配置:"));
        player.sendSystemMessage(Component.literal("  - 风弹影响队友: " + (team.isWindChargeAffectsTeammates() ? "§a已开启" : "§c已关闭")));
        player.sendSystemMessage(Component.literal("  - 钓鱼竿影响队友: " + (team.isFishingHookAffectsTeammates() ? "§a已开启" : "§c已关闭")));
        player.sendSystemMessage(Component.literal("§e队员列表 (" + team.getMemberCount() + "/3):"));
        for (UUID memberUuid : team.getMemberIds()) {
            ServerPlayer member = server.getPlayerList().getPlayer(memberUuid);
            String name = member != null ? member.getPlainTextName() : memberUuid.toString();
            player.sendSystemMessage(Component.literal("  - §f" + name + (memberUuid.equals(team.getLeaderId()) ? " §6[队长]" : "")));
        }
        player.sendSystemMessage(Component.literal("§6=============================="));
    }

    /**
     * 获取玩家所在的队伍
     */
    public static ScavengerTeam getTeamByPlayer(ServerPlayer player) {
        if (player == null) return null;
        UUID teamId = playerToTeamId.get(player.getUUID());
        return teamId != null ? teamsByTeamId.get(teamId) : null;
    }

    /**
     * 设置队伍风弹影响开关
     */
    public static boolean setWindChargeAffectsTeammates(ServerPlayer leader, boolean enabled) {
        UUID teamId = playerToTeamId.get(leader.getUUID());
        if (teamId == null) {
            leader.sendSystemMessage(Component.literal("§c你当前不在队伍中！"));
            return false;
        }

        ScavengerTeam team = teamsByTeamId.get(teamId);
        if (team == null || !team.isLeader(leader.getUUID())) {
            leader.sendSystemMessage(Component.literal("§c只有队长才有权限修改小队配置！"));
            return false;
        }

        team.setWindChargeAffectsTeammates(enabled);
        MinecraftServer server = leader.level().getServer();
        String statusText = enabled ? "§a已开启（受风弹击退与弹射影响）" : "§c已关闭（免疫风弹击退与弹射）";
        broadcastToTeam(team, server, Component.literal("§e[队伍配置] 队长已将【风弹影响队友】设置为: " + statusText));
        ModLog.debug(leader, null, "【拾荒回廊组队】队长修改了风弹影响开关: " + enabled);
        return true;
    }

    /**
     * 设置队伍钓鱼竿影响开关
     */
    public static boolean setFishingHookAffectsTeammates(ServerPlayer leader, boolean enabled) {
        UUID teamId = playerToTeamId.get(leader.getUUID());
        if (teamId == null) {
            leader.sendSystemMessage(Component.literal("§c你当前不在队伍中！"));
            return false;
        }

        ScavengerTeam team = teamsByTeamId.get(teamId);
        if (team == null || !team.isLeader(leader.getUUID())) {
            leader.sendSystemMessage(Component.literal("§c只有队长才有权限修改小队配置！"));
            return false;
        }

        team.setFishingHookAffectsTeammates(enabled);
        MinecraftServer server = leader.level().getServer();
        String statusText = enabled ? "§a已开启（可被队友钓中与拉扯）" : "§c已关闭（免疫队友钓中与拉扯）";
        broadcastToTeam(team, server, Component.literal("§e[队伍配置] 队长已将【钓鱼竿影响队友】设置为: " + statusText));
        ModLog.debug(leader, null, "【拾荒回廊组队】队长修改了钓鱼竿影响开关: " + enabled);
        return true;
    }

    /**
     * 设置总友军物理影响开关（同时设置风弹与钓鱼竿）
     */
    public static boolean setFriendlyPhysics(ServerPlayer leader, boolean enabled) {
        UUID teamId = playerToTeamId.get(leader.getUUID());
        if (teamId == null) {
            leader.sendSystemMessage(Component.literal("§c你当前不在队伍中！"));
            return false;
        }

        ScavengerTeam team = teamsByTeamId.get(teamId);
        if (team == null || !team.isLeader(leader.getUUID())) {
            leader.sendSystemMessage(Component.literal("§c只有队长才有权限修改小队配置！"));
            return false;
        }

        team.setWindChargeAffectsTeammates(enabled);
        team.setFishingHookAffectsTeammates(enabled);
        MinecraftServer server = leader.level().getServer();
        String statusText = enabled ? "§a全部开启（允许风弹与钓鱼竿位移辅助）" : "§c全部关闭（免疫风弹与钓鱼竿影响）";
        broadcastToTeam(team, server, Component.literal("§e[队伍配置] 队长已将【友军物理互动】设置为: " + statusText));
        ModLog.debug(leader, null, "【拾荒回廊组队】队长修改了总友军物理影响开关: " + enabled);
        return true;
    }

    /**
     * 展示队伍配置交互面板
     */
    public static void showTeamConfig(ServerPlayer player) {
        UUID teamId = playerToTeamId.get(player.getUUID());
        if (teamId == null) {
            player.sendSystemMessage(Component.literal("§7你当前未加入任何拾荒回廊小队。"));
            return;
        }

        ScavengerTeam team = teamsByTeamId.get(teamId);
        if (team == null) {
            player.sendSystemMessage(Component.literal("§c队伍数据异常。"));
            return;
        }

        boolean isLeader = team.isLeader(player.getUUID());
        player.sendSystemMessage(Component.literal("§6====== 拾荒回廊小队配置 ======"));
        player.sendSystemMessage(Component.literal("§e队伍高亮颜色: ").append(Component.literal(team.getColor().getSerializedName()).withStyle(style -> style.withColor(team.getColor().textColor()))));

        // 风弹开关状态与交互按钮
        boolean wc = team.isWindChargeAffectsTeammates();
        Component wcBtn = Component.literal(wc ? "§a[已开启]" : "§c[已关闭]");
        if (isLeader) {
            wcBtn = wcBtn.copy().withStyle(style -> style
                .withClickEvent(new ClickEvent.RunCommand("/corridor team config wind_charge " + !wc))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("点击切换风弹影响（当前: " + (wc ? "允许" : "免疫") + "）")))
            );
        }
        player.sendSystemMessage(Component.empty().append(Component.literal("§e风弹影响队友: ")).append(wcBtn).append(Component.literal(isLeader ? " §7(点击切换)" : "")));

        // 钓鱼竿开关状态与交互按钮
        boolean fh = team.isFishingHookAffectsTeammates();
        Component fhBtn = Component.literal(fh ? "§a[已开启]" : "§c[已关闭]");
        if (isLeader) {
            fhBtn = fhBtn.copy().withStyle(style -> style
                .withClickEvent(new ClickEvent.RunCommand("/corridor team config fishing_rod " + !fh))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("点击切换钓鱼竿影响（当前: " + (fh ? "允许" : "免疫") + "）")))
            );
        }
        player.sendSystemMessage(Component.empty().append(Component.literal("§e钓鱼竿影响队友: ")).append(fhBtn).append(Component.literal(isLeader ? " §7(点击切换)" : "")));

        if (isLeader) {
            Component allOnBtn = Component.literal("§a[全部开启]").withStyle(style -> style
                .withClickEvent(new ClickEvent.RunCommand("/corridor team config friendly_physics true"))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("一键开启风弹与钓鱼竿影响")))
            );
            Component allOffBtn = Component.literal("§c[全部关闭]").withStyle(style -> style
                .withClickEvent(new ClickEvent.RunCommand("/corridor team config friendly_physics false"))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("一键关闭风弹与钓鱼竿影响")))
            );
            player.sendSystemMessage(Component.empty().append(Component.literal("§e快捷切换: ")).append(allOnBtn).append(Component.literal("  ")).append(allOffBtn));
        }
        player.sendSystemMessage(Component.literal("§6=============================="));
    }

    /**
     * 玩家离开回廊或断开连接时的队伍生命周期处理
     */
    public static void onPlayerLeaveCorridorOrDisconnect(ServerPlayer player) {
        UUID teamId = playerToTeamId.remove(player.getUUID());
        pendingInvites.remove(player.getUUID());
        clearPlayerClientTeam(player);

        if (teamId == null) {
            return;
        }

        ScavengerTeam team = teamsByTeamId.get(teamId);
        if (team == null) {
            return;
        }

        MinecraftServer server = player.level().getServer();
        team.removeMember(player.getUUID());

        if (team.getMemberCount() <= 1) {
            disbandTeamInternal(team, server, "有队员离开回廊/断开连接，队伍剩余人数少于2人，小队自动解散。");
        } else {
            ServerPlayer leader = server.getPlayerList().getPlayer(team.getLeaderId());
            String leaderName = leader != null ? leader.getPlainTextName() : "未知";
            broadcastToTeam(team, server, Component.literal("§e队员 §b" + player.getPlainTextName() + " §e已离开回廊/断开连接并退出小队。当前队长为: §b" + leaderName));
            syncTeamToMembers(team, server);
        }
    }

    private static void disbandTeamInternal(ScavengerTeam team, MinecraftServer server, String reason) {
        teamsByTeamId.remove(team.getTeamId());
        for (UUID memberId : team.getMemberIds()) {
            playerToTeamId.remove(memberId);
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);
            if (member != null) {
                clearPlayerClientTeam(member);
                member.sendSystemMessage(Component.literal("§c" + reason));
            }
        }
        ModLog.debug("【拾荒回廊组队】小队已解散: " + team.getTeamId() + "，原因: " + reason);
    }

    private static void broadcastToTeam(ScavengerTeam team, MinecraftServer server, Component message) {
        for (UUID memberId : team.getMemberIds()) {
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);
            if (member != null) {
                member.sendSystemMessage(message);
            }
        }
    }

    public static void syncTeamToMembers(ScavengerTeam team, MinecraftServer server) {
        List<UUID> memberList = new ArrayList<>(team.getMemberIds());
        ScavengerTeamSyncPacket packet = new ScavengerTeamSyncPacket(true, team.getColorRgb(), memberList);

        for (UUID memberId : team.getMemberIds()) {
            ServerPlayer member = server.getPlayerList().getPlayer(memberId);
            if (member != null) {
                ServerPlayNetworking.send(member, packet);
            }
        }
    }

    public static void clearPlayerClientTeam(ServerPlayer player) {
        ScavengerTeamSyncPacket packet = new ScavengerTeamSyncPacket(false, 0x55FF55, List.of());
        ServerPlayNetworking.send(player, packet);
    }
}
