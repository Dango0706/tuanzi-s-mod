package me.tuanzi.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public class ImmortalTalismanItem extends Item {
    public ImmortalTalismanItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.RARE));
    }
}
