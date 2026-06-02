package me.tuanzi.mixin;

import me.tuanzi.util.IGachaFirework;
import me.tuanzi.util.ModLog;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.player.Player;
import java.util.UUID;

@Mixin(FireworkRocketEntity.class)
public class FireworkRocketEntityMixin implements IGachaFirework {
    
    @Unique
    private ItemStack tuanziGachaPrize = ItemStack.EMPTY;
    @Unique
    private UUID tuanziGachaTarget = null;
    @Unique
    private String tuanziGachaRarity = "common";

    @Override
    public void tuanzi$setGachaData(ItemStack prize, UUID target, String rarity) {
        this.tuanziGachaPrize = prize != null ? prize : ItemStack.EMPTY;
        this.tuanziGachaTarget = target;
        this.tuanziGachaRarity = rarity != null ? rarity : "common";
    }

    @Inject(method = "explode", at = @At("HEAD"))
    private void onExplode(ServerLevel level, CallbackInfo ci) {
        if (this.tuanziGachaPrize != null && !this.tuanziGachaPrize.isEmpty()) {
            Entity self = (Entity) (Object) this;
            double ex = self.getX();
            double ey = self.getY();
            double ez = self.getZ();

            // 1. 在烟花爆炸的高空精确位置生成物品实体
            ItemEntity itemEntity = new ItemEntity(level, ex, ey, ez, this.tuanziGachaPrize.copy());

            // 2. 赋予开卡人专属拾取限制 + 绝对防火免伤 + 1秒拾取延迟
            itemEntity.setTarget(this.tuanziGachaTarget);
            itemEntity.setInvulnerable(true);
            itemEntity.setPickUpDelay(20);

            // 3. 计算背离玩家中心的方向，让掉落物朝外侧优雅散射抛出
            double dx = 0;
            double dz = 0;
            try {
                Player player = level.getPlayerByUUID(this.tuanziGachaTarget);
                if (player != null) {
                    dx = ex - player.getX();
                    dz = ez - player.getZ();
                }
            } catch (Exception e) {
                // 忽略
            }
            // 降级兜底方案，如果太近或者获取不到玩家，使用随机朝向
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len < 0.1) {
                double randAngle = level.getRandom().nextDouble() * 2 * Math.PI;
                dx = Math.cos(randAngle);
                dz = Math.sin(randAngle);
            } else {
                dx /= len;
                dz /= len;
            }
            itemEntity.setDeltaMovement(dx * 0.12, 0.15, dz * 0.12);

            // 4. 开启原生发光轮廓
            itemEntity.setGlowingTag(true);

            // 5. 根据稀有度品质，赋予对应的发光轮廓颜色 (计分板 Team 队伍颜色)
            try {
                Scoreboard scoreboard = level.getScoreboard();
                String teamName = "gacha_" + this.tuanziGachaRarity.toLowerCase();
                PlayerTeam team = scoreboard.getPlayerTeam(teamName);
                if (team == null) {
                    team = scoreboard.addPlayerTeam(teamName);
                    ChatFormatting color = switch (this.tuanziGachaRarity.toLowerCase()) {
                        case "legendary" -> ChatFormatting.GOLD;
                        case "epic" -> ChatFormatting.LIGHT_PURPLE;
                        case "rare" -> ChatFormatting.AQUA;
                        case "uncommon" -> ChatFormatting.GREEN;
                        default -> ChatFormatting.WHITE;
                    };
                    team.setColor(color);
                }
                scoreboard.addPlayerToTeam(itemEntity.getScoreboardName(), team);
            } catch (Exception e) {
                ModLog.debug("Error assigning scoreboard team color for gacha drop: " + e.getMessage());
            }

            // 6. 将定制掉落物放入世界
            level.addFreshEntity(itemEntity);

            // 7. 打印调试日志
            ModLog.debug("Gacha Burst Spawned: Item " + this.tuanziGachaPrize.getHoverName().getString() 
                    + " at (" + ex + ", " + ey + ", " + ez + ") with glowing rarity " + this.tuanziGachaRarity 
                    + " for player " + this.tuanziGachaTarget);
        }
    }
}
