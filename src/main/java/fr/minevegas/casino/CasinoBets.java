package fr.minevegas.casino;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class CasinoBets {
   public static final TagKey<Item> BETS;
   public static final TagKey<Item> NOT_BETS;
   private static final List<String> UNFINISHED;
   private static final List<String> FINISHED_FORMS;
   private static final String INGOTS = "ingots/";
   private static final String GEMS = "gems/";
   private static final String STORAGE_BLOCKS = "storage_blocks/";
   private static final String RAW = "raw_";

   private CasinoBets() {
   }

   private static TagKey<Item> common(String path) {
      return TagKey.<Item>create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", path));
   }

   public static boolean accepts(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else {
         CasinoBetRules.Snapshot rules = CasinoBetRules.current();
         if (rules.notBets().contains(stack.getItem())) {
            return false;
         } else {
            return rules.bets().contains(stack.getItem()) || acceptsByTags(stack);
         }
      }
   }

   public static boolean acceptsByTags(ItemStack stack) {
      if (!stack.isEmpty() && !stack.is(NOT_BETS)) {
         if (stack.is(BETS)) {
            return true;
         } else {
            Holder<Item> item = stack.typeHolder();
            return !vanilla(item) && finishedWithBlock(item);
         }
      } else {
         return false;
      }
   }

   public static List<Item> bettableItems() {
      CasinoBetRules.Snapshot rules = CasinoBetRules.current();
      Set<Item> items = new LinkedHashSet();

      for(Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(BETS)) {
         if (accepts(new ItemStack(holder.value()))) {
            items.add(holder.value());
         }
      }

      for(Item item : BuiltInRegistries.ITEM) {
         if (!items.contains(item) && !rules.bets().contains(item) && accepts(new ItemStack(item))) {
            items.add(item);
         }
      }

      items.addAll(rules.betsInOrder());
      return List.copyOf(items);
   }

   private static boolean vanilla(Holder<Item> item) {
      return "minecraft".equals(BuiltInRegistries.ITEM.getKey(item.value()).getNamespace());
   }

   private static boolean finishedWithBlock(Holder<Item> item) {
      if (unfinished(item)) {
         return false;
      } else {
         Iterator<TagKey<Item>> tags = item.tags().iterator();

         while(tags.hasNext()) {
            TagKey<Item> tag = (TagKey)tags.next();
            String ingot = material(tag, "ingots/");
            String gem = material(tag, "gems/");
            String block = material(tag, "storage_blocks/");
            if (ingot != null && hasBlock(ingot) || gem != null && hasBlock(gem) || block != null && finished(block)) {
               return true;
            }
         }

         if (!item.is(ItemTags.COALS)) {
            return false;
         } else {
            String coal = BuiltInRegistries.ITEM.getKey(item.value()).getPath();
            return simple(coal) && hasBlock(coal);
         }
      }
   }

   private static boolean unfinished(Holder<Item> item) {
      return item.tags().anyMatch((tag) -> {
         Identifier location = tag.location();
         if (!"c".equals(location.getNamespace())) {
            return false;
         } else {
            String path = location.getPath();

            for(String kind : UNFINISHED) {
               if (path.equals(kind) || path.startsWith(kind + "/")) {
                  return true;
               }
            }

            return false;
         }
      });
   }

   private static String material(TagKey<Item> tag, String kind) {
      Identifier location = tag.location();
      if ("c".equals(location.getNamespace()) && location.getPath().startsWith(kind)) {
         String material = location.getPath().substring(kind.length());
         return simple(material) ? material : null;
      } else {
         return null;
      }
   }

   private static boolean simple(String material) {
      return !material.isEmpty() && material.indexOf(47) < 0 && !material.startsWith("raw_");
   }

   private static boolean hasBlock(String material) {
      for(Holder<Item> block : BuiltInRegistries.ITEM.getTagOrEmpty(common("storage_blocks/" + material))) {
         if (finishedItem(block)) {
            return true;
         }
      }

      return false;
   }

   private static boolean finished(String material) {
      for(String form : FINISHED_FORMS) {
         for(Holder<Item> item : BuiltInRegistries.ITEM.getTagOrEmpty(common(form + material))) {
            if (finishedItem(item)) {
               return true;
            }
         }
      }

      for(Holder<Item> coal : BuiltInRegistries.ITEM.getTagOrEmpty(ItemTags.COALS)) {
         if (material.equals(BuiltInRegistries.ITEM.getKey(coal.value()).getPath()) && finishedItem(coal)) {
            return true;
         }
      }

      return false;
   }

   private static boolean finishedItem(Holder<Item> item) {
      return item.is(BETS) || !vanilla(item) && !unfinished(item);
   }

   static {
      BETS = TagKey.<Item>create(Registries.ITEM, RouletteMod.id("bets"));
      NOT_BETS = TagKey.<Item>create(Registries.ITEM, RouletteMod.id("not_bets"));
      UNFINISHED = List.of("ores", "raw_materials", "nuggets", "dusts");
      FINISHED_FORMS = List.of("ingots/", "gems/", "dusts/");
   }
}
