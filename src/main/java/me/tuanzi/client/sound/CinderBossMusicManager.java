package me.tuanzi.client.sound;

import me.tuanzi.entity.CinderSovereignEntity;
import me.tuanzi.util.ModLog;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

import java.util.List;

/**
 * 客户端 Boss 战斗背景音乐管理器
 */
@Environment(EnvType.CLIENT)
public class CinderBossMusicManager {
    private static CinderBossMusicInstance currentMusic = null;

    public static void startMusic() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSoundManager() == null || mc.level == null || mc.player == null) return;

        // 严格安全前置检查：周围 48 格内必须确有存活且未处于死亡状态的 Boss
        List<CinderSovereignEntity> aliveBosses = mc.level.getEntitiesOfClass(
                CinderSovereignEntity.class,
                mc.player.getBoundingBox().inflate(52.0),
                b -> b.isAlive() && !b.isDeadOrDying() && b.distanceTo(mc.player) <= 48.0
        );
        if (aliveBosses.isEmpty()) {
            return;
        }

        if (currentMusic == null || currentMusic.isStopped() || !mc.getSoundManager().isActive(currentMusic)) {
            currentMusic = new CinderBossMusicInstance();
            mc.getSoundManager().play(currentMusic);
            if (mc.getMusicManager() != null) {
                mc.getMusicManager().stopPlaying();
            }
            ModLog.debug("【日冕核心·战斗BGM】玩家进入 Boss 半径 48 格领域，开始播放激昂战斗背景音乐！");
        }
    }

    public static void stopMusic() {
        if (currentMusic != null) {
            currentMusic.fadeOut();
            currentMusic = null;
            ModLog.debug("【日冕核心·战斗BGM】Boss 死亡或脱离领域，战斗背景音乐平滑淡出停止。");
        }
    }
}
