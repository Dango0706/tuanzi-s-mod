package me.tuanzi.util;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 临时方块管理器：
 * 负责管理陨石坑中心等临时生成的岩浆方块，在指定延迟（如 6 秒后）自动冷却固化或还原。
 */
public class TempBlockManager {
    public record TempEntry(ServerLevel level, BlockPos pos, long expireTick, BlockState targetState) {}

    private static final List<TempEntry> PENDING_ENTRIES = new ArrayList<>();

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(TempBlockManager::tick);
    }

    public static synchronized void schedule(ServerLevel level, BlockPos pos, int delayTicks, BlockState targetState) {
        long expireTick = level.getGameTime() + delayTicks;
        PENDING_ENTRIES.add(new TempEntry(level, pos, expireTick, targetState));
        ModLog.debug("【临时方块调度】在 " + pos + " 调度临时方块任务，将在 " + delayTicks + " 刻后转换为 " + targetState.getBlock().getName().getString());
    }

    private static synchronized void tick(MinecraftServer server) {
        if (PENDING_ENTRIES.isEmpty()) return;

        Iterator<TempEntry> iterator = PENDING_ENTRIES.iterator();
        while (iterator.hasNext()) {
            TempEntry entry = iterator.next();
            if (entry.level().getGameTime() >= entry.expireTick()) {
                iterator.remove();
                ServerLevel level = entry.level();
                BlockPos pos = entry.pos();
                // 仅当该方块仍为岩浆或原方块时进行固化替换
                if (level.getBlockState(pos).is(Blocks.LAVA)) {
                    level.setBlockAndUpdate(pos, entry.targetState());
                    level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.2F);
                    level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 6, 0.2, 0.3, 0.2, 0.05);
                    ModLog.debug("【临时方块还原】位置 " + pos + " 的临时岩浆已成功冷却固化为黑曜石！");
                }
            }
        }
    }
}
