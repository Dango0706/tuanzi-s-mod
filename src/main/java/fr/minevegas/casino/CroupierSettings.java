package fr.minevegas.casino;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.Identifier;

public record CroupierSettings(List<String> bets, List<String> notBets, boolean onlyListed, int chance, List<CasinoExchange.Rule> exchanges, int bias) {
   public static final int INHERIT = Integer.MIN_VALUE;
   public static final CroupierSettings EMPTY = new CroupierSettings(List.of(), List.of(), false, -1, List.of(), Integer.MIN_VALUE);
   public static final int MAX_ITEMS = 512;
   public static final int MAX_EXCHANGES = 1024;
   public static final int MAX_JSON = 240000;
   public static final int MAX_MACHINE_JSON = 60000;
   public static final Codec<CroupierSettings> CODEC;

   public CroupierSettings(List<String> bets, List<String> notBets, boolean onlyListed, int chance, List<CasinoExchange.Rule> exchanges, int bias) {
      bets = List.copyOf(new LinkedHashSet(bets));
      notBets = List.copyOf(new LinkedHashSet(notBets));
      exchanges = List.copyOf(exchanges);
      if (bets.size() <= 512 && notBets.size() <= 512 && exchanges.size() <= 1024 && chance >= -1 && chance <= 100 && (bias == Integer.MIN_VALUE || bias >= -100 && bias <= 100)) {
         for(String id : bets) {
            requireId(id);
         }

         for(String id : notBets) {
            requireId(id);
         }

         if (!Collections.disjoint(bets, notBets)) {
            throw new IllegalArgumentException("contradictory item rules");
         } else {
            Set<String> keys = new HashSet();

            for(CasinoExchange.Rule rule : exchanges) {
               if (!keys.add(rule.stake())) {
                  throw new IllegalArgumentException("duplicate exchange");
               }
            }

            if (encodedBound(bets, notBets, exchanges) > 240000) {
               throw new IllegalArgumentException("settings too large");
            } else {
               this.bets = bets;
               this.notBets = notBets;
               this.onlyListed = onlyListed;
               this.chance = chance;
               this.exchanges = exchanges;
               this.bias = bias;
            }
         }
      } else {
         throw new IllegalArgumentException("settings bounds");
      }
   }

   public CroupierSettings(List<String> bets, List<String> notBets, boolean onlyListed, int chance, List<CasinoExchange.Rule> exchanges) {
      this(bets, notBets, onlyListed, chance, exchanges, Integer.MIN_VALUE);
   }

   private static int encodedBound(List<String> bets, List<String> notBets, List<CasinoExchange.Rule> exchanges) {
      int bound = 160;

      for(String id : bets) {
         bound += id.length() + 3;
      }

      for(String id : notBets) {
         bound += id.length() + 3;
      }

      for(CasinoExchange.Rule rule : exchanges) {
         bound += rule.stake().length() + rule.item().length() + 70;
      }

      return bound;
   }

   public boolean fitsMachine() {
      return encodedBound(this.bets, this.notBets, this.exchanges) <= 60000;
   }

   public static void requireId(String id) {
      if (id == null || id.length() > 256 || Identifier.tryParse(id) == null) {
         throw new IllegalArgumentException("item identifier");
      }
   }

   public CroupierSettings withBet(String id, boolean enabled) {
      requireId(id);
      List<String> yes = new ArrayList(this.bets);
      List<String> no = new ArrayList(this.notBets);
      yes.remove(id);
      no.remove(id);
      (enabled ? yes : no).add(id);
      return new CroupierSettings(yes, no, this.onlyListed, this.chance, this.exchanges, this.bias);
   }

   public CroupierSettings inheritBet(String id) {
      List<String> yes = new ArrayList(this.bets);
      List<String> no = new ArrayList(this.notBets);
      yes.remove(id);
      no.remove(id);
      return new CroupierSettings(yes, no, this.onlyListed, this.chance, this.exchanges, this.bias);
   }

   public CroupierSettings withOnly(boolean only) {
      return new CroupierSettings(this.bets, this.notBets, only, this.chance, this.exchanges, this.bias);
   }

   public CroupierSettings withChance(int percent) {
      return new CroupierSettings(this.bets, this.notBets, this.onlyListed, percent, this.exchanges, this.bias);
   }

