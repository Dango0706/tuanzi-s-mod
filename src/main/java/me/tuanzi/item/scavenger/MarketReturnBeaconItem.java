package me.tuanzi.item.scavenger;

import me.tuanzi.init.ModDimensions;
import me.tuanzi.util.ModLog;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/**
 * 归市信标 (Market Return Beacon)
 * 八边形扁盒状便携信标，嵌有翠绿色发光天平水晶。
 * 右键使用将玩家接引传送至拾荒回廊集市/商店 (5000, 64, -5000)。
 */
public class MarketReturnBeaconItem extends Item {

    public static final int COOLDOWN_TICKS = 300; // 15秒
    public static final BlockPos MARKET_POS = new BlockPos(5000, 64, -5000);

    public MarketReturnBeaconItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        return executeBeaconTeleport(player, stack);
    }

    /**
     * 执行归市传送逻辑（支持手持或独立饰品快捷键调用）
     */
    public static InteractionResult executeBeaconTeleport(Player player, ItemStack stack) {
        Level level = player.level();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.PASS;
        }

        if (level.dimension() != ModDimensions.SCAVENGER_CORRIDOR_LEVEL) {
            player.sendSystemMessage(Component.translatable("message.tuanzis_mod.beacon.wrong_dimension").withStyle(ChatFormatting.RED));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0F, 1.0F);
            return InteractionResult.FAIL;
        }

        ServerLevel corridorLevel = (ServerLevel) level;
        ServerPlayer serverPlayer = (ServerPlayer) player;

        // 确保集市安全基台完备
        ModDimensions.ensurePlatform(corridorLevel, MARKET_POS);

        // 原地音效与粒子
        corridorLevel.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.3F);
        corridorLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 35, 0.4, 0.8, 0.4, 0.05);

        Vec3 targetPos = new Vec3(MARKET_POS.getX() + 0.5, MARKET_POS.getY() + 1.0, MARKET_POS.getZ() + 0.5);
        TeleportTransition transition = new TeleportTransition(
            corridorLevel,
            targetPos,
            Vec3.ZERO,
            serverPlayer.getYRot(),
            serverPlayer.getXRot(),
            TeleportTransition.DO_NOTHING
        );
        serverPlayer.teleport(transition);

        // 目标地音效与金绿粒子
        corridorLevel.playSound(null, targetPos.x, targetPos.y, targetPos.z, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.1F);
        corridorLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, targetPos.x, targetPos.y + 1.0, targetPos.z, 30, 0.5, 0.8, 0.5, 0.1);
        corridorLevel.sendParticles(ParticleTypes.WAX_ON, targetPos.x, targetPos.y + 0.8, targetPos.z, 20, 0.4, 0.5, 0.4, 0.05);

        serverPlayer.sendSystemMessage(Component.translatable("message.tuanzis_mod.market_beacon.success").withStyle(ChatFormatting.GREEN));
        player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);

        ModLog.debug(serverPlayer, null, "【拾荒回廊】玩家触发归市信标，已瞬发传送至回廊集市 (5000.5, 65.0, -4999.5)。");
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipComponents, net.minecraft.world.item.TooltipFlag tooltipFlag) {
        tooltipComponents.accept(Component.translatable("tooltip.tuanzis_mod.market_return_beacon.desc1").withStyle(ChatFormatting.GRAY));
        tooltipComponents.accept(Component.translatable("tooltip.tuanzis_mod.market_return_beacon.desc2").withStyle(ChatFormatting.DARK_GREEN));
        tooltipComponents.accept(Component.translatable("tooltip.tuanzis_mod.market_return_beacon.usage").withStyle(ChatFormatting.YELLOW));
        tooltipComponents.accept(Component.translatable("tooltip.tuanzis_mod.beacon.cooldown_tip").withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, tooltipDisplay, tooltipComponents, tooltipFlag);
    }
}
