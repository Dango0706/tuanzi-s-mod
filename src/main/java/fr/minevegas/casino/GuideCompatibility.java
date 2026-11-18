package fr.minevegas.casino;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.minevegas.casino.compat.EasyNpcApi;
import net.minecraft.core.registries.BuiltInRegistries;

public final class GuideCompatibility {
   private GuideCompatibility() {
   }

   public static boolean mca() {
      try {
         Class.forName("fr.minevegas.casino.compat.McaDealerApi", false, GuideCompatibility.class.getClassLoader());
      } catch (ClassNotFoundException var1) {
         return false;
      }

      return BuiltInRegistries.ENTITY_TYPE.keySet().stream().anyMatch((id) -> id.getNamespace().equals("mca"));
   }

   public static void append(JsonObject book) {
      JsonArray pages = book.getAsJsonArray("pages");
      JsonArray tabs = book.getAsJsonArray("tabs");
      if (mca()) {
         JsonObject page = page("mca");
         table(page, book, "blackjack_table", 16, 16, 32, 140, "mca", 0);
         table(page, book, "poker_table", 190, 16, 32, 140, "mca", 1);
         note(page, "guide.roulettecasino.compat.mca_blackjack", 16, 176, 154, 2);
         note(page, "guide.roulettecasino.compat.mca_poker", 190, 176, 154, 2);
         tab(tabs, "mca", pages.size() + 1, "minecraft:villager_spawn_egg");
         pages.add((JsonElement)page);
      }

      if (EasyNpcApi.available()) {
         JsonObject page = page("easy_npc");
         title(page, "guide.roulettecasino.compat.hire", 18, 14);
         note(page, "guide.roulettecasino.compat.hire_text", 16, 31, 154, 2);
         JsonObject scene = JsonParser.parseString("{\"blocks\":[{\"state\":\"casinodeco:ruby_velvet\",\"at\":[0,0,0],\"to\":[2,0,1]}],\"yaw\":170,\"pitch\":12,\"zoom\":1,\"spin\":false,\"focus\":[1.5,2,0.65,1.6,2.6],\"hiring\":true,\"compat\":\"easy_npc\"}").getAsJsonObject();
         widget(page, "scene", 16, 48, 154, 118).add("scene", scene);
         note(page, "guide.roulettecasino.compat.inventory_text", 16, 172, 154, 2);
         table(page, book, "blackjack_table", 190, 14, 28, 70, "easy_npc", 0);
         table(page, book, "poker_table", 190, 104, 118, 70, "easy_npc", 1);
         tab(tabs, "easy_npc", pages.size() + 1, "roulettecasino:croupier_badge");
         pages.add((JsonElement)page);
      }

   }

   private static JsonObject page(String chapter) {
      JsonObject p = new JsonObject();
      p.addProperty("chapter", chapter);
      p.add("widgets", new JsonArray());
      return p;
   }

   private static JsonObject widget(JsonObject page, String kind, int x, int y, int w, int h) {
      JsonObject j = new JsonObject();
      j.addProperty("kind", kind);
      j.addProperty("x", (Number)x);
      j.addProperty("y", (Number)y);
      j.addProperty("w", (Number)w);
      j.addProperty("h", (Number)h);
      page.getAsJsonArray("widgets").add((JsonElement)j);
      return j;
   }

   private static void title(JsonObject page, String key, int x, int y) {
      widget(page, "title", x, y, 150, 0).addProperty("key", key);
   }

   private static void note(JsonObject page, String key, int x, int y, int w, int lines) {
      JsonObject j = widget(page, "text", x, y, w, 0);
      j.addProperty("key", key);
      j.addProperty("lines", (Number)lines);
      j.addProperty("color", "cream");
      j.addProperty("align", "center");
   }

   private static void tab(JsonArray tabs, String label, int page, String ref) {
      JsonObject j = new JsonObject();
      j.addProperty("label", label);
      j.addProperty("page", (Number)page);
      j.addProperty("ref", ref);
      tabs.add((JsonElement)j);
   }

   private static void table(JsonObject page, JsonObject book, String name, int x, int titleY, int sceneY, int sceneH, String compatibility, int seed) {
      title(page, "block.roulettecasino." + name, x + 2, titleY);
      int from = book.getAsJsonObject("index").get("roulettecasino:" + name).getAsInt() - 1;

      for(JsonElement entry : book.getAsJsonArray("pages").get(from).getAsJsonObject().getAsJsonArray("widgets")) {
         JsonObject original = entry.getAsJsonObject();
         if (original.has("scene")) {
            JsonObject scene = original.getAsJsonObject("scene").deepCopy();
            scene.addProperty("compat", compatibility);
            if (seed != 0) {
               scene.addProperty("seed", (Number)seed);
            }

            widget(page, "scene", x, sceneY, 154, sceneH).add("scene", scene);
            return;
         }
      }

   }
}
