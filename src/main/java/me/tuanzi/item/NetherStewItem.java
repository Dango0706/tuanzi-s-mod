package me.tuanzi.item;

import me.tuanzi.init.ModStatusEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class NetherStewItem extends Item {
    public NetherStewItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        ItemStack result = super.finishUsingItem(stack, level, user);
        
        if (!level.isClientSide()) {
            // 赋予三个状态效果，持续时间 1分30秒 (90秒 = 1800 ticks)
            user.addEffect(new MobEffectInstance(ModStatusEffects.HELL_FIRE, 1800, 0));
            user.addEffect(new MobEffectInstance(ModStatusEffects.BLAZING_HUNGER, 1800, 0));
            user.addEffect(new MobEffectInstance(ModStatusEffects.HYDROPHOBIA, 1800, 0));
        }

        // 吃完后返回一个碗。此物品可以堆叠至 64 个，吃完一个后 stack 数量 - 1。
        if (user instanceof Player player && !player.getAbilities().instabuild) {
            ItemStack bowl = new ItemStack(Items.BOWL);
            if (result.isEmpty()) {
                return bowl;
            }
            if (!player.getInventory().add(bowl)) {
                player.drop(bowl, false);
            }
        }
        return result;
    }
}
