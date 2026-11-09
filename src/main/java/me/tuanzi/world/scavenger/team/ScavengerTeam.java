package me.tuanzi.world.scavenger.team;

import net.minecraft.world.scores.TeamColor;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 拾荒回廊专属小队实体类
 * 最多容纳 3 名玩家，拥有队长与队伍发光颜色。
 */
public class ScavengerTeam {
    public static final int MAX_MEMBERS = 3;

    private final UUID teamId;
    private UUID leaderId;
    private final Set<UUID> memberIds = new LinkedHashSet<>();
    private TeamColor color = TeamColor.GREEN;
    // 队伍配置：风弹与钓鱼竿对队友的影响（队长可配置是否受到影响，默认 true）
    private boolean windChargeAffectsTeammates = true;
    private boolean fishingHookAffectsTeammates = true;

    public ScavengerTeam(UUID teamId, UUID leaderId) {
        this.teamId = teamId;
        this.leaderId = leaderId;
        this.memberIds.add(leaderId);
    }

    public UUID getTeamId() {
        return teamId;
    }

    public UUID getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(UUID leaderId) {
        if (memberIds.contains(leaderId)) {
            this.leaderId = leaderId;
        }
    }

    public Set<UUID> getMemberIds() {
        return Collections.unmodifiableSet(memberIds);
    }

    public int getMemberCount() {
        return memberIds.size();
    }

    public boolean isFull() {
        return memberIds.size() >= MAX_MEMBERS;
    }

    public boolean isLeader(UUID playerId) {
        return leaderId.equals(playerId);
    }

    public boolean isMember(UUID playerId) {
        return memberIds.contains(playerId);
    }

    public boolean addMember(UUID playerId) {
        if (isFull() || memberIds.contains(playerId)) {
            return false;
        }
        return memberIds.add(playerId);
    }

    public boolean removeMember(UUID playerId) {
        boolean removed = memberIds.remove(playerId);
        if (removed && leaderId.equals(playerId) && !memberIds.isEmpty()) {
            // 队长离开，顺位将队长移交给集合中的第一位成员
            this.leaderId = memberIds.iterator().next();
        }
        return removed;
    }

    public TeamColor getColor() {
        return color;
    }

    public void setColor(TeamColor color) {
        if (color != null) {
            this.color = color;
        }
    }

    public int getColorRgb() {
        return color != null ? color.rgb() : 0x55FF55; // 默认绿色
    }

    public boolean isWindChargeAffectsTeammates() {
        return windChargeAffectsTeammates;
    }

    public void setWindChargeAffectsTeammates(boolean enabled) {
        this.windChargeAffectsTeammates = enabled;
    }

    public boolean isFishingHookAffectsTeammates() {
        return fishingHookAffectsTeammates;
    }

    public void setFishingHookAffectsTeammates(boolean enabled) {
        this.fishingHookAffectsTeammates = enabled;
    }
}
