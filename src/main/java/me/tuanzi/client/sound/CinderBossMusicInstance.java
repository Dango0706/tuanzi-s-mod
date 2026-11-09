package me.tuanzi.client.sound;

import me.tuanzi.client.gui.CinderHeatHud;
import me.tuanzi.entity.CinderSovereignEntity;
import me.tuanzi.init.ModSounds;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

import java.util.List;

/**
 * 日冕核心 · 烈焰统领 战斗背景音乐 Tickable 循环播放实例
 * - 半径 48 格领域内全景背景立体声循环播放
 * - 进出领域自动平滑淡入/淡出
 * - 自动压制原版常规背景音乐
 */
@Environment(EnvType.CLIENT)
public class CinderBossMusicInstance extends AbstractTickableSoundInstance {
    private boolean isFadingOut = false;

    public CinderBossMusicInstance() {
        super(ModSounds.CINDER_SOVEREIGN_THEME, SoundSource.MUSIC, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.volume = 0.05F; // 初始极微弱，平滑淡入
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            this.stop();
            return;
        }

        // 严格检查客户端周围 52 格内是否存在未死亡且存活的 Boss
        List<CinderSovereignEntity> aliveBosses = mc.level.getEntitiesOfClass(
                CinderSovereignEntity.class,
                mc.player.getBoundingBox().inflate(52.0),
                b -> b.isAlive() && !b.isDeadOrDying() && b.distanceTo(mc.player) <= 48.0
        );

        boolean bossAliveNearby = !aliveBosses.isEmpty();

        // 核心判定：仅当附近确有存活且未处于死亡状态的 Boss 且未主动触发淡出时维持播放；
        // 一旦 Boss 死亡 (bossAliveNearby 为 false) 或主动退出领域，立即平滑淡出终止
        boolean active = bossAliveNearby && !this.isFadingOut;

        if (active) {
            // 压制原版舒缓背景乐
            if (mc.getMusicManager() != null) {
                mc.getMusicManager().stopPlaying();
            }
            // 平滑淡入
            if (this.volume < 1.0F) {
                this.volume = Math.min(1.0F, this.volume + 0.04F);
            }
        } else {
            // 离开领域或 Boss 死亡：平滑淡出 (约 25 ticks / 1.25s 平滑淡出完毕)
            this.volume = Math.max(0.0F, this.volume - 0.04F);
            if (this.volume <= 0.0F) {
                this.stop();
            }
        }
    }

    public void fadeOut() {
        this.isFadingOut = true;
    }
}
