package fr.minevegas.casinodeco;

import java.util.function.Consumer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.references.BlockItemId;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.PushReaction;

public final class CasinoDecoMod {
   private static final BlockItemId KEYS = BlockItemId.create(id("casino_sofa"), id("casino_sofa"));
   public static final CasinoSofaBlock SOFA;
   public static final Item SOFA_ITEM;
   private static final BlockItemId SIGN_KEYS;
   public static final LargeCasinoSignBlock SIGN;
   public static final Item SIGN_ITEM;
   private static final BlockItemId SMALL_SIGN_KEYS;
   public static final CasinoSignBlock SMALL_SIGN;
   public static final Item SMALL_SIGN_ITEM;
   private static final ResourceKey<EntityType<?>> SEAT_KEY;
   public static final EntityType<SofaSeatEntity> SEAT;
   private static final BlockItemId WOOD_COFFEE_KEYS;
   public static final CasinoTableBlock WOOD_COFFEE_TABLE;
   public static final Item WOOD_COFFEE_TABLE_ITEM;
   private static final BlockItemId GLASS_COFFEE_KEYS;
   public static final CasinoTableBlock GLASS_COFFEE_TABLE;
   public static final Item GLASS_COFFEE_TABLE_ITEM;
   private static final BlockItemId WOOD_TABLE_KEYS;
   public static final CasinoTableBlock WOOD_TABLE;
   public static final Item WOOD_TABLE_ITEM;
   private static final BlockItemId GLASS_TABLE_KEYS;
   public static final CasinoTableBlock GLASS_TABLE;
   public static final Item GLASS_TABLE_ITEM;
   private static final BlockItemId CHANDELIER_KEYS;
   public static final CasinoChandelierBlock CHANDELIER;
   public static final Item CHANDELIER_ITEM;
   private static final BlockItemId ROPE_POST_KEYS;
   public static final VelvetRopePostBlock ROPE_POST;
   public static final Item ROPE_POST_ITEM;

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath("casinodeco", path);
   }

   private CasinoDecoMod() {
   }

   public static void init() {
      CasinoFloorsAndWalls.init();
      CasinoFloorShapes.init();
      CasinoBarAndDecor.init();
   }

   public static void functionalBlocks(Consumer<ItemLike> output) {
      output.accept(SOFA_ITEM);
      output.accept(WOOD_COFFEE_TABLE_ITEM);
      output.accept(GLASS_COFFEE_TABLE_ITEM);
      output.accept(WOOD_TABLE_ITEM);
      output.accept(GLASS_TABLE_ITEM);
      output.accept(CHANDELIER_ITEM);
      output.accept(ROPE_POST_ITEM);
      CasinoBarAndDecor.creative(output);
      output.accept(SIGN_ITEM);
      output.accept(SMALL_SIGN_ITEM);
   }

   public static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
      chunk.findBlocks((state) -> state.is(SOFA) || state.is(SIGN) || state.getBlock() instanceof CasinoPilasterBlock, (pos, state) -> level.scheduleTick(pos, state.getBlock(), 1));
   }

   static {
      SOFA = (CasinoSofaBlock)Registry.register(BuiltInRegistries.BLOCK, (ResourceKey)KEYS.block(), new CasinoSofaBlock(Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(2.5F).noOcclusion().pushReaction(PushReaction.IMMOVEABLE).setId(KEYS.block())));
      SOFA_ITEM = (Item)Registry.register(BuiltInRegistries.ITEM, (ResourceKey)KEYS.item(), new BlockItem(SOFA, (new Item.Properties()).useBlockDescriptionPrefix().setId(KEYS.item())));
      SIGN_KEYS = BlockItemId.create(id("casino_sign"), id("casino_sign"));
      SIGN = (LargeCasinoSignBlock)Registry.register(BuiltInRegistries.BLOCK, (ResourceKey)SIGN_KEYS.block(), new LargeCasinoSignBlock(Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(1.5F).noOcclusion().lightLevel((state) -> 11).pushReaction(PushReaction.IMMOVEABLE).setId(SIGN_KEYS.block())));
      SIGN_ITEM = (Item)Registry.register(BuiltInRegistries.ITEM, (ResourceKey)SIGN_KEYS.item(), new BlockItem(SIGN, (new Item.Properties()).useBlockDescriptionPrefix().setId(SIGN_KEYS.item())));
      SMALL_SIGN_KEYS = BlockItemId.create(id("small_casino_sign"), id("small_casino_sign"));
      SMALL_SIGN = (CasinoSignBlock)Registry.register(BuiltInRegistries.BLOCK, (ResourceKey)SMALL_SIGN_KEYS.block(), new CasinoSignBlock(Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(1.5F).noOcclusion().lightLevel((state) -> 7).pushReaction(PushReaction.IMMOVEABLE).setId(SMALL_SIGN_KEYS.block())));
      SMALL_SIGN_ITEM = (Item)Registry.register(BuiltInRegistries.ITEM, (ResourceKey)SMALL_SIGN_KEYS.item(), new BlockItem(SMALL_SIGN, (new Item.Properties()).useBlockDescriptionPrefix().setId(SMALL_SIGN_KEYS.item())));
      SEAT_KEY = ResourceKey.create(Registries.ENTITY_TYPE, id("sofa_seat"));
      SEAT = (EntityType)Registry.register(BuiltInRegistries.ENTITY_TYPE, (ResourceKey)SEAT_KEY, Builder.of(SofaSeatEntity::new, MobCategory.MISC).sized(0.1F, 0.1F).noSummon().noLootTable().fireImmune().clientTrackingRange(8).updateInterval(20).build(SEAT_KEY));
      WOOD_COFFEE_KEYS = BlockItemId.create(id("wood_coffee_table"), id("wood_coffee_table"));
      WOOD_COFFEE_TABLE = (CasinoTableBlock)Registry.register(BuiltInRegistries.BLOCK, (ResourceKey)WOOD_COFFEE_KEYS.block(), new CasinoTableBlock(Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(1.5F).noOcclusion().pushReaction(PushReaction.IMMOVEABLE).setId(WOOD_COFFEE_KEYS.block()), DecorGeometry.TABLE_WOOD_LOW));
      WOOD_COFFEE_TABLE_ITEM = (Item)Registry.register(BuiltInRegistries.ITEM, (ResourceKey)WOOD_COFFEE_KEYS.item(), new BlockItem(WOOD_COFFEE_TABLE, (new Item.Properties()).useBlockDescriptionPrefix().setId(WOOD_COFFEE_KEYS.item())));
      GLASS_COFFEE_KEYS = BlockItemId.create(id("glass_coffee_table"), id("glass_coffee_table"));
      GLASS_COFFEE_TABLE = (CasinoTableBlock)Registry.register(BuiltInRegistries.BLOCK, (ResourceKey)GLASS_COFFEE_KEYS.block(), new CasinoTableBlock(Properties.ofFullCopy(Blocks.GLASS).strength(0.6F).noOcclusion().pushReaction(PushReaction.IMMOVEABLE).setId(GLASS_COFFEE_KEYS.block()), DecorGeometry.TABLE_GLASS_LOW));
      GLASS_COFFEE_TABLE_ITEM = (Item)Registry.register(BuiltInRegistries.ITEM, (ResourceKey)GLASS_COFFEE_KEYS.item(), new BlockItem(GLASS_COFFEE_TABLE, (new Item.Properties()).useBlockDescriptionPrefix().setId(GLASS_COFFEE_KEYS.item())));
      WOOD_TABLE_KEYS = BlockItemId.create(id("wood_table"), id("wood_table"));
      WOOD_TABLE = (CasinoTableBlock)Registry.register(BuiltInRegistries.BLOCK, (ResourceKey)WOOD_TABLE_KEYS.block(), new CasinoTableBlock(Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(1.5F).noOcclusion().forceSolidOn().pushReaction(PushReaction.IMMOVEABLE).setId(WOOD_TABLE_KEYS.block()), DecorGeometry.TABLE_WOOD_TALL));
      WOOD_TABLE_ITEM = (Item)Registry.register(BuiltInRegistries.ITEM, (ResourceKey)WOOD_TABLE_KEYS.item(), new BlockItem(WOOD_TABLE, (new Item.Properties()).useBlockDescriptionPrefix().setId(WOOD_TABLE_KEYS.item())));
      GLASS_TABLE_KEYS = BlockItemId.create(id("glass_table"), id("glass_table"));
      GLASS_TABLE = (CasinoTableBlock)Registry.register(BuiltInRegistries.BLOCK, (ResourceKey)GLASS_TABLE_KEYS.block(), new CasinoTableBlock(Properties.ofFullCopy(Blocks.GLASS).strength(0.6F).noOcclusion().forceSolidOn().pushReaction(PushReaction.IMMOVEABLE).setId(GLASS_TABLE_KEYS.block()), DecorGeometry.TABLE_GLASS_TALL));
      GLASS_TABLE_ITEM = (Item)Registry.register(BuiltInRegistries.ITEM, (ResourceKey)GLASS_TABLE_KEYS.item(), new BlockItem(GLASS_TABLE, (new Item.Properties()).useBlockDescriptionPrefix().setId(GLASS_TABLE_KEYS.item())));
      CHANDELIER_KEYS = BlockItemId.create(id("casino_chandelier"), id("casino_chandelier"));
      CHANDELIER = (CasinoChandelierBlock)Registry.register(BuiltInRegistries.BLOCK, (ResourceKey)CHANDELIER_KEYS.block(), new CasinoChandelierBlock(Properties.ofFullCopy(Blocks.OAK_PLANKS).sound(SoundType.LANTERN).strength(1.0F).noOcclusion().lightLevel((state) -> (Integer)state.getValue(CasinoChandelierBlock.PART) < 9 ? 14 : 0).pushReaction(PushReaction.IMMOVEABLE).setId(CHANDELIER_KEYS.block())));
      CHANDELIER_ITEM = (Item)Registry.register(BuiltInRegistries.ITEM, (ResourceKey)CHANDELIER_KEYS.item(), new BlockItem(CHANDELIER, (new Item.Properties()).useBlockDescriptionPrefix().setId(CHANDELIER_KEYS.item())));
      ROPE_POST_KEYS = BlockItemId.create(id("velvet_rope_post"), id("velvet_rope_post"));
      ROPE_POST = (VelvetRopePostBlock)Registry.register(BuiltInRegistries.BLOCK, (ResourceKey)ROPE_POST_KEYS.block(), new VelvetRopePostBlock(Properties.ofFullCopy(Blocks.OAK_PLANKS).sound(SoundType.METAL).strength(1.5F).noOcclusion().pushReaction(PushReaction.IMMOVEABLE).setId(ROPE_POST_KEYS.block())));
      ROPE_POST_ITEM = (Item)Registry.register(BuiltInRegistries.ITEM, (ResourceKey)ROPE_POST_KEYS.item(), new BlockItem(ROPE_POST, (new Item.Properties()).useBlockDescriptionPrefix().setId(ROPE_POST_KEYS.item())));
   }
}
