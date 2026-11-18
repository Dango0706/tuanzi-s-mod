package fr.minevegas.casino;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import fr.minevegas.casino.platform.Services;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.slf4j.Logger;

public final class CasinoBetRules extends SavedData {
   public static final int NOT_A_BET = 0;
   public static final int BET = 1;
   public static final int DEFAULTS = 2;
   private static final Logger LOGGER = LogUtils.getLogger();
   static final Codec<CasinoBetRules> CODEC = RecordCodecBuilder.<CasinoBetRules>create((instance) -> instance.group(Identifier.CODEC.listOf().optionalFieldOf("bets", List.of()).forGetter((rules) -> List.copyOf(rules.bets)), Identifier.CODEC.listOf().optionalFieldOf("not_bets", List.of()).forGetter((rules) -> List.copyOf(rules.notBets)), Codec.STRING.listOf().optionalFieldOf("machine_settings", List.of()).forGetter((rules) -> pieces(rules.machineSettings()))).apply(instance, CasinoBetRules::new));
   private static final int PIECE = 20000;
   private static final SavedDataType<CasinoBetRules> TYPE;
   private static final AtomicInteger VERSIONS;
   private static volatile Snapshot current;
   private static CasinoBetRules loaded;
   private final Set<Identifier> bets = new LinkedHashSet();
   private final Set<Identifier> notBets = new LinkedHashSet();
   private String machineSettings = "{}";
   private boolean stale;

   private CasinoBetRules() {
   }

   private CasinoBetRules(List<Identifier> bets, List<Identifier> notBets, List<String> machineSettings) {
      this.machineSettings = machineSettings.isEmpty() ? "{}" : String.join("", machineSettings);
      this.bets.addAll(bets);
      this.notBets.addAll(notBets);
   }

   private String machineSettings() {
      if (this.stale && this == loaded) {
         this.machineSettings = CasinoSettings.saveWorld();
         this.stale = false;
      }

      return this.machineSettings;
   }

   private static List<String> pieces(String settings) {
      if (settings.equals("{}")) {
         return List.of();
      } else {
         List<String> pieces = new ArrayList();

         for(int i = 0; i < settings.length(); i += 20000) {
            pieces.add(settings.substring(i, Math.min(settings.length(), i + 20000)));
         }

         return pieces;
      }
   }

   public static Snapshot current() {
      return current;
   }

   static void load(MinecraftServer server) {
      loaded = (CasinoBetRules)server.overworld().getDataStorage().computeIfAbsent(TYPE);
      CasinoSettings.loadWorld(loaded.machineSettings);
      current = snapshot(loaded.bets, loaded.notBets);
   }

   static void unload() {
      loaded = null;
      CasinoSettings.reset();
      current = snapshot(List.of(), List.of());
   }

   static void sendTo(ServerPlayer player) {
      if (loaded != null) {
         Services.PLATFORM.sendToPlayer(player, loaded.payload());
         CasinoSettings.sendWorldTo(player);
      }

   }

   static void edit(ServerPlayer player, EditPayload edit) {
      if (loaded != null) {
         boolean allowed = player.isCreative() && (player.getMainHandItem().is(CroupierHandItem.ITEM) || player.getOffhandItem().is(CroupierHandItem.ITEM));
         if (allowed && loaded.apply(edit)) {
            loaded.setDirty();
            current = snapshot(loaded.bets, loaded.notBets);
            RulesPayload payload = loaded.payload();

            for(ServerPlayer each : player.level().getServer().getPlayerList().getPlayers()) {
               Services.PLATFORM.sendToPlayer(each, payload);
            }

            String var10000;
            switch (edit.action()) {
               case 0 -> var10000 = String.valueOf(edit.item()) + " is no longer a bet";
               case 1 -> var10000 = String.valueOf(edit.item()) + " is now a bet";
               default -> var10000 = "the bets are the casino's own again";
            }

            String what = var10000;
            LOGGER.info((String)"[MineVegas Casino] {} (croupier's hand): {}", (Object)player.getName().getString(), (Object)what);
         } else {
            Services.PLATFORM.sendToPlayer(player, loaded.payload());
         }
      }
   }

