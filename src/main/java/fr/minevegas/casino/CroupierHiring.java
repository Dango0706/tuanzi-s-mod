package fr.minevegas.casino;

import com.mojang.logging.LogUtils;
import fr.minevegas.casino.compat.EasyNpcApi;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

public final class CroupierHiring {
   static final int STREAM_TICKS = 14;
   private static final DustParticleOptions RED_CONFETTI = new DustParticleOptions(14629707, 1.15F);
   private static final DustParticleOptions GOLD_CONFETTI = new DustParticleOptions(16765288, 1.25F);
   private static final DustParticleOptions GREEN_CONFETTI = new DustParticleOptions(3594374, 1.15F);
   private static final List<Stream> STREAMS = new ArrayList();

   private CroupierHiring() {
   }

   static boolean isCroupier(Entity entity) {
      boolean var10000;
      if (entity instanceof Villager villager) {
         if (villager.getVillagerData().profession().is(RouletteMod.CROUPIER_KEY)) {
            var10000 = true;
            return var10000;
         }
      }

      var10000 = false;
      return var10000;
   }

   static InteractionHand cardHand(Player player, InteractionHand clicked) {
      if (player.getItemInHand(clicked).is(RouletteMod.HIRING_CARD)) {
         return clicked;
      } else {
         return clicked == InteractionHand.MAIN_HAND && player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty() && player.getItemInHand(InteractionHand.OFF_HAND).is(RouletteMod.HIRING_CARD) ? InteractionHand.OFF_HAND : null;
      }
   }

