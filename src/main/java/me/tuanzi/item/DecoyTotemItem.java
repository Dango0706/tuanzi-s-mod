package me.tuanzi.item;

import me.tuanzi.entity.DecoyEntity;
import me.tuanzi.init.ModEntities;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class DecoyTotemItem extends Item {
    public DecoyTotemItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            Vec3 lookAngle = player.getLookAngle();
            double spawnX = player.getX() + lookAngle.x * 1.0;
            double spawnY = player.getY();
            double spawnZ = player.getZ() + lookAngle.z * 1.0;

            DecoyEntity decoy = ModEntities.DECOY.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
            if (decoy != null) {
                decoy.snapTo(new Vec3(spawnX, spawnY, spawnZ), player.getYRot(), player.getXRot());

                // 1. 复制玩家 GameProfile
                decoy.setProfile(ResolvableProfile.createResolved(player.getGameProfile()));

                // 2. 复制玩家所有装备与主副手持物
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    decoy.setItemSlot(slot, player.getItemBySlot(slot).copy());
                }

                // 3. 复制玩家的血量、最大血量、基础护甲值（穿戴装备与附魔修饰符由 setItemSlot + detectEquipmentUpdates 自动结算）
                var maxHealthAttr = decoy.getAttribute(Attributes.MAX_HEALTH);
                if (maxHealthAttr != null) {
                    maxHealthAttr.setBaseValue(player.getMaxHealth());
                }
                decoy.setHealth(player.getHealth());

                var armorAttr = decoy.getAttribute(Attributes.ARMOR);
                if (armorAttr != null) {
                    armorAttr.setBaseValue(player.getAttributeBaseValue(Attributes.ARMOR));
                }

                level.addFreshEntity(decoy);
            }
        }

        player.getCooldowns().addCooldown(itemStack, 300); // 15秒冷却 (300 ticks)
        if (!player.getAbilities().instabuild) {
            itemStack.shrink(1);
        }

        return InteractionResult.SUCCESS.heldItemTransformedTo(itemStack);
    }
}
