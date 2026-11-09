package me.tuanzi.util;

import me.tuanzi.init.ModEnchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.equine.Donkey;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.equine.Mule;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class HusbandryHelper {

    /**
     * 判断是否属于牧养附魔生效的可繁殖动物：
     * 牛、羊、猪、鸡、兔、马、驴、骡、羊驼、山羊
     */
    public static boolean isApplicableAnimal(Animal animal) {
        return animal instanceof Cow
            || animal instanceof Sheep
            || animal instanceof Pig
            || animal instanceof Chicken
            || animal instanceof Rabbit
            || animal instanceof Horse
            || animal instanceof Donkey
            || animal instanceof Mule
            || animal instanceof Llama
            || animal instanceof Goat;
    }

    /**
     * 获取玩家护腿上的牧养附魔等级。
     */
    public static int getHusbandryLevel(ServerPlayer player) {
        if (player == null) return 0;
        ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
        if (legs.isEmpty()) return 0;
        var enchRegistry = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var husbandryHolder = enchRegistry.getOrThrow(ModEnchantments.HUSBANDRY);
        return EnchantmentHelper.getItemEnchantmentLevel(husbandryHolder, legs);
    }

    /**
     * 玩家 tick 时调用（每 10 ticks = 0.5s 执行一次），加速 8 格内适用的幼崽成长速度。
     * 加速 +10%/20%/30%：每 10 ticks 额外增加 1/2/3 ticks 成长。
     */
    public static void tickBabyGrowth(ServerPlayer player) {
        int level = getHusbandryLevel(player);
        if (level <= 0) return;

        AABB box = player.getBoundingBox().inflate(8.0);
        List<Animal> animals = player.level().getEntitiesOfClass(Animal.class, box, animal -> animal.isBaby() && isApplicableAnimal(animal));
        for (Animal baby : animals) {
            int currentAge = baby.getAge();
            if (currentAge < 0) {
                int newAge = currentAge + level; // 额外增加 1/2/3 ticks
                baby.setAge(newAge);
                ModLog.debug(player, "【牧养】加速幼崽 %s 成长（年龄进度: %d -> %d, 等级: %d）",
                    baby.getType().getDescription().getString(), currentAge, newAge, level);
            }
        }
    }
}
