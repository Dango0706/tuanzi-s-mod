package fr.minevegas.casino;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.ClientAsset;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class GuideHiringPreview {
   private static final int CLICK = 32;
   private static final int LOOP = 112;
   private static final Vec3 FROM = new Vec3((double)2.25F, 1.95, 0.65);
   private static final Vec3 HAND = new Vec3(1.05, 2.05, 0.85);
   private static final BlockState[] CONFETTI = new BlockState[]{GuideScene.state("minecraft:yellow_concrete"), GuideScene.state("minecraft:lime_concrete"), GuideScene.state("minecraft:white_concrete"), GuideScene.state("minecraft:red_concrete")};
   private final DemoPlayer player;
   private final LivingEntity croupier;

   public GuideHiringPreview() {
      this(false);
   }

   public GuideHiringPreview(boolean easyNpc) {
      Minecraft mc = Minecraft.getInstance();
      this.player = new DemoPlayer(mc);
      EntityType<? extends Villager> type = (EntityType<? extends Villager>)BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse("minecraft:villager"));
      Villager original = new Villager(type, mc.level);
      this.player.setId(-3);
      original.setId(-4);
      original.setVillagerData(original.getVillagerData().withProfession(BuiltInRegistries.VILLAGER_PROFESSION.wrapAsHolder(RouletteMod.CROUPIER)));
      LivingEntity chosen = original;
      if (easyNpc) {
         try {
            Entity actor = EasyNpcDealerRenderer.createActor(mc.level, EasyNpcDealerRenderer.guideAppearance());
            if (actor instanceof LivingEntity) {
               LivingEntity living = (LivingEntity)actor;
               chosen = living;
            }
         } catch (ReflectiveOperationException error) {
            LogUtils.getLogger().warn((String)"MineVegas: Easy NPC hiring preview unavailable", (Throwable)error);
         }
      }

      this.croupier = chosen;
      pose(this.player, -65.0F);
      pose(this.croupier, 65.0F);
   }

   private static void pose(LivingEntity actor, float yaw) {
      actor.setPos((double)0.0F, (double)64.0F, (double)0.0F);
      actor.setYRot(yaw);
      actor.yBodyRot = actor.yBodyRotO = yaw;
      actor.yHeadRot = actor.yHeadRotO = yaw;
      actor.setXRot(0.0F);
   }

   public Frame frame(float age) {
      float phase = age % 112.0F;
      this.player.gesture = phase >= 24.0F && phase < 34.0F ? (phase - 32.0F + 8.0F) / 10.0F : 0.0F;
      this.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(phase < 32.0F ? RouletteMod.HIRING_CARD : RouletteMod.CROUPIER_BADGE));
      List<Actor> actors = new ArrayList();
      actors.add(new Actor(this.player, 0.65F, 1.0F, 0.65F));
      if (phase < 32.0F) {
         actors.add(new Actor(this.croupier, 2.25F, 1.0F, 0.65F));
      }

      List<Spark> sparks = new ArrayList();
      float elapsed = phase - 32.0F;
      if (elapsed >= 0.0F && elapsed < 22.0F) {
         for(int i = 0; i < 40; ++i) {
            float progress = (elapsed - (float)(i % 8) * 0.8F) / 14.0F;
            if (!(progress < 0.0F) && !(progress > 1.0F)) {
               double eased = (double)(progress * progress * (3.0F - 2.0F * progress));
               double angle = (double)(progress * 9.0F) + (double)i * 1.57;
               Vec3 position = FROM.lerp(HAND, eased).add(Math.cos(angle) * 0.22 * (double)(1.0F - progress), Math.sin((double)progress * Math.PI) * 0.9, Math.sin(angle) * 0.22 * (double)(1.0F - progress));
               sparks.add(new Spark(CONFETTI[i % CONFETTI.length], (float)position.x, (float)position.y, (float)position.z, 0.035F));
            }
         }
      }

      return new Frame(List.copyOf(actors), List.copyOf(sparks));
   }

   public static record Actor(LivingEntity entity, float x, float y, float z) {
   }

   public static record Spark(BlockState state, float x, float y, float z, float size) {
   }

   public static record Frame(List<Actor> actors, List<Spark> sparks) {
      public static final Frame EMPTY = new Frame(List.of(), List.of());
   }

   private static final class DemoPlayer extends RemotePlayer {
      float gesture;
      final HumanoidArm arm;
      private static final PlayerSkin SKIN;

      DemoPlayer(Minecraft mc) {
         super(mc.level, mc.player.getGameProfile());
         this.arm = mc.player.getMainArm();
      }

      public boolean isModelPartShown(PlayerModelPart part) {
         return true;
      }

      public PlayerSkin getSkin() {
         return SKIN;
      }

      public HumanoidArm getMainArm() {
         return this.arm == null ? HumanoidArm.RIGHT : this.arm;
      }

      public float getSwingAnimation(float partial) {
         return this.gesture;
      }

      public LivingEntity.SwingDescription getCurrentSwing() {
         return this.gesture == 0.0F ? null : new LivingEntity.SwingDescription(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, 10);
      }

      static {
         SKIN = new PlayerSkin(new ClientAsset.ResourceTexture(RouletteMod.id("guide_author"), RouletteMod.id("textures/gui/casino_guide/player_skin.png")), (ClientAsset.Texture)null, (ClientAsset.Texture)null, PlayerModelType.WIDE, true);
      }
   }
}