   public static InteractionResult onUse(Player player, Level level, InteractionHand hand, Entity entity) {
      if (player.isSpectator()) {
         return InteractionResult.PASS;
      } else if (EasyNpcApi.isNpc(entity)) {
         return hireNpc(player, level, hand, entity);
      } else if (!isCroupier(entity)) {
         return InteractionResult.PASS;
      } else {
         InteractionHand cardHand = cardHand(player, hand);
         if (cardHand == null) {
            return InteractionResult.PASS;
         } else if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
         } else {
            if (player instanceof ServerPlayer) {
               ServerPlayer serverPlayer = (ServerPlayer)player;
               if (level instanceof ServerLevel) {
                  ServerLevel serverLevel = (ServerLevel)level;
                  Villager villager = (Villager)entity;
                  if (villager.isAlive() && !villager.isRemoved()) {
                     if (villager.isTrading()) {
                        serverPlayer.sendOverlayMessage(Component.translatable("message.roulettecasino.croupier_busy"));
                        return InteractionResult.FAIL;
                     }

                     hire(serverPlayer, serverLevel, villager, cardHand);
                     return InteractionResult.SUCCESS;
                  }

                  return InteractionResult.FAIL;
               }
            }

            return InteractionResult.PASS;
         }
      }
   }

   static void hire(ServerPlayer player, ServerLevel level, Villager villager, InteractionHand hand) {
      Vec3 where = villager.getBoundingBox().getCenter();

      for(MemoryModuleType<GlobalPos> memory : Villager.POI_MEMORIES.keySet()) {
         villager.releasePoi(memory);
      }

      SimpleContainer pockets = villager.getInventory();

      for(int i = 0; i < pockets.getContainerSize(); ++i) {
         ItemStack carried = pockets.removeItemNoUpdate(i);
         if (!carried.isEmpty()) {
            Block.popResource(level, villager.blockPosition(), carried);
         }
      }

      villager.discard();
      finishHire(player, level, new ItemStack(RouletteMod.CROUPIER_BADGE), hand, where);
   }

   private static InteractionResult hireNpc(Player player, Level level, InteractionHand clicked, Entity npc) {
      InteractionHand hand = cardHand(player, clicked);
      if (hand == null) {
         return InteractionResult.PASS;
      } else if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      } else {
         if (player instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)player;
            if (level instanceof ServerLevel) {
               ServerLevel server = (ServerLevel)level;
               if (npc.isAlive() && !npc.isRemoved()) {
                  try {
                     if (!EasyNpcApi.canRecruit(npc, player)) {
                        return npcFailure(serverPlayer, "owner");
                     }

                     DealerAppearance appearance = EasyNpcApi.capture(npc);
                     if (appearance.isEmpty()) {
                        return npcFailure(serverPlayer, "model");
                     }

                     Object inventory = EasyNpcApi.call(npc, "getEasyNPCInventory");
                     if (inventory instanceof Container) {
                        Container container = (Container)inventory;
                        if (!container.isEmpty()) {
                           return npcFailure(serverPlayer, "inventory");
                        }
                     }

                     if (npc instanceof LivingEntity) {
                        LivingEntity living = (LivingEntity)npc;

                        for(EquipmentSlot slot : EquipmentSlot.values()) {
                           if (!living.getItemBySlot(slot).isEmpty()) {
                              return npcFailure(serverPlayer, "inventory");
                           }
                        }
                     }

                     ItemStack badge = new ItemStack(RouletteMod.CROUPIER_BADGE);
                     appearance.putOn(badge);
                     Vec3 where = npc.getBoundingBox().getCenter();
                     npc.discard();
                     finishHire(serverPlayer, server, badge, hand, where);
                     return InteractionResult.SUCCESS;
                  } catch (RuntimeException | ReflectiveOperationException error) {
                     LogUtils.getLogger().warn((String)"MineVegas: could not recruit Easy NPC; NPC and card were kept", (Throwable)error);
                     return npcFailure(serverPlayer, "unavailable");
                  }
               }

               return InteractionResult.FAIL;
            }
         }

         return InteractionResult.PASS;
      }
   }

   private static InteractionResult npcFailure(ServerPlayer player, String reason) {
      player.sendOverlayMessage(Component.translatable("message.roulettecasino.easy_npc_" + reason));
      return InteractionResult.FAIL;
   }

   private static void finishHire(ServerPlayer player, ServerLevel level, ItemStack badge, InteractionHand hand, Vec3 where) {
      ItemStack held = player.getItemInHand(hand);
      if (player.isCreative()) {
         give(player, level, badge);
      } else {
         held.shrink(1);
         if (held.isEmpty()) {
            player.setItemInHand(hand, badge);
         } else {
            give(player, level, badge);
         }
      }

      player.getInventory().setChanged();
      player.containerMenu.broadcastChanges();
      burst(level, where);
      level.playSound((Entity)null, where.x, where.y, where.z, SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1.0F, 1.0F);
      level.playSound((Entity)null, where.x, where.y, where.z, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.7F, 1.2F);
      STREAMS.add(new Stream(level, player.getUUID(), where));
   }

   private static void give(ServerPlayer player, ServerLevel level, ItemStack stack) {
      player.getInventory().add(stack);
      if (!stack.isEmpty()) {
         player.spawnAtLocation(level, stack);
      }

   }

   private static void burst(ServerLevel level, Vec3 at) {
      level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, at.x, at.y, at.z, 34, 0.3, 0.45, 0.3, 0.45);
      level.sendParticles(ParticleTypes.POOF, at.x, at.y - 0.2, at.z, 12, (double)0.25F, 0.4, (double)0.25F, 0.04);
      level.sendParticles(RED_CONFETTI, at.x, at.y, at.z, 14, 0.4, 0.55, 0.4, 0.2);
      level.sendParticles(GOLD_CONFETTI, at.x, at.y, at.z, 16, 0.4, 0.55, 0.4, 0.2);
      level.sendParticles(GREEN_CONFETTI, at.x, at.y, at.z, 14, 0.4, 0.55, 0.4, 0.2);
      level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 14, 0.3, (double)0.5F, 0.3, 0.12);
   }

   public static void tickStreams() {
      Iterator<Stream> it = STREAMS.iterator();

      while(it.hasNext()) {
         Stream s;
         ServerPlayer var10000;
         label32: {
            s = (Stream)it.next();
            Player var4 = s.level.getPlayerByUUID(s.player);
            if (var4 instanceof ServerPlayer p) {
               if (!p.isRemoved()) {
                  var10000 = p;
                  break label32;
               }
            }

            var10000 = null;
         }

         ServerPlayer player = var10000;
         if (player != null && player.level() == s.level) {
            Vec3 hands = player.getEyePosition().add(player.getLookAngle().scale(0.55)).add((double)0.0F, -0.45, (double)0.0F);
            double t0 = (double)s.age / (double)14.0F;
            double t1 = (double)(s.age + 1) / (double)14.0F;

            for(int k = 0; k < 4; ++k) {
               double t = Mth.lerp((double)k / (double)4.0F, t0, t1);
               double eased = t * t * ((double)3.0F - (double)2.0F * t);
               Vec3 pos = s.from.lerp(hands, eased).add((double)0.0F, Math.sin(t * Math.PI) * 0.9, (double)0.0F);
               double swirl = t * (double)9.0F + (double)k * 1.57;
               pos = pos.add(Math.cos(swirl) * 0.22 * ((double)1.0F - t), (double)0.0F, Math.sin(swirl) * 0.22 * ((double)1.0F - t));
               Object var20;
               switch (k) {
                  case 0 -> var20 = ParticleTypes.END_ROD;
                  case 1 -> var20 = GOLD_CONFETTI;
                  case 2 -> var20 = ParticleTypes.HAPPY_VILLAGER;
                  default -> var20 = GREEN_CONFETTI;
               }

               ParticleOptions type = (ParticleOptions)var20;
               s.level.sendParticles(type, pos.x, pos.y, pos.z, 1, (double)0.0F, (double)0.0F, (double)0.0F, (double)0.0F);
            }

            if (++s.age > 14) {
               s.level.sendParticles(ParticleTypes.HAPPY_VILLAGER, hands.x, hands.y, hands.z, 10, (double)0.25F, 0.2, (double)0.25F, (double)0.0F);
               s.level.sendParticles(GOLD_CONFETTI, hands.x, hands.y, hands.z, 10, 0.2, 0.2, 0.2, 0.05);
               s.level.playSound((Entity)null, hands.x, hands.y, hands.z, RouletteMod.CARD_FLIP, SoundSource.PLAYERS, 0.9F, 1.0F);
               s.level.playSound((Entity)null, hands.x, hands.y, hands.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9F, 1.4F);
               it.remove();
            }
         } else {
            it.remove();
         }
      }

   }

   private static final class Stream {
      final ServerLevel level;
      final UUID player;
      final Vec3 from;
      int age;

      Stream(ServerLevel level, UUID player, Vec3 from) {
         this.level = level;
         this.player = player;
         this.from = from;
      }
   }
}
