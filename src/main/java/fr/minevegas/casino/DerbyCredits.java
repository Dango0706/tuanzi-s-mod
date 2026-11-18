package fr.minevegas.casino;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class DerbyCredits extends SavedData {
   private static final Codec<DerbyCredits> CODEC;
   private static final SavedDataType<DerbyCredits> TYPE;
   private final Map<String, Credit> credits = new LinkedHashMap();

   public DerbyCredits() {
   }

   private DerbyCredits(Map<String, Credit> data) {
      this.credits.putAll(data);
   }

   public static DerbyCredits get(MinecraftServer server) {
      return (DerbyCredits)server.overworld().getDataStorage().computeIfAbsent(TYPE);
   }

   private static List<ItemStack> copies(List<ItemStack> stacks) {
      return stacks.stream().filter((s) -> !s.isEmpty()).map(ItemStack::copy).toList();
   }

   public String hold(ServerPlayer owner, List<ItemStack> stacks, long deadline, BlockPos origin) {
      String id = UUID.randomUUID().toString();
      this.credits.put(id, new Credit(owner.getUUID().toString(), copies(stacks), true, deadline, owner.level().dimension().identifier().toString(), origin.asLong(), true));
      this.setDirty();
      return id;
   }

   public boolean held(String id) {
      Credit c = (Credit)this.credits.get(id);
      return c != null && c.held;
   }

   public List<ItemStack> stakes(String id) {
      Credit c = (Credit)this.credits.get(id);
      return c == null ? List.of() : copies(c.stacks);
   }

   public void revalidate(String id, DuckDerbyBlockEntity machine) {
      Credit c = (Credit)this.credits.get(id);
      if (c != null && c.held) {
         List<ItemStack> accepted = new ArrayList();
         List<ItemStack> rejected = new ArrayList();

         for(ItemStack stack : c.stacks) {
            (CasinoSettings.accepts((BlockEntity)machine, stack) ? accepted : rejected).add(stack.copy());
         }

         if (!rejected.isEmpty()) {
            if (accepted.isEmpty()) {
               this.credits.remove(id);
            } else {
               this.credits.put(id, new Credit(c.owner, List.copyOf(accepted), true, c.due, c.dimension, c.origin, true));
            }

            this.credits.put(UUID.randomUUID().toString(), new Credit(c.owner, List.copyOf(rejected), false, 0L, c.dimension, c.origin, true));
            this.setDirty();
         }
      }
   }

   public boolean settle(String id, List<ItemStack> result, long due) {
      Credit c = (Credit)this.credits.get(id);
      if (c != null && c.held) {
         if (result.isEmpty()) {
            this.credits.remove(id);
         } else {
            this.credits.put(id, new Credit(c.owner, copies(result), false, due, c.dimension, c.origin, false));
         }

         this.setDirty();
         return true;
      } else {
         return false;
      }
   }

   public void release(String id) {
      Credit c = (Credit)this.credits.get(id);
      if (c != null && c.held) {
         this.credits.put(id, c.released());
         this.setDirty();
      }

   }

   public void makeDue(String id) {
      Credit c = (Credit)this.credits.get(id);
      if (c != null && !c.held) {
         this.credits.put(id, new Credit(c.owner, c.stacks, false, 0L, c.dimension, c.origin, c.refund));
         this.setDirty();
      }

   }

   public void refund(ServerPlayer player, List<ItemStack> stacks, BlockPos origin) {
      if (!stacks.stream().allMatch(ItemStack::isEmpty)) {
         String id = this.hold(player, stacks, 0L, origin);
         this.release(id);
         this.pay(player, true);
      }
   }

   public void refund(UUID owner, List<ItemStack> stacks, MinecraftServer server, String dimension, BlockPos origin) {
      if (!stacks.stream().allMatch(ItemStack::isEmpty)) {
         this.credits.put(UUID.randomUUID().toString(), new Credit(owner.toString(), copies(stacks), false, 0L, dimension, origin.asLong(), true));
         this.setDirty();
         ServerPlayer player = server.getPlayerList().getPlayer(owner);
         if (player != null) {
            this.pay(player, true);
         }

      }
   }

   public int available(ServerPlayer p) {
      long now = p.level().getServer().overworld().getGameTime();
      return (int)this.credits.values().stream().filter((c) -> !c.held && c.due <= now && c.owner.equals(p.getUUID().toString())).count();
   }

   private static void insert(ServerPlayer player, ItemStack remainder) {
      NonNullList<ItemStack> items = player.getInventory().getNonEquipmentItems();

      for(ItemStack slot : items) {
         if (remainder.isEmpty()) {
            break;
         }

         if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, remainder)) {
            int count = Math.min(remainder.getCount(), Math.max(0, slot.getMaxStackSize() - slot.getCount()));
            slot.grow(count);
            remainder.shrink(count);
         }
      }

      for(int i = 0; i < items.size() && !remainder.isEmpty(); ++i) {
         if (((ItemStack)items.get(i)).isEmpty()) {
            int count = Math.min(remainder.getCount(), remainder.getMaxStackSize());
            items.set(i, remainder.copyWithCount(count));
            remainder.shrink(count);
         }
      }

      player.getInventory().setChanged();
   }

   public void pay(ServerPlayer player, boolean claim) {
      if (player.isAlive() && !player.isRemoved()) {
         long now = player.level().getServer().overworld().getGameTime();
         Iterator<Map.Entry<String, Credit>> iterator = this.credits.entrySet().iterator();

         while(iterator.hasNext()) {
            Map.Entry<String, Credit> entry = (Map.Entry)iterator.next();
            Credit c = (Credit)entry.getValue();
            if (!c.held && c.due <= now && c.owner.equals(player.getUUID().toString())) {
               BlockPos origin = BlockPos.of(c.origin);
               boolean nearby = player.level().dimension().identifier().toString().equals(c.dimension) && player.distanceToSqr((double)origin.getX() + (double)0.5F, (double)origin.getY() + (double)0.5F, (double)origin.getZ() + (double)0.5F) <= (double)256.0F;
               if (claim || c.refund || nearby) {
                  List<ItemStack> left = new ArrayList();
                  int delivered = 0;

                  for(ItemStack original : c.stacks) {
                     ItemStack remainder = original.copy();
                     int before = remainder.getCount();
                     insert(player, remainder);
                     delivered += before - remainder.getCount();
                     if (!remainder.isEmpty()) {
                        left.add(remainder);
                     }
                  }

                  if (left.isEmpty()) {
                     iterator.remove();
                  } else {
                     entry.setValue(new Credit(c.owner, List.copyOf(left), false, c.due, c.dimension, c.origin, c.refund));
                  }

                  if (delivered > 0) {
                     this.setDirty();
                     player.inventoryMenu.broadcastChanges();
                     if (nearby) {
                        player.level().playSound((Entity)null, origin, RouletteMod.DUCK_PAYOUT, SoundSource.BLOCKS, 0.43F, 1.0F);
                     }

                     if (nearby) {
                        for(int n = 1; n <= 5; ++n) {
                           double f = (double)n / (double)5.0F;
                           player.level().sendParticles(ParticleTypes.HAPPY_VILLAGER, (double)origin.getX() + (double)0.5F + (player.getX() - (double)origin.getX() - (double)0.5F) * f, (double)(origin.getY() + 1) + (player.getY() + 0.7 - (double)origin.getY() - (double)1.0F) * f, (double)origin.getZ() + (double)0.5F + (player.getZ() - (double)origin.getZ() - (double)0.5F) * f, 1, 0.03, 0.03, 0.03, (double)0.0F);
                        }
                     }

                     player.sendOverlayMessage(Component.translatable("message.roulettecasino.derby.received", delivered));
                  }
               }
            }
         }

      }
   }

   public static void started(MinecraftServer server) {
      DerbyCredits d = get(server);
      d.credits.replaceAll((id, c) -> c.held ? c.released() : c);
      d.setDirty();
   }

   public static void tick(MinecraftServer server) {
      DerbyCredits d = get(server);
      long now = server.overworld().getGameTime();
      d.credits.replaceAll((id, c) -> {
         if (c.held && c.due <= now) {
            d.setDirty();
            return c.released();
         } else {
            return c;
         }
      });
      if (now % 20L == 0L) {
         for(ServerPlayer player : server.getPlayerList().getPlayers()) {
            d.pay(player, false);
         }
      }

   }

   static {
      CODEC = Codec.unboundedMap(Codec.STRING, DerbyCredits.Credit.CODEC).xmap(DerbyCredits::new, (d) -> Map.copyOf(d.credits));
      TYPE = new SavedDataType<DerbyCredits>(RouletteMod.id("derby_credits"), DerbyCredits::new, CODEC, DataFixTypes.LEVEL);
   }

   private static record Credit(String owner, List<ItemStack> stacks, boolean held, long due, String dimension, long origin, boolean refund) {
      static final Codec<Credit> CODEC = RecordCodecBuilder.<Credit>create((i) -> i.group(Codec.STRING.fieldOf("owner").forGetter(Credit::owner), ItemStack.CODEC.listOf().fieldOf("stacks").forGetter(Credit::stacks), Codec.BOOL.fieldOf("held").forGetter(Credit::held), Codec.LONG.fieldOf("due").forGetter(Credit::due), Codec.STRING.fieldOf("dimension").forGetter(Credit::dimension), Codec.LONG.fieldOf("origin").forGetter(Credit::origin), Codec.BOOL.fieldOf("refund").forGetter(Credit::refund)).apply(i, Credit::new));

      Credit released() {
         return new Credit(this.owner, this.stacks, false, 0L, this.dimension, this.origin, true);
      }
   }
}
