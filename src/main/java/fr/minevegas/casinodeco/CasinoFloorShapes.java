package fr.minevegas.casinodeco;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.references.BlockItemId;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public final class CasinoFloorShapes {
   private static final Map<String, List<Item>> ITEMS = new HashMap();

   private CasinoFloorShapes() {
   }

   public static void init() {
   }

   private static Block material(String name) {
      return BuiltInRegistries.BLOCK.getValue(CasinoDecoMod.id(name));
   }

   private static void family(String name) {
      Supplier<BlockBehaviour.Properties> properties = () -> Properties.ofFullCopy(material(name));
      register(name, "stairs", (p) -> new CasinoFloorStairsBlock(material(name).defaultBlockState(), p), properties);
      register(name, "slab", SlabBlock::new, properties);
   }

   private static void register(String family, String shape, Function<BlockBehaviour.Properties, Block> factory, Supplier<BlockBehaviour.Properties> properties) {
      BlockItemId keys = BlockItemId.create(CasinoDecoMod.id(family + "_" + shape), CasinoDecoMod.id(family + "_" + shape));
      Block block = (Block)Registry.register(BuiltInRegistries.BLOCK, (ResourceKey)keys.block(), (Block)factory.apply(((BlockBehaviour.Properties)properties.get()).setId(keys.block())));
      Item item = (Item)Registry.register(BuiltInRegistries.ITEM, (ResourceKey)keys.item(), new BlockItem(block, (new Item.Properties()).useBlockDescriptionPrefix().setId(keys.item())));
      ((List)ITEMS.computeIfAbsent(family, (k) -> new ArrayList())).add(item);
   }

   static void creativeVariants(Item floor, Consumer<ItemLike> output) {
      List<Item> variants = (List)ITEMS.get(BuiltInRegistries.ITEM.getKey(floor).getPath());
      if (variants != null) {
         Objects.requireNonNull(output);
         variants.forEach(output::accept);
      }

   }

   static {
      for(String colour : new String[]{"ruby", "emerald", "sapphire"}) {
         family(colour + "_velvet");
      }

      for(String floor : new String[]{"ivory_black_checkers", "ivory_marble", "obsidian_marble", "gold_veined_black_marble"}) {
         family(floor);
      }

   }
}
