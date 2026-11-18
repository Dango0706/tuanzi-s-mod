package fr.minevegas.casinosound;

import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.references.BlockItemId;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.PushReaction;

public final class CasinoSoundMod {
   private static final BlockItemId KEYS = BlockItemId.create(id("casino_speaker"), id("casino_speaker"));
   public static final SpeakerBlock SPEAKER;
   public static final Item SPEAKER_ITEM;
   public static final BlockEntityType<SpeakerBlockEntity> SPEAKER_ENTITY;
   public static final SoundEvent[] TRACKS;

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath("casinosound", path);
   }

   private CasinoSoundMod() {
   }

   public static void init() {
      for(int i = 0; i < TRACKS.length; ++i) {
         TRACKS[i] = (SoundEvent)Registry.register(BuiltInRegistries.SOUND_EVENT, (Identifier)id(MusicTimeline.IDS[i]), SoundEvent.createVariableRangeEvent(id(MusicTimeline.IDS[i])));
      }

   }

   public static void functionalBlocks(Consumer<ItemLike> out) {
      out.accept(SPEAKER_ITEM);
   }

   public static void onLevelTick(ServerLevel level) {
      if (level.getGameTime() % 100L == 0L) {
         CasinoStation.get(level).pruneLoaded(level);
      }

   }

   static {
      SPEAKER = (SpeakerBlock)Registry.register(BuiltInRegistries.BLOCK, (ResourceKey)KEYS.block(), new SpeakerBlock(Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(2.5F).noOcclusion().pushReaction(PushReaction.IMMOVEABLE).lightLevel((s) -> (Boolean)s.getValue(SpeakerBlock.ON) ? 2 : 0).setId(KEYS.block())));
      SPEAKER_ITEM = (Item)Registry.register(BuiltInRegistries.ITEM, (ResourceKey)KEYS.item(), new BlockItem(SPEAKER, (new Item.Properties()).useBlockDescriptionPrefix().setId(KEYS.item())));
      SPEAKER_ENTITY = (BlockEntityType)Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, (ResourceKey)ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, id("casino_speaker")), new BlockEntityType(SpeakerBlockEntity::new, Set.of(SPEAKER)));
      TRACKS = new SoundEvent[MusicTimeline.IDS.length];
   }
}
