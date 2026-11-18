package fr.minevegas.casino;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Function4;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import fr.minevegas.casino.platform.Services;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class DerbyLobby {
   public static final Codec<Member> MEMBER_CODEC = RecordCodecBuilder.<Member>create((i) -> i.group(Codec.STRING.fieldOf("id").forGetter((m) -> m.id.toString()), Codec.STRING.fieldOf("name").forGetter(Member::name), Codec.INT.fieldOf("lane").forGetter(Member::lane), ItemStack.CODEC.listOf().optionalFieldOf("stakes", List.of()).forGetter(Member::stakes)).apply(i, (id, name, lane, stakes) -> new Member(UUID.fromString(id), name, lane, stakes)));
   private static final Map<Integer, View> CLIENT = new LinkedHashMap();

   public static String json(DuckDerbyBlockEntity machine, ServerPlayer player) {
      JsonObject data = new JsonObject();
      data.addProperty("enrolling", machine.enrolling());
      data.addProperty("seconds", (Number)machine.enrollmentSeconds());
      data.addProperty("confirmed", machine.confirmed(player.getUUID()));
      data.addProperty("allowed", machine.groupAllowed());
      data.addProperty("legacy", !machine.bets().isEmpty());
      data.addProperty("credits", (Number)DerbyCredits.get(player.level().getServer()).available(player));
      JsonArray people = new JsonArray();

      for(Member member : machine.members()) {
         JsonObject p = new JsonObject();
         p.addProperty("id", member.id.toString());
         p.addProperty("name", member.name);
         p.addProperty("lane", (Number)member.lane);
         people.add((JsonElement)p);
      }

      data.add("members", people);
      return data.toString();
   }

   public static void send(ServerPlayer player, int menu, String json) {
      Services.PLATFORM.sendToPlayer(player, new Payload(menu, json));
   }

   public static void receive(Payload payload) {
      JsonObject d = JsonParser.parseString(payload.json).getAsJsonObject();
      List<Member> members = new ArrayList();

      for(JsonElement e : d.getAsJsonArray("members")) {
         JsonObject p = e.getAsJsonObject();
         members.add(new Member(UUID.fromString(p.get("id").getAsString()), p.get("name").getAsString(), p.get("lane").getAsInt()));
      }

      CLIENT.put(payload.menu, new View(d.get("enrolling").getAsBoolean(), d.get("seconds").getAsInt(), d.get("confirmed").getAsBoolean(), d.get("allowed").getAsBoolean(), d.get("legacy").getAsBoolean(), d.get("credits").getAsInt(), List.copyOf(members)));

      while(CLIENT.size() > 16) {
         CLIENT.remove(CLIENT.keySet().iterator().next());
      }

   }

   public static View view(int id) {
      return (View)CLIENT.getOrDefault(id, DerbyLobby.View.EMPTY);
   }

   public static List<Member> visible(List<Member> members, UUID viewer, int limit) {
      List<Member> result = new ArrayList();

      for(Member member : members) {
         if (member.id.equals(viewer)) {
            result.add(member);
            break;
         }
      }

      for(Member member : members) {
         if (result.size() < limit && !member.id.equals(viewer)) {
            result.add(member);
         }
      }

      return result;
   }

   public static void clear() {
      CLIENT.clear();
   }

   public static record Member(UUID id, String name, int lane, List<ItemStack> stakes) {
      public Member(UUID id, String name, int lane) {
         this(id, name, lane, List.of());
      }
   }

   public static record View(boolean enrolling, int seconds, boolean confirmed, boolean groupAllowed, boolean legacy, int credits, List<Member> members) {
      public static final View EMPTY = new View(false, 0, false, false, false, 0, List.of());
   }

   public static record Payload(int menu, String json) implements CustomPacketPayload {
      public static final CustomPacketPayload.Type<Payload> TYPE = new CustomPacketPayload.Type<Payload>(RouletteMod.id("derby_lobby"));
      public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = StreamCodec.<RegistryFriendlyByteBuf, Payload>of((b, p) -> {
         b.writeVarInt(p.menu);
         b.writeUtf(p.json, 16384);
      }, (b) -> new Payload(b.readVarInt(), b.readUtf(16384)));

      public CustomPacketPayload.Type<Payload> type() {
         return TYPE;
      }
   }
}

