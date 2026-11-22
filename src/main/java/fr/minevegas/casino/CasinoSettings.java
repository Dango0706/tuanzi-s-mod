package fr.minevegas.casino;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import fr.minevegas.casino.platform.Services;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class CasinoSettings {
   private static final int MAX_COPIES = 256;
   private static final int MAX_MACHINES = 4096;
   private static final Map<UUID, Rate> RATES = new HashMap();
   private static final Map<String, Copied> COPIED = new LinkedHashMap();
   private static final Map<Integer, MenuState> MENUS = new ConcurrentHashMap();
   private static final Map<String, Listed> MACHINES = new LinkedHashMap();
   private static int listRevision;
   private static final Map<UUID, Integer> LIST_SENT = new HashMap();
   private static final Map<BlockEntity, Boolean> LISTED = new WeakHashMap();
   private static final AtomicInteger REVISION = new AtomicInteger();
   private static CroupierSettings world;
   private static final Map<String, CroupierSettings> TYPES;
   private static final Map<String, Integer> NUMBERS;
   private static boolean breakable;
   private static volatile CroupierSettings clientWorld;
   private static volatile boolean clientBreakable;
   public static Consumer<View> viewReceiver;

   private CasinoSettings() {
   }

   public static InteractionResult interact(Player player, Level level, BlockPos pos) {
      if (!allowed(player)) {
         if (!level.isClientSide()) {
            player.sendOverlayMessage(Component.translatable("message.roulettecasino.croupier_hand.creative_only"));
         }

         return InteractionResult.FAIL;
      } else {
         BlockEntity machine = CasinoMachine.at(level, pos);
         if (machine == null) {
            return (InteractionResult)(player.isShiftKeyDown() ? InteractionResult.FAIL : InteractionResult.PASS);
         } else {
            if (player instanceof ServerPlayer) {
               ServerPlayer server = (ServerPlayer)player;
               edit(server, new EditPayload("machine", machine.getBlockPos().asLong(), player.isShiftKeyDown() ? "paste" : "open", "", "", 0, player.isShiftKeyDown(), dimensionOf(level)));
            }

            return InteractionResult.SUCCESS;
         }
      }
   }

   public static boolean allowed(Player player) {
      return player != null && player.isCreative() && (player.getMainHandItem().is(CroupierHandItem.ITEM) || player.getOffhandItem().is(CroupierHandItem.ITEM));
   }

   public static int revision() {
      return REVISION.get();
   }

   public static CroupierSettings settings(BlockEntity machine) {
      CroupierSettings var10000;
      if (machine instanceof CasinoConfiguredMachine configured) {
         var10000 = configured.croupierSettings();
      } else {
         var10000 = CroupierSettings.EMPTY;
      }

      return var10000;
   }

   public static CasinoMachineIdentity identify(BlockEntity machine) {
      if (machine instanceof CasinoConfiguredMachine configured) {
         Level var3 = machine.getLevel();
         if (var3 instanceof ServerLevel server) {
            CasinoMachineIdentity var6 = configured.machineIdentity();
            String type = CasinoMachine.type(machine);
            int current = (Integer)NUMBERS.getOrDefault(type, 0);
            if (var6.number() == 0) {
               var6 = new CasinoMachineIdentity(var6.name(), Math.incrementExact(current));
               configured.machineIdentity(var6);
               machine.setChanged();
               server.getChunkSource().blockChanged(machine.getBlockPos());
            }

            if (var6.number() > current) {
               NUMBERS.put(type, var6.number());
               CasinoBetRules.settingsChanged();
            }

            if (LISTED.put(machine, Boolean.TRUE) == null) {
               list(machine);
            }

            return var6;
         } else {
            return configured.machineIdentity();
         }
      } else {
         return CasinoMachineIdentity.EMPTY;
      }
   }

   static void list(BlockEntity machine) {
      Level var3 = machine.getLevel();
      if (var3 instanceof ServerLevel server) {
         if (machine instanceof CasinoConfiguredMachine configured) {
            CroupierSettings var6 = configured.croupierSettings();
            CasinoMachineIdentity identity = configured.machineIdentity();
            Listed line = new Listed(dimensionOf(server), machine.getBlockPos().asLong(), CasinoMachine.type(machine), identity.name(), identity.number(), var6.bets().size(), var6.notBets().size(), var6.onlyListed(), var6.chance(), var6.bias(), var6.exchanges().size());
            if (!MACHINES.containsKey(line.key()) && MACHINES.size() >= 4096) {
               return;
            }

            if (!line.equals(MACHINES.put(line.key(), line))) {
               listChanged();
            }

            return;
         }
      }

   }

   public static void forget(BlockEntity machine) {
      Level var2 = machine.getLevel();
      if (var2 instanceof ServerLevel server) {
         LISTED.remove(machine);
         Map var10000 = MACHINES;
         String var10001 = dimensionOf(server);
         if (var10000.remove(var10001 + "|" + machine.getBlockPos().asLong()) != null) {
            listChanged();
         }

      }
   }

   private static void listChanged() {
      ++listRevision;
      CasinoBetRules.settingsChanged();
   }

   public static String dimensionOf(Level level) {
      return level.dimension().identifier().toString();
   }

   private static ServerLevel levelOf(MinecraftServer server, String dimension) {
      Identifier id = Identifier.tryParse(dimension);
      return id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
   }

   public static boolean accepts(CroupierSettings settings, ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else {
         String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
         if (settings.notBets().contains(id)) {
            return false;
         } else if (settings.bets().contains(id)) {
            return true;
         } else {
            return !settings.onlyListed() && CasinoBets.accepts(stack);
         }
      }
   }

   public static boolean accepts(BlockEntity machine, ItemStack stack) {
      return accepts(settings(machine), stack);
   }

   public static List<Item> bettableItems(CroupierSettings settings) {
      LinkedHashSet<Item> result = new LinkedHashSet();

      for(Item item : CasinoBets.bettableItems()) {
         if (accepts(settings, new ItemStack(item))) {
            result.add(item);
         }
      }

      for(String id : settings.bets()) {
         Optional<Item> var10000 = BuiltInRegistries.ITEM.getOptional(Identifier.parse(id)).filter((itemx) -> itemx != Items.AIR && accepts(settings, new ItemStack(itemx)));
         Objects.requireNonNull(result);
         var10000.ifPresent(result::add);
      }

      return List.copyOf(result);
   }

   public static double playerLuck(Player player) {
      return player != null ? (double) player.getLuck() : 0.0D;
   }

   public static int chance(BlockEntity machine) {
      int own = settings(machine).chance();
      return own >= 0 ? own : ((CroupierSettings)TYPES.getOrDefault(CasinoMachine.type(machine), CroupierSettings.EMPTY)).chance();
   }

   public static int bias(BlockEntity machine) {
      int own = settings(machine).bias();
      if (own != Integer.MIN_VALUE) {
         return own;
      } else {
         int type = ((CroupierSettings)TYPES.getOrDefault(CasinoMachine.type(machine), CroupierSettings.EMPTY)).bias();
         return type == Integer.MIN_VALUE ? 0 : type;
      }
   }

   public static boolean breakable(Level level) {
      return level.isClientSide() ? clientBreakable : breakable;
   }

   public static CasinoExchange.Rule exchange(BlockEntity machine, ItemStack stake) {
      return exchange(settings(machine), machine.getLevel() != null && machine.getLevel().isClientSide() ? clientWorld : world, stake);
   }

   public static CasinoExchange.Rule exchange(CroupierSettings own, CroupierSettings inherited, ItemStack stake) {
      String id = BuiltInRegistries.ITEM.getKey(stake.getItem()).toString();

      for(CasinoExchange.Rule rule : own.exchanges()) {
         if (rule.stake().equals(id)) {
            return rule;
         }
      }

      for(CasinoExchange.Rule rule : inherited.exchanges()) {
         if (rule.stake().equals(id)) {
            return rule;
         }
      }

      return null;
   }

   public static CasinoExchange.Rule menuExchange(AbstractContainerMenu menu, ItemStack stake) {
      BlockEntity machine = menuMachine(menu);
      return machine == null ? exchange(forMenu(menu.containerId), clientWorld, stake) : exchange(machine, stake);
   }

   public static void returnRejected(BlockEntity machine, SimpleContainer bets, ServerPlayer player) {
      for(int i = 0; i < bets.getContainerSize(); ++i) {
         ItemStack stack = bets.getItem(i);
         if (!stack.isEmpty() && !accepts(machine, stack)) {
            ItemStack rejected = bets.removeItemNoUpdate(i);
            if (player != null) {
               if (!player.getInventory().add(rejected)) {
                  player.drop(rejected, false, Prediction.SERVER_ONLY);
               }
            } else {
               Containers.dropItemStack(machine.getLevel(), (double)machine.getBlockPos().getX() + (double)0.5F, (double)machine.getBlockPos().getY() + (double)0.5F, (double)machine.getBlockPos().getZ() + (double)0.5F, rejected);
            }

            machine.setChanged();
         }
      }

   }

   public static void loadWorld(String json) {
      reset();

      try {
         JsonObject data = JsonParser.parseString(json).getAsJsonObject();
         breakable = !data.has("breakable") || data.get("breakable").getAsBoolean();
         if (data.has("world")) {
            world = CroupierSettings.fromJson(data.get("world").toString());
         }

         if (data.has("types")) {
            for(Map.Entry<String, JsonElement> entry : data.getAsJsonObject("types").entrySet()) {
               if (CasinoMachine.TYPES.contains(entry.getKey())) {
                  TYPES.put((String)entry.getKey(), CroupierSettings.fromJson(((JsonElement)entry.getValue()).toString()));
               }
            }
         }

         if (data.has("clipboard")) {
            for(Map.Entry<String, JsonElement> entry : data.getAsJsonObject("clipboard").entrySet()) {
               JsonElement value = (JsonElement)entry.getValue();
               Copied copied = value.isJsonObject() ? new Copied(value.getAsJsonObject().get("chance").getAsInt(), value.getAsJsonObject().has("bias") ? value.getAsJsonObject().get("bias").getAsInt() : Integer.MIN_VALUE) : new Copied(value.getAsInt(), Integer.MIN_VALUE);
               if (COPIED.size() < 256 && copied.chance() >= -1 && copied.chance() <= 100) {
                  COPIED.put((String)entry.getKey(), copied);
               }
            }
         }

         if (data.has("numbers")) {
            for(Map.Entry<String, JsonElement> entry : data.getAsJsonObject("numbers").entrySet()) {
               if (CasinoMachine.TYPES.contains(entry.getKey()) && ((JsonElement)entry.getValue()).getAsInt() >= 0) {
                  NUMBERS.put((String)entry.getKey(), ((JsonElement)entry.getValue()).getAsInt());
               }
            }
         }

         if (data.has("machines")) {
            for(JsonElement element : data.getAsJsonArray("machines")) {
               JsonArray m = element.getAsJsonArray();
               Listed line = new Listed(m.get(0).getAsString(), m.get(1).getAsLong(), m.get(2).getAsString(), m.get(3).getAsString(), m.get(4).getAsInt(), m.get(5).getAsInt(), m.get(6).getAsInt(), m.get(7).getAsBoolean(), m.get(8).getAsInt(), m.get(9).getAsInt(), m.get(10).getAsInt());
               if (CasinoMachine.TYPES.contains(line.type()) && MACHINES.size() < 4096) {
                  MACHINES.put(line.key(), line);
               }
            }
         }

         clientWorld = world.publicView();
         clientBreakable = breakable;
      } catch (RuntimeException e) {
         reset();
         LogUtils.getLogger().warn((String)"[MineVegas Casino] Invalid settings, using defaults", (Throwable)e);
      }

   }

   public static String saveWorld() {
      JsonObject data = new JsonObject();
      JsonObject types = new JsonObject();
      data.addProperty("breakable", breakable);
      data.add("world", JsonParser.parseString(world.toJson()));
      TYPES.forEach((type, value) -> types.add(type, JsonParser.parseString(value.toJson())));
      data.add("types", types);
      JsonObject clipboard = new JsonObject();
      COPIED.forEach((token, copied) -> {
         JsonObject entry = new JsonObject();
         entry.addProperty("chance", (Number)copied.chance());
         if (copied.bias() != Integer.MIN_VALUE) {
            entry.addProperty("bias", (Number)copied.bias());
         }

         clipboard.add(token, entry);
      });
      data.add("clipboard", clipboard);
      JsonObject numbers = new JsonObject();
      Map<String, Integer> var10000 = NUMBERS;
      Objects.requireNonNull(numbers);
      var10000.forEach(numbers::addProperty);
      data.add("numbers", numbers);
      JsonArray machines = new JsonArray();

      for(Listed line : MACHINES.values()) {
         JsonArray m = new JsonArray();
         m.add(line.dimension());
         m.add((Number)line.position());
         m.add(line.type());
         m.add(line.name());
         m.add((Number)line.number());
         m.add((Number)line.bets());
         m.add((Number)line.notBets());
         m.add(line.only());
         m.add((Number)line.chance());
         m.add((Number)line.bias());
         m.add((Number)line.exchanges());
         machines.add((JsonElement)m);
      }

      data.add("machines", machines);
      return data.toString();
   }

   public static void reset() {
      world = CroupierSettings.EMPTY;
      TYPES.clear();
      NUMBERS.clear();
      breakable = true;
      RATES.clear();
      COPIED.clear();
      MACHINES.clear();
      LISTED.clear();
      LIST_SENT.clear();
   }

   public static void forget() {
      MENUS.clear();
      clientWorld = CroupierSettings.EMPTY;
      clientBreakable = true;
      REVISION.incrementAndGet();
   }

   public static CroupierSettings forMenu(int id) {
      MenuState state = (MenuState)MENUS.get(id);
      return state == null ? CroupierSettings.EMPTY : state.settings();
   }

   public static void menuOpened(ServerPlayer player, int id, BlockEntity machine) {
      Services.PLATFORM.sendToPlayer(player, menuPayload(id, machine));
   }

   private static Payload menuPayload(int id, BlockEntity machine) {
      JsonObject json = new JsonObject();
      json.addProperty("kind", "menu");
      json.addProperty("id", (Number)id);
      json.addProperty("position", (Number)machine.getBlockPos().asLong());
      json.add("settings", JsonParser.parseString(settings(machine).publicView().toJson()));
      return new Payload(json.toString());
   }

   public static void sendWorldTo(ServerPlayer player) {
      JsonObject json = new JsonObject();
      json.addProperty("kind", "world");
      json.addProperty("breakable", breakable);
      json.add("settings", JsonParser.parseString(world.publicView().toJson()));
      Services.PLATFORM.sendToPlayer(player, new Payload(json.toString()));
   }

   public static void receive(Payload payload) {
      try {
         JsonObject data = JsonParser.parseString(payload.data()).getAsJsonObject();
         switch (data.get("kind").getAsString()) {
            case "world":
               clientBreakable = data.get("breakable").getAsBoolean();
               clientWorld = CroupierSettings.fromJson(data.get("settings").toString());
               break;
            case "menu":
               if (MENUS.size() > 128) {
                  MENUS.clear();
               }

               MENUS.put(data.get("id").getAsInt(), new MenuState(data.get("position").getAsLong(), CroupierSettings.fromJson(data.get("settings").toString())));
               break;
            case "view":
               Map<String, CroupierSettings> types = new HashMap();
               if (data.has("types")) {
                  for(Map.Entry<String, JsonElement> entry : data.getAsJsonObject("types").entrySet()) {
                     types.put((String)entry.getKey(), CroupierSettings.fromJson(((JsonElement)entry.getValue()).toString()));
                  }
               }

               List<Listed> machines = data.has("machines") ? new ArrayList() : null;
               if (machines != null) {
                  for(JsonElement element : data.getAsJsonArray("machines")) {
                     JsonArray m = element.getAsJsonArray();
                     machines.add(new Listed(m.get(0).getAsString(), m.get(1).getAsLong(), m.get(2).getAsString(), m.get(3).getAsString(), m.get(4).getAsInt(), m.get(5).getAsInt(), m.get(6).getAsInt(), m.get(7).getAsBoolean(), m.get(8).getAsInt(), m.get(9).getAsInt(), m.get(10).getAsInt()));
                  }
               }

               viewReceiver.accept(new View(data.get("target").getAsString(), data.get("position").getAsLong(), data.get("type").getAsString(), CroupierSettings.fromJson(data.get("settings").toString()), CroupierSettings.fromJson(data.get("inherited").toString()), data.get("breakable").getAsBoolean(), data.get("revision").getAsInt(), data.has("name") ? data.get("name").getAsString() : "", data.has("number") ? data.get("number").getAsInt() : 0, !data.has("open") || data.get("open").getAsBoolean(), Map.copyOf(types), data.has("dimension") ? data.get("dimension").getAsString() : "", machines == null ? null : List.copyOf(machines)));
         }

         REVISION.incrementAndGet();
      } catch (RuntimeException var9) {
         LogUtils.getLogger().warn("[MineVegas Casino] Rejected malformed settings response");
      }

   }

   private static boolean limited(ServerPlayer player) {
      long second = player.level().getGameTime() / 20L;
      Rate rate = (Rate)RATES.get(player.getUUID());
      if (rate != null && rate.second() == second) {
         if (rate.count() >= 20) {
            return true;
         } else {
            RATES.put(player.getUUID(), new Rate(second, rate.count() + 1));
            return false;
         }
      } else {
         RATES.put(player.getUUID(), new Rate(second, 1));
         return false;
      }
   }

   private static BlockEntity machineFor(ServerPlayer player, String dimension, long position) {
      ServerLevel level = dimension.isEmpty() ? player.level() : levelOf(player.level().getServer(), dimension);
      if (level == null) {
         return null;
      } else {
         BlockPos pos = BlockPos.of(position);
         Map var10000 = MACHINES;
         String var10001 = dimensionOf(level);
         if (var10000.containsKey(var10001 + "|" + position)) {
            level.getChunk(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
         }

         BlockEntity machine = CasinoMachine.at(level, pos);
         if (machine == null) {
            var10000 = MACHINES;
            var10001 = dimensionOf(level);
            if (var10000.remove(var10001 + "|" + position) != null) {
               listChanged();
            }
         }

         return machine;
      }
   }

   public static void edit(ServerPlayer player, EditPayload edit) {
      if (allowed(player) && !limited(player)) {
         boolean machineTarget = edit.target().equals("machine");
         boolean worldTarget = edit.target().equals("world");
         BlockEntity machine = machineTarget ? machineFor(player, edit.dimension(), edit.position()) : null;
         if (machineTarget && machine == null) {
            if (edit.action().equals("open")) {
               sendView(player, "world", (BlockEntity)null, world, "", true);
            }

         } else {
            if (machineTarget) {
               identify(machine);
            }

            if (machineTarget || worldTarget || CasinoMachine.TYPES.contains(edit.target())) {
               String type = machineTarget ? CasinoMachine.type(machine) : (worldTarget ? "" : edit.target());
               CroupierSettings old = machineTarget ? settings(machine) : (worldTarget ? world : (CroupierSettings)TYPES.getOrDefault(type, CroupierSettings.EMPTY));
               String shown = machineTarget ? "machine" : "world";
               if (edit.action().equals("open")) {
                  sendView(player, shown, machine, machineTarget ? old : world, type, true);
               } else {
                  try {
                     CroupierSettings next = old; ItemStack hand; CompoundTag tag;
                     switch (edit.action()) {
                        case "rename":
                           if (!machineTarget) {
                              return;
                           }

                           CasinoConfiguredMachine configured = (CasinoConfiguredMachine)machine;
                           configured.machineIdentity(new CasinoMachineIdentity(edit.item(), configured.machineIdentity().number()));
                           break;
                        case "bet":
                           if (machineTarget && known(edit.item()) && !edit.item().equals("roulettecasino:croupier_hand")) {
                              next = old.withBet(edit.item(), edit.flag());
                              break;
                           }

                           return;
                        case "inheritbet":
                           next = old.inheritBet(edit.item());
                           break;
                        case "only":
                           if (!machineTarget) {
                              return;
                           }

                           next = old.withOnly(edit.flag());
                           break;
                        case "chance":
                           if (!worldTarget && !CasinoMachine.usesBias(type)) {
                              next = old.withChance(edit.value());
                              break;
                           }

                           return;
                        case "bias":
                           if (!worldTarget && CasinoMachine.usesBias(type)) {
                              next = old.withBias(edit.flag() ? Integer.MIN_VALUE : edit.value());
                              break;
                           }

                           return;
                        case "exchange":
                           if (!machineTarget && !worldTarget || !known(edit.item()) || !known(edit.result())) {
                              return;
                           }

                           next = old.withExchange(new CasinoExchange.Rule(edit.item(), edit.result(), edit.value(), edit.flag()));
                           break;
                        case "removeexchange":
                           if (!known(edit.item())) {
                              return;
                           }

                           boolean inheritedExchange = world.exchanges().stream().anyMatch((rule) -> rule.stake().equals(edit.item()));
                           next = machineTarget && inheritedExchange ? old.withExchange(new CasinoExchange.Rule(edit.item(), edit.item(), 1, false)) : old.withoutExchange(edit.item());
                           break;
                        case "inheritexchange":
                           next = old.withoutExchange(edit.item());
                           break;
                        case "breakable":
                           if (!worldTarget) {
                              return;
                           }

                           breakable = edit.flag();
                           break;
                        case "reset":
                           next = CroupierSettings.EMPTY;
                           if (worldTarget) {
                              TYPES.clear();
                              breakable = true;
                           }
                           break;
                        case "clearexchanges":
                           if (!worldTarget) {
                              return;
                           }

                           next = old.withoutExchanges();
                           break;
                        case "cleartypes":
                           if (!worldTarget) {
                              return;
                           }

                           TYPES.clear();
                           break;
                        case "copy":
                           if (!machineTarget) {
                              return;
                           }

                           hand = hand(player);
                           tag = ((CustomData)hand.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
                           String token = UUID.randomUUID().toString();

                           while(COPIED.size() >= 256) {
                              COPIED.remove(COPIED.keySet().iterator().next());
                           }

                           COPIED.put(token, new Copied(old.chance(), old.bias()));
                           CasinoMachineIdentity identity = ((CasinoConfiguredMachine)machine).machineIdentity();
                           tag.putString("MineVegasSettings", old.publicView().toJson());
                           tag.putString("MineVegasType", type);
                           tag.putString("MineVegasCopyToken", token);
                           tag.putLong("MineVegasCopyPosition", machine.getBlockPos().asLong());
                           tag.putString("MineVegasCopyName", identity.name());
                           tag.putInt("MineVegasCopyNumber", identity.number());
                           hand.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                           CasinoBetRules.settingsChanged();
                           sendView(player, shown, machine, old, type, false);
                           return;
                        case "forgetcopy":
                           hand = hand(player);
                           tag = ((CustomData)hand.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
                           if (!tag.contains("MineVegasSettings")) {
                              return;
                           }

                           for(String key : List.of("MineVegasSettings", "MineVegasType", "MineVegasCopyToken", "MineVegasCopyPosition", "MineVegasCopyName", "MineVegasCopyNumber")) {
                              tag.remove(key);
                           }

                           if (tag.isEmpty()) {
                              hand.remove(DataComponents.CUSTOM_DATA);
                           } else {
                              hand.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                           }

                           player.sendOverlayMessage(Component.translatable("message.roulettecasino.croupier_hand.copy_forgotten"));
                           return;
                        case "paste":
                           if (!machineTarget) {
                              return;
                           }

                           tag = ((CustomData)hand(player).getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag();
                           String json = (String)tag.getString("MineVegasSettings").orElse("");
                           if (json.isEmpty()) {
                              player.sendOverlayMessage(Component.translatable("message.roulettecasino.croupier_hand.nothing_to_paste"));
                              return;
                           }

                           Copied copied = (Copied)COPIED.getOrDefault(tag.getString("MineVegasCopyToken").orElse(""), new Copied(-1, Integer.MIN_VALUE));
                           next = CroupierSettings.fromJson(json).withChance(copied.chance()).withBias(copied.bias());
                           if (!type.equals(tag.getString("MineVegasType").orElse(""))) {
                              next = next.withChance(old.chance()).withBias(old.bias());
                           }
                           break;
                        default:
                           return;
                     }

                     if (machineTarget) {
                        if (next.exchanges().size() > 256 || !next.fitsMachine()) {
                           return;
                        }

                        ((CasinoConfiguredMachine)machine).croupierSettings(next);
                        machine.setChanged();
                        ((ServerLevel)machine.getLevel()).getChunkSource().blockChanged(machine.getBlockPos());
                        list(machine);
                     } else if (worldTarget) {
                        world = next;
                     } else {
                        TYPES.put(type, next);
                     }

                     CasinoBetRules.settingsChanged();
                     REVISION.incrementAndGet();
                     MinecraftServer server = player.level().getServer();

                     for(ServerPlayer each : server.getPlayerList().getPlayers()) {
                        sendWorldTo(each);
                        BlockEntity opened = menuMachine(each.containerMenu);
                        if (opened != null) {
                           menuOpened(each, each.containerMenu.containerId, opened);
                        }
                     }

                     sendView(player, shown, machine, machineTarget ? next : world, type, false);
                     if (edit.action().equals("paste") && edit.flag()) {
                        player.sendOverlayMessage(Component.translatable("message.roulettecasino.croupier_hand.pasted", CasinoMachine.displayName(type, ((CasinoConfiguredMachine)machine).machineIdentity().name(), ((CasinoConfiguredMachine)machine).machineIdentity().number())));
                     }

                     LogUtils.getLogger().info("[MineVegas Casino] {} changed {} settings at {}: {}", player.getName().getString(), edit.target(), edit.position(), edit.action());
                  } catch (RuntimeException e) {
                     LogUtils.getLogger().warn((String)"[MineVegas Casino] Invalid settings edit rejected: {}", (Object)e.getMessage());
                  }

               }
            }
         }
      }
   }

   private static boolean known(String id) {
      Identifier key = Identifier.tryParse(id);
      return key != null && BuiltInRegistries.ITEM.getOptional(key).filter((item) -> item != Items.AIR).isPresent();
   }

   private static ItemStack hand(Player player) {
      return player.getMainHandItem().is(CroupierHandItem.ITEM) ? player.getMainHandItem() : player.getOffhandItem();
   }

   public static BlockEntity menuMachine(AbstractContainerMenu menu) {
      if (menu instanceof RouletteMenu m) {
         return m.table();
      } else if (menu instanceof BlackjackMenu m) {
         return m.table();
      } else if (menu instanceof PokerMenu m) {
         return m.table();
      } else if (menu instanceof DuckDerbyMenu m) {
         return m.machine();
      } else if (menu instanceof SlotMachineMenu m) {
         return m.machine();
      } else if (menu instanceof FortuneWheelMenu m) {
         return m.wheel();
      } else if (menu instanceof PlinkoMenu m) {
         return m.machine();
      } else {
         return null;
      }
   }

   private static List<Listed> machines(MinecraftServer server) {
      boolean changed = false;
      Iterator<Listed> it = MACHINES.values().iterator();

      while(it.hasNext()) {
         Listed line = (Listed)it.next();
         ServerLevel level = levelOf(server, line.dimension());
         BlockPos pos = BlockPos.of(line.position());
         if (level != null && level.hasChunkAt(pos)) {
            BlockEntity machine = CasinoMachine.at(level, pos);
            if (machine == null || !machine.getBlockPos().equals(pos) || !CasinoMachine.type(machine).equals(line.type())) {
               it.remove();
               changed = true;
            }
         }
      }

      if (changed) {
         listChanged();
      }

      List<Listed> sorted = new ArrayList(MACHINES.values());
      sorted.sort(Comparator.<Listed>comparingInt((linex) -> CasinoMachine.TYPES.indexOf(linex.type())).thenComparingInt(Listed::number).thenComparing(Listed::name).thenComparing(Listed::dimension).thenComparingLong(Listed::position));
      return sorted;
   }

   private static void sendView(ServerPlayer player, String target, BlockEntity machine, CroupierSettings settings, String type, boolean open) {
      JsonObject json = new JsonObject();
      json.addProperty("kind", "view");
      json.addProperty("target", target);
      json.addProperty("position", (Number)(machine == null ? 0L : machine.getBlockPos().asLong()));
      json.addProperty("dimension", machine == null ? "" : dimensionOf(machine.getLevel()));
      json.addProperty("type", machine == null ? "" : type);
      json.addProperty("revision", (Number)REVISION.get());
      json.addProperty("breakable", breakable);
      json.addProperty("open", open);
      if (machine instanceof CasinoConfiguredMachine configured) {
         json.addProperty("name", configured.machineIdentity().name());
         json.addProperty("number", (Number)configured.machineIdentity().number());
      }

      CroupierSettings inherited = CroupierSettings.EMPTY;
      if (machine != null) {
         CroupierSettings ofType = (CroupierSettings)TYPES.getOrDefault(type, CroupierSettings.EMPTY);
         inherited = new CroupierSettings(List.of(), List.of(), false, ofType.chance(), world.exchanges(), ofType.bias());
      }

      JsonObject types = new JsonObject();
      TYPES.forEach((name, value) -> types.add(name, JsonParser.parseString(value.toJson())));
      json.add("types", types);
      json.add("settings", JsonParser.parseString(settings.toJson()));
      json.add("inherited", JsonParser.parseString(inherited.toJson()));
      List<Listed> listed = machines(player.level().getServer());
      if (open || !Objects.equals(LIST_SENT.get(player.getUUID()), listRevision)) {
         int room = 580000 - json.toString().getBytes(StandardCharsets.UTF_8).length;
         JsonArray list = new JsonArray();

         for(Listed line : listed) {
            JsonArray m = new JsonArray();
            m.add(line.dimension());
            m.add((Number)line.position());
            m.add(line.type());
            m.add(line.name());
            m.add((Number)line.number());
            m.add((Number)line.bets());
            m.add((Number)line.notBets());
            m.add(line.only());
            m.add((Number)line.chance());
            m.add((Number)line.bias());
            m.add((Number)line.exchanges());
            room -= m.toString().getBytes(StandardCharsets.UTF_8).length + 1;
            if (room < 0) {
               break;
            }

            list.add((JsonElement)m);
         }

         json.add("machines", list);
         LIST_SENT.put(player.getUUID(), listRevision);
      }

      Services.PLATFORM.sendToPlayer(player, new Payload(json.toString()));
   }

   static {
      world = CroupierSettings.EMPTY;
      TYPES = new HashMap();
      NUMBERS = new HashMap();
      breakable = true;
      clientWorld = CroupierSettings.EMPTY;
      clientBreakable = true;
      viewReceiver = (view) -> {
      };
   }

   public static record View(String target, long position, String type, CroupierSettings settings, CroupierSettings inherited, boolean breakable, int revision, String name, int number, boolean open, Map<String, CroupierSettings> types, String dimension, List<Listed> machines) {
      public View(String target, long position, String type, CroupierSettings settings, CroupierSettings inherited, boolean breakable, int revision, String name, int number) {
         this(target, position, type, settings, inherited, breakable, revision, name, number, true, Map.of(), "", List.of());
      }

      public boolean sameTarget(View other) {
         return other != null && this.target.equals(other.target()) && this.position == other.position() && this.dimension.equals(other.dimension());
      }

      public View withMachines(List<Listed> list) {
         return new View(this.target, this.position, this.type, this.settings, this.inherited, this.breakable, this.revision, this.name, this.number, this.open, this.types, this.dimension, list);
      }
   }

   public static record Listed(String dimension, long position, String type, String name, int number, int bets, int notBets, boolean only, int chance, int bias, int exchanges) {
      public boolean own() {
         return this.bets + this.notBets + this.exchanges > 0 || this.only || this.chance >= 0 || this.bias != Integer.MIN_VALUE;
      }

      String key() {
         return this.dimension + "|" + this.position;
      }
   }

   private static record MenuState(long position, CroupierSettings settings) {
   }

   private static record Rate(long second, int count) {
   }

   private static record Copied(int chance, int bias) {
   }

   public static record Payload(String data) implements CustomPacketPayload {
      public static final CustomPacketPayload.Type<Payload> TYPE = new CustomPacketPayload.Type<Payload>(RouletteMod.id("settings"));
      public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = StreamCodec.<RegistryFriendlyByteBuf, Payload>of((buf, val) -> buf.writeUtf(val.data(), 600000), (buf) -> new Payload(buf.readUtf(600000)));

      public CustomPacketPayload.Type<Payload> type() {
         return TYPE;
      }
   }

   public static record EditPayload(String target, long position, String action, String item, String result, int value, boolean flag, String dimension) implements CustomPacketPayload {
      public static final CustomPacketPayload.Type<EditPayload> TYPE = new CustomPacketPayload.Type<EditPayload>(RouletteMod.id("settings_edit"));
      public static final StreamCodec<RegistryFriendlyByteBuf, EditPayload> CODEC = StreamCodec.<RegistryFriendlyByteBuf, EditPayload>of((buf, val) -> {
         buf.writeUtf(val.target(), 32);
         buf.writeLong(val.position());
         buf.writeUtf(val.action(), 32);
         buf.writeUtf(val.item(), 256);
         buf.writeUtf(val.result(), 256);
         buf.writeVarInt(val.value());
         buf.writeBoolean(val.flag());
         buf.writeUtf(val.dimension(), 256);
      }, (buf) -> new EditPayload(buf.readUtf(32), buf.readLong(), buf.readUtf(32), buf.readUtf(256), buf.readUtf(256), buf.readVarInt(), buf.readBoolean(), buf.readUtf(256)));

      public EditPayload(String target, long position, String action, String item, String result, int value, boolean flag) {
         this(target, position, action, item, result, value, flag, "");
      }

      public CustomPacketPayload.Type<EditPayload> type() {
         return TYPE;
      }
   }
}






