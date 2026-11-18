package fr.minevegas.casinodeco;

import fr.minevegas.casino.platform.Services;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public final class CasinoBarAndDecor {
   private static final List<Item> ITEMS = new ArrayList();
   public static final CasinoBarBlock WALNUT_BAR;
   public static final CasinoBarBlock IVORY_MARBLE_BAR;
   public static final CasinoBarBlock BLACK_MARBLE_BAR;
   public static final CasinoWallSconceBlock WALL_SCONCE;
   public static final CasinoStackedDecorBlock BOTTLES;
   public static final CasinoStackedDecorBlock CHAMPAGNE_COUPES;
   public static final CasinoStackedDecorBlock SHOT_GLASSES;
   public static final CasinoDecorBlock CHIP_PILE;
   public static final CasinoChipCaseBlock CHIP_CASE;
   public static final CasinoDecorBlock RECEPTION_PLAQUE;
   public static final CasinoDecorBlock CASH_REGISTER;
   public static final CasinoDecorBlock VIP_ONLY_PLAQUE;
   public static final CasinoDecorBlock BOTTLE_DISPLAY_SHELF;
   public static final CasinoDecorBlock GLASS_DISPLAY_SHELF;
   public static final CasinoDecorBlock SERVING_TRAY;

   private CasinoBarAndDecor() {
   }

   private static BlockBehaviour.Properties glass() {
      return Properties.ofFullCopy(Blocks.GLASS).strength(0.3F).sound(SoundType.GLASS).noOcclusion().pushReaction(PushReaction.POPPED);
   }

   private static BlockBehaviour.Properties small(Block material, SoundType sound) {
      return Properties.ofFullCopy(material).strength(0.8F).sound(sound).noOcclusion().pushReaction(PushReaction.POPPED);
   }

   private static <B extends Block> B register(String name, Function<BlockBehaviour.Properties, B> factory, BlockBehaviour.Properties properties) {
      BlockItemId keys = BlockItemId.create(CasinoDecoMod.id(name), CasinoDecoMod.id(name));
      B block = (B)(Registry.register(BuiltInRegistries.BLOCK, (ResourceKey)keys.block(), (Block)factory.apply(properties.setId(keys.block()))));
      ITEMS.add((Item)Registry.register(BuiltInRegistries.ITEM, (ResourceKey)keys.item(), new BlockItem(block, (new Item.Properties()).useBlockDescriptionPrefix().setId(keys.item()))));
      return block;
   }

   public static void init() {
   }

   public static void creative(Consumer<ItemLike> output) {
      List<Item> var10000 = ITEMS;
      Objects.requireNonNull(output);
      var10000.forEach(output::accept);
   }

   static {
      WALNUT_BAR = (CasinoBarBlock)register("walnut_bar", (p) -> new CasinoBarBlock(p, true), Properties.ofFullCopy(Blocks.DARK_OAK_PLANKS).strength(2.0F).noOcclusion().mapColor(MapColor.COLOR_BROWN));
      IVORY_MARBLE_BAR = (CasinoBarBlock)register("ivory_marble_bar", (p) -> new CasinoBarBlock(p, false), Properties.ofFullCopy(Blocks.POLISHED_ANDESITE).noOcclusion().mapColor(MapColor.QUARTZ));
      BLACK_MARBLE_BAR = (CasinoBarBlock)register("black_marble_bar", (p) -> new CasinoBarBlock(p, false), Properties.ofFullCopy(Blocks.POLISHED_ANDESITE).noOcclusion().mapColor(MapColor.COLOR_BLACK));
      WALL_SCONCE = (CasinoWallSconceBlock)register("chandelier_wall_sconce", CasinoWallSconceBlock::new, Properties.ofFullCopy(Blocks.OAK_PLANKS).sound(SoundType.LANTERN).strength(1.0F).noOcclusion().noCollision().lightLevel((state) -> (Boolean)state.getValue(CasinoWallSconceBlock.WATERLOGGED) ? 0 : 14).pushReaction(PushReaction.POPPED));
      BOTTLES = (CasinoStackedDecorBlock)register("casino_bottles", (p) -> new CasinoStackedDecorBlock(p, CasinoDecorGeometry.CASINO_BOTTLES), glass());
      CHAMPAGNE_COUPES = (CasinoStackedDecorBlock)register("champagne_coupes", (p) -> new CasinoStackedDecorBlock(p, CasinoDecorGeometry.CHAMPAGNE_COUPES), glass());
      SHOT_GLASSES = (CasinoStackedDecorBlock)register("shot_glasses", (p) -> new CasinoStackedDecorBlock(p, CasinoDecorGeometry.SHOT_GLASSES), glass());
      CHIP_PILE = (CasinoDecorBlock)register("casino_chip_pile", (p) -> new CasinoDecorBlock(p, CasinoDecorGeometry.CASINO_CHIP_PILE, false), Properties.ofFullCopy(Blocks.FLOWER_POT).strength(0.2F).sound(SoundType.STONE));
      CHIP_CASE = (CasinoChipCaseBlock)register("chip_case", CasinoChipCaseBlock::new, small(Blocks.CHEST, SoundType.WOOD));
      RECEPTION_PLAQUE = (CasinoDecorBlock)register("reception_plaque", (p) -> new CasinoDecorBlock(p, CasinoDecorGeometry.RECEPTION_PLAQUE, false), small(Blocks.DARK_OAK_PLANKS, SoundType.METAL));
      CASH_REGISTER = (CasinoDecorBlock)register("cash_register", (p) -> new CasinoDecorBlock(p, CasinoDecorGeometry.CASH_REGISTER, false), small(Blocks.DARK_OAK_PLANKS, SoundType.WOOD));
      VIP_ONLY_PLAQUE = (CasinoDecorBlock)register("vip_only_plaque", (p) -> new CasinoDecorBlock(p, CasinoDecorGeometry.VIP_ONLY_PLAQUE, true), small(Blocks.DARK_OAK_PLANKS, SoundType.METAL).noCollision());
      BOTTLE_DISPLAY_SHELF = (CasinoDecorBlock)register("bottle_display_shelf", (p) -> new CasinoDecorBlock(p, CasinoDecorGeometry.BOTTLE_DISPLAY_SHELF, true), small(Blocks.CHEST, SoundType.WOOD));
      GLASS_DISPLAY_SHELF = (CasinoDecorBlock)register("glass_display_shelf", (p) -> new CasinoDecorBlock(p, CasinoDecorGeometry.GLASS_DISPLAY_SHELF, true), small(Blocks.CHEST, SoundType.WOOD));
      SERVING_TRAY = (CasinoDecorBlock)register("serving_tray", (p) -> new CasinoDecorBlock(p, CasinoDecorGeometry.SERVING_TRAY, false), small(Blocks.CHEST, SoundType.METAL));
      Services.PLATFORM.flammable(WALNUT_BAR, 5, 20);
   }
}

