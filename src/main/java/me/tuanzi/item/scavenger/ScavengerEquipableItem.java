package me.tuanzi.item.scavenger;

import me.tuanzi.network.scavenger.ScavengerServerPacketHandler;
import me.tuanzi.util.ModLog;
import me.tuanzi.world.scavenger.ScavengerPlayerTracker;
import me.tuanzi.world.scavenger.loot.meta.EquipmentGridSpec;
import me.tuanzi.world.scavenger.profile.PlayerCorridorProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * 拾荒回廊可穿戴装备物品基类（战术背囊 / 安全箱）。
 * 支持在手持时右键直接穿戴到回廊档案装备槽中。
 */
public class ScavengerEquipableItem extends Item {

    public ScavengerEquipableItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (player instanceof ServerPlayer serverPlayer && serverPlayer instanceof ScavengerPlayerTracker tracker) {
            PlayerCorridorProfile profile = tracker.tuanzis_mod$getCorridorProfile();

            // 1. 尝试作为战术背囊穿戴
            Optional<EquipmentGridSpec> bpSpec = PlayerCorridorProfile.resolveEquipmentSpec(stack, EquipmentGridSpec.SlotType.BACKPACK);
            if (bpSpec.isPresent()) {
                if (!profile.hasBackpack()) {
                    if (profile.equipBackpack(stack, level.registryAccess())) {
                        stack.shrink(1);
                        profile.markDirty();
                        ScavengerServerPacketHandler.syncProfile(serverPlayer);
                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.PLAYERS, 1.0f, 1.0f);
                        serverPlayer.sendSystemMessage(Component.literal("§a已装备战术背囊: §f" + stack.getHoverName().getString()), true);
                        ModLog.debug(player, null, "玩家穿戴战术背囊成功: " + stack.getItem());
                        return InteractionResult.SUCCESS;
                    }
                } else {
                    serverPlayer.sendSystemMessage(Component.literal("§c已装备战术背囊，无法重复穿戴！请先在战术整备界面卸下。"), true);
                    return InteractionResult.FAIL;
                }
            }

            // 2. 尝试作为安全箱穿戴
            Optional<EquipmentGridSpec> sbSpec = PlayerCorridorProfile.resolveEquipmentSpec(stack, EquipmentGridSpec.SlotType.SAFEBOX);
            if (sbSpec.isPresent()) {
                if (!profile.hasSafebox()) {
                    if (profile.equipSafebox(stack, level.registryAccess())) {
                        stack.shrink(1);
                        profile.markDirty();
                        ScavengerServerPacketHandler.syncProfile(serverPlayer);
                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.PLAYERS, 1.0f, 1.0f);
                        serverPlayer.sendSystemMessage(Component.literal("§6已装备安全箱: §f" + stack.getHoverName().getString()), true);
                        ModLog.debug(player, null, "玩家穿戴安全箱成功: " + stack.getItem());
                        return InteractionResult.SUCCESS;
                    }
                } else {
                    serverPlayer.sendSystemMessage(Component.literal("§c已装备安全箱，无法重复穿戴！请先在战术整备界面卸下。"), true);
                    return InteractionResult.FAIL;
                }
            }
        }

        return super.use(level, player, hand);
    }
}
