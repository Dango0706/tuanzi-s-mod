package me.tuanzi.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public class BerserkCharmItem extends Item {
    public BerserkCharmItem(Properties properties) {
        super(properties.stacksTo(16).rarity(Rarity.UNCOMMON));
    }
}
