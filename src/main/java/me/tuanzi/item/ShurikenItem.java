package me.tuanzi.item;

import me.tuanzi.entity.ShurikenEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ShurikenItem extends Item {
    public ShurikenItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        
        boolean isSneaking = player.isShiftKeyDown();
        int required = isSneaking ? 3 : 1;
        
        boolean hasEnough = player.getAbilities().instabuild || countShurikens(player) >= required;
        if (!hasEnough) {
            return InteractionResult.FAIL;
        }

        if (!level.isClientSide()) {
            if (!player.getAbilities().instabuild) {
                consumeShurikens(player, required);
            }

            float speed = 4.5f; // 1.5倍箭速
            
            if (isSneaking) {
                // 潜行+右键：扇形三连发
                throwShuriken(level, player, 0.0f, speed);
                throwShuriken(level, player, -15.0f, speed);
                throwShuriken(level, player, 15.0f, speed);

                level.playSound(null, player.getX(), player.getY(), player.getZ(), 
                    SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.6F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
                
                // 12 ticks (0.6 秒) 冷却
                player.getCooldowns().addCooldown(itemStack, 12);
            } else {
                // 普通单发
                throwShuriken(level, player, 0.0f, speed);

                level.playSound(null, player.getX(), player.getY(), player.getZ(), 
                    SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
                
                // 4 ticks (0.2 秒) 冷却
                player.getCooldowns().addCooldown(itemStack, 4);
            }
            
            player.awardStat(Stats.ITEM_USED.get(this));
        }

        player.swing(hand);
        return InteractionResult.SUCCESS.heldItemTransformedTo(itemStack);
    }

    private void throwShuriken(Level level, Player player, float yawOffset, float speed) {
        ShurikenEntity shuriken = new ShurikenEntity(level, player, new ItemStack(this));
        shuriken.shootFromRotation(player, player.getXRot(), player.getYRot() + yawOffset, 0.0f, speed, 0.5f);
        level.addFreshEntity(shuriken);
    }

    private int countShurikens(Player player) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(this)) {
                count += stack.getCount();
            }
        }
        ItemStack offhandStack = player.getOffhandItem();
        if (offhandStack.is(this)) {
            count += offhandStack.getCount();
        }
        return count;
    }

    private void consumeShurikens(Player player, int amount) {
        int remaining = amount;
        
        // 优先从副手扣除
        ItemStack offhandStack = player.getOffhandItem();
        if (offhandStack.is(this)) {
            int take = Math.min(remaining, offhandStack.getCount());
            offhandStack.shrink(take);
            remaining -= take;
            if (remaining <= 0) return;
        }
        
        // 再从主手及背包扣除
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.is(this)) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
                if (remaining <= 0) return;
            }
        }
    }
}