   public CroupierSettings withBias(int value) {
      return new CroupierSettings(this.bets, this.notBets, this.onlyListed, this.chance, this.exchanges, value);
   }

   public CroupierSettings withExchange(CasinoExchange.Rule rule) {
      List<CasinoExchange.Rule> list = new ArrayList(this.exchanges);
      list.removeIf((r) -> r.stake().equals(rule.stake()));
      list.add(rule);
      return new CroupierSettings(this.bets, this.notBets, this.onlyListed, this.chance, list, this.bias);
   }

   public CroupierSettings withoutExchange(String id) {
      return new CroupierSettings(this.bets, this.notBets, this.onlyListed, this.chance, this.exchanges.stream().filter((r) -> !r.stake().equals(id)).toList(), this.bias);
   }

   public CroupierSettings withoutExchanges() {
      return new CroupierSettings(this.bets, this.notBets, this.onlyListed, this.chance, List.of(), this.bias);
   }

   public CroupierSettings publicView() {
      return new CroupierSettings(this.bets, this.notBets, this.onlyListed, -1, this.exchanges, Integer.MIN_VALUE);
   }

   public String toJson() {
      JsonObject json = new JsonObject();
      JsonArray yes = new JsonArray();
      JsonArray no = new JsonArray();
      JsonArray rules = new JsonArray();
      List<String> var10000 = this.bets;
      Objects.requireNonNull(yes);
      var10000.forEach(yes::add);
      var10000 = this.notBets;
      Objects.requireNonNull(no);
      var10000.forEach(no::add);
      json.add("bets", yes);
      json.add("not_bets", no);
      json.addProperty("only_listed", this.onlyListed);
      if (this.chance >= 0) {
         json.addProperty("win_chance", (Number)this.chance);
      }

      if (this.bias != Integer.MIN_VALUE) {
         json.addProperty("blackjack_bias", (Number)this.bias);
      }

      for(CasinoExchange.Rule rule : this.exchanges) {
         JsonObject r = new JsonObject();
         r.addProperty("stake", rule.stake());
         r.addProperty("item", rule.item());
         r.addProperty("count", (Number)rule.count());
         r.addProperty("mode", rule.profit() ? "profit" : "total");
         rules.add((JsonElement)r);
      }

      json.add("exchanges", rules);
      return json.toString();
   }

   public static CroupierSettings fromJson(String text) {
      if (text.length() > 240000) {
         throw new IllegalArgumentException("settings too large");
      } else {
         JsonObject json = JsonParser.parseString(text).getAsJsonObject();
         List<String> yes = strings(json, "bets");
         List<String> no = strings(json, "not_bets");
         List<CasinoExchange.Rule> rules = new ArrayList();
         if (json.has("exchanges")) {
            for(JsonElement e : json.getAsJsonArray("exchanges")) {
               JsonObject r = e.getAsJsonObject();
               String mode = r.has("mode") ? r.get("mode").getAsString() : "total";
               if (!mode.equals("total") && !mode.equals("profit")) {
                  throw new IllegalArgumentException("exchange mode");
               }

               rules.add(new CasinoExchange.Rule(r.get("stake").getAsString(), r.get("item").getAsString(), r.get("count").getAsInt(), mode.equals("profit")));
            }
         }

         return new CroupierSettings(yes, no, json.has("only_listed") && json.get("only_listed").getAsBoolean(), json.has("win_chance") ? json.get("win_chance").getAsInt() : -1, rules, json.has("blackjack_bias") ? json.get("blackjack_bias").getAsInt() : Integer.MIN_VALUE);
      }
   }

   private static List<String> strings(JsonObject json, String key) {
      List<String> list = new ArrayList();
      if (json.has(key)) {
         for(JsonElement e : json.getAsJsonArray(key)) {
            list.add(e.getAsString());
         }
      }

      return list;
   }

   static {
      CODEC = Codec.STRING.comapFlatMap((text) -> {
         try {
            return DataResult.success(fromJson(text));
         } catch (RuntimeException e) {
            return DataResult.error(() -> "Invalid croupier settings: " + e.getMessage());
         }
      }, CroupierSettings::toJson);
   }
}

