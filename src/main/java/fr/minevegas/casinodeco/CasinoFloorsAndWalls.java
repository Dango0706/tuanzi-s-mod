package fr.minevegas.casinodeco;

import fr.minevegas.casino.platform.Services;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.references.BlockItemId;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;

public final class CasinoFloorsAndWalls {
   private static final String[] PANELING = new String[]{"dark_oak_paneling", "gilded_dark_oak_paneling", "dark_oak_baseboard", "dark_oak_cornice", "dark_oak_dado_rail", "dark_oak_pilaster", "dark_oak_diamond_panel"};
   private static final List<Item> ITEMS = new ArrayList();
   private static final Map<Block, Integer> BANDS = new HashMap();

   private CasinoFloorsAndWalls() {
   }

   private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
      BlockItemId keys = BlockItemId.create(CasinoDecoMod.id(name), CasinoDecoMod.id(name));
      Block block = (Block)Registry.register(BuiltInRegistries.BLOCK, (ResourceKey)keys.block(), (Block)factory.apply(properties.setId(keys.block())));
      ITEMS.add((Item)Registry.register(BuiltInRegistries.ITEM, (ResourceKey)keys.item(), new BlockItem(block, (new Item.Properties()).useBlockDescriptionPrefix().setId(keys.item()))));
      return block;
   }

   public static void init() {
   }

   public static void creative(Consumer<ItemLike> output) {
      for(Item item : ITEMS) {
         output.accept(item);
         CasinoFloorShapes.creativeVariants(item, output);
      }

   }

   static int band(Block block) {
      return (Integer)BANDS.getOrDefault(block, 0);
   }

   static {
      for(String colour : new String[]{"ruby", "emerald", "sapphire"}) {
         MapColor var10000;
         switch (colour) {
            case "ruby" -> var10000 = MapColor.COLOR_RED;
            case "emerald" -> var10000 = MapColor.COLOR_GREEN;
            default -> var10000 = MapColor.COLOR_BLUE;
         }

         MapColor map = var10000;

         for(String pattern : new String[]{"trellis", "stars", "velvet"}) {
            Function<BlockBehaviour.Properties, Block> factory = pattern.equals("stars") ? CasinoStarCarpetBlock::new : Block::new;
            register(colour + "_" + pattern, factory, Properties.ofFullCopy(Blocks.WOOL.red()).mapColor(map));
         }
      }

      register("ivory_black_checkers", Block::new, Properties.ofFullCopy(Blocks.POLISHED_ANDESITE));
      register("jade_inlay", Block::new, Properties.ofFullCopy(Blocks.POLISHED_ANDESITE).mapColor(MapColor.COLOR_GREEN));
      register("ivory_marble", Block::new, Properties.ofFullCopy(Blocks.POLISHED_ANDESITE).mapColor(MapColor.QUARTZ));
      register("obsidian_marble", Block::new, Properties.ofFullCopy(Blocks.POLISHED_ANDESITE).mapColor(MapColor.COLOR_BLACK));
      register("gold_veined_black_marble", Block::new, Properties.ofFullCopy(Blocks.POLISHED_ANDESITE).mapColor(MapColor.COLOR_BLACK));

      for(String series : new String[]{"", "classic_"}) {
         for(String name : PANELING) {
            if (!series.equals("classic_") || !name.equals("dark_oak_paneling")) {
               Block block = register(series + name, name.equals("dark_oak_pilaster") ? CasinoPilasterBlock::new : CasinoWoodPanelBlock::new, Properties.ofFullCopy(Blocks.DARK_OAK_PLANKS));
               Services.PLATFORM.flammable(block, 5, 20);
               switch (name) {
                  case "dark_oak_baseboard":
                     BANDS.put(block, 1);
                     break;
                  case "dark_oak_cornice":
                     BANDS.put(block, 2);
                     break;
                  case "dark_oak_dado_rail":
                     BANDS.put(block, 3);
               }
            }
         }
      }

   }
}
