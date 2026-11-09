package me.tuanzi.init;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class ModBlockTags {
    public static final TagKey<Block> SNOW_STRIDER_BLOCKS = TagKey.create(
        Registries.BLOCK, Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "snow_strider_blocks")
    );
}
