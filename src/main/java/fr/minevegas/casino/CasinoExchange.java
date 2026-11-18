package fr.minevegas.casino;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class CasinoExchange {
   private CasinoExchange() {
   }

   public static List<ItemStack> payout(ItemStack stake, int numerator, int denominator, int roll, Rule rule, boolean refund) {
      Objects.requireNonNull(stake, "stake");
      if (stake.isEmpty()) {
         return List.of();
      } else if (refund) {
         List<ItemStack> result = new ArrayList();
         add(result, stake, stake.getCount());
         return result;
      } else if (numerator >= 0 && denominator > 0) {
         long amount = Math.multiplyExact((long)stake.getCount(), numerator);
         List<ItemStack> result = new ArrayList();
         Item target = target(stake, rule);
         if (target == null) {
            add(result, stake, share(amount, denominator, roll));
         } else if (!rule.profit()) {
            add(result, new ItemStack(target), share(Math.multiplyExact(amount, rule.count()), denominator, roll));
         } else {
            long principal = Math.multiplyExact((long)stake.getCount(), denominator);
            add(result, stake, share(Math.min(principal, amount), denominator, roll));
            long profit = Math.max(0L, amount - principal);
            add(result, new ItemStack(target), share(Math.multiplyExact(profit, rule.count()), denominator, roll));
         }

         return result;
      } else {
         throw new IllegalArgumentException("Invalid payout multiplier");
      }
   }

   private static Item target(ItemStack stake, Rule rule) {
      if (rule != null && BuiltInRegistries.ITEM.getKey(stake.getItem()).equals(Identifier.parse(rule.stake()))) {
         if (!rule.profit() && rule.count() == 1 && Identifier.parse(rule.stake()).equals(Identifier.parse(rule.item()))) {
            return null;
         } else {
            Item target = (Item)BuiltInRegistries.ITEM.getOptional(Identifier.parse(rule.item())).orElse(Items.AIR);
            return target == Items.AIR ? null : target;
         }
      } else {
         return null;
      }
   }

   private static int share(long amount, int denominator, int roll) {
      long maximum = amount / (long)denominator + (long)(amount % (long)denominator == 0L ? 0 : 1);
      if (maximum > 2147483647L) {
         throw new ArithmeticException("Exchange payment exceeds the supported item count");
      } else {
         return FairPayout.share(amount, denominator, roll);
      }
   }

   private static void add(List<ItemStack> result, ItemStack prototype, int count) {
      if (count > 0) {
         int maximum = Math.max(1, prototype.getMaxStackSize());

         for(ItemStack existing : result) {
            if (ItemStack.isSameItemSameComponents(existing, prototype)) {
               int space = Math.max(0, Math.min(maximum, existing.getMaxStackSize()) - existing.getCount());
               int moved = Math.min(space, count);
               existing.grow(moved);
               count -= moved;
               if (count == 0) {
                  return;
               }
            }
         }

         while(count > 0) {
            int size = Math.min(maximum, count);
            result.add(prototype.copyWithCount(size));
            count -= size;
         }

      }
   }

   public static record Rule(String stake, String item, int count, boolean profit) {
      public Rule(String stake, String item, int count, boolean profit) {
         Objects.requireNonNull(stake, "stake");
         Objects.requireNonNull(item, "item");
         if (Identifier.tryParse(stake) != null && Identifier.tryParse(item) != null) {
            if (count >= 1 && count <= 64) {
               this.stake = stake;
               this.item = item;
               this.count = count;
               this.profit = profit;
            } else {
               throw new IllegalArgumentException("Exchange count outside 1..64: " + count);
            }
         } else {
            throw new IllegalArgumentException("Invalid exchange item identifier");
         }
      }
   }
}