   private boolean apply(EditPayload edit) {
      if (edit.action() == 2) {
         boolean changed = !this.bets.isEmpty() || !this.notBets.isEmpty();
         this.bets.clear();
         this.notBets.clear();
         return changed;
      } else {
         Item item = (Item)BuiltInRegistries.ITEM.getOptional(edit.item()).orElse(Items.AIR);
         if (item != Items.AIR && item != CroupierHandItem.ITEM) {
            boolean byTags = CasinoBets.acceptsByTags(new ItemStack(item));
            boolean var10000;
            switch (edit.action()) {
               case 0 -> var10000 = this.bets.remove(edit.item()) | (byTags && this.notBets.add(edit.item()));
               case 1 -> var10000 = this.notBets.remove(edit.item()) | (!byTags && this.bets.add(edit.item()));
               default -> var10000 = false;
            }

            return var10000;
         } else {
            return false;
         }
      }
   }

   static void settingsChanged() {
      if (loaded != null) {
         loaded.stale = true;
         loaded.setDirty();
      }

   }

   private RulesPayload payload() {
      return new RulesPayload(List.copyOf(this.bets), List.copyOf(this.notBets));
   }

   static void receive(RulesPayload payload) {
      current = snapshot(payload.bets(), payload.notBets());
   }

   static void forget() {
      CasinoSettings.forget();
      current = snapshot(List.of(), List.of());
   }

   private static Snapshot snapshot(Collection<Identifier> bets, Collection<Identifier> notBets) {
      List<Item> inOrder = new ArrayList();

      for(Identifier id : bets) {
         Optional<Item> var10000 = BuiltInRegistries.ITEM.getOptional(id).filter((item) -> item != Items.AIR);
         Objects.requireNonNull(inOrder);
         var10000.ifPresent(inOrder::add);
      }

      Set<Item> not = new HashSet();

      for(Identifier id : notBets) {
         Optional<Item> var8 = BuiltInRegistries.ITEM.getOptional(id).filter((item) -> item != Items.AIR);
         Objects.requireNonNull(not);
         var8.ifPresent(not::add);
      }

      return new Snapshot(Set.copyOf(inOrder), Set.copyOf(not), List.copyOf(inOrder), VERSIONS.incrementAndGet());
   }

   static {
      TYPE = new SavedDataType<CasinoBetRules>(RouletteMod.id("bet_rules"), CasinoBetRules::new, CODEC, DataFixTypes.LEVEL);
      VERSIONS = new AtomicInteger();
      current = new Snapshot(Set.of(), Set.of(), List.of(), 0);
   }

   public static record Snapshot(Set<Item> bets, Set<Item> notBets, List<Item> betsInOrder, int version) {
   }

   public static record RulesPayload(List<Identifier> bets, List<Identifier> notBets) implements CustomPacketPayload {
      public static final CustomPacketPayload.Type<RulesPayload> TYPE = new CustomPacketPayload.Type<RulesPayload>(RouletteMod.id("bet_rules"));
      public static final StreamCodec<RegistryFriendlyByteBuf, RulesPayload> CODEC;

      public CustomPacketPayload.Type<RulesPayload> type() {
         return TYPE;
      }

      static {
         CODEC = StreamCodec.composite(Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()), RulesPayload::bets, Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()), RulesPayload::notBets, RulesPayload::new);
      }
   }

   public static record EditPayload(int action, Identifier item) implements CustomPacketPayload {
      public static final CustomPacketPayload.Type<EditPayload> TYPE = new CustomPacketPayload.Type<EditPayload>(RouletteMod.id("bet_rules_edit"));
      public static final StreamCodec<RegistryFriendlyByteBuf, EditPayload> CODEC;

      public CustomPacketPayload.Type<EditPayload> type() {
         return TYPE;
      }

      static {
         CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, EditPayload::action, Identifier.STREAM_CODEC, EditPayload::item, EditPayload::new);
      }
   }
}


