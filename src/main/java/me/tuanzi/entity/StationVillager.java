package me.tuanzi.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class StationVillager extends Villager {
    public StationVillager(EntityType<? extends Villager> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public boolean stillValid(Player player) {
        double distSqr = player.distanceToSqr(this.getX(), this.getY(), this.getZ());
        // 只要虚拟村民存活，且玩家与贸易站物理距离在 8 格内，就永远保持交易有效，完全绕过原版在世界中无实体跟踪导致闪退的限制
        return this.isAlive() && distSqr <= 64.0;
    }

    @Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        // 服务端拒绝一切伤害
        return false;
    }
}
