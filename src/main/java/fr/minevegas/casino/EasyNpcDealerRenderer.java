package fr.minevegas.casino;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import fr.minevegas.casino.compat.EasyNpcApi;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public final class EasyNpcDealerRenderer {
   private final Map<DealerAppearance, Entity> actors = new LinkedHashMap<DealerAppearance, Entity>(16, 0.75F, true) {
      {
         Objects.requireNonNull(EasyNpcDealerRenderer.this);
      }

      protected boolean removeEldestEntry(Map.Entry<DealerAppearance, Entity> entry) {
         return this.size() > 64;
      }
   };
   private static final Set<EasyNpcDealerRenderer> ACTIVE = Collections.newSetFromMap(new WeakHashMap());
   private WeakReference<Level> world = new WeakReference((Object)null);
   private boolean reported;
   private final BlackjackDealerModel normal = model(humanoid(false, true));
   private final BlackjackDealerModel slim = model(humanoid(true, true));
   private final BlackjackDealerModel normalBare = model(humanoid(false, false));
   private final BlackjackDealerModel slimBare = model(humanoid(true, false));
   private final BlackjackDealerModel villager = model(BlackjackDealerModel.createLayer());

   public EasyNpcDealerRenderer() {
      ACTIVE.add(this);
   }

   public static void onClientLevel(Level level) {
      for(EasyNpcDealerRenderer renderer : ACTIVE) {
         if (renderer.world.get() != level) {
            renderer.actors.clear();
            renderer.world = new WeakReference(level);
         }
      }

   }

   private static BlackjackDealerModel model(LayerDefinition layer) {
      return new BlackjackDealerModel(layer.bakeRoot(), RenderTypes::entityCutout);
   }

   public Prepared prepare(Level level, DealerAppearance appearance) {
      if (!appearance.isEmpty() && level != null && EasyNpcApi.available()) {
         if (this.world.get() != level) {
            this.actors.clear();
            this.world = new WeakReference(level);
         }

         try {
            Entity actor = (Entity)this.actors.get(appearance);
            if (actor == null) {
               actor = createActor(level, appearance);
               if (actor == null) {
                  return null;
               }

               this.actors.put(appearance, actor);
            }

            Object renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(actor);
            Class<?> contract = Class.forName("de.markusbordihn.easynpc.client.renderer.entity.EasyNPCEntityRenderer");
            Class<?> npc = Class.forName("de.markusbordihn.easynpc.entity.easynpc.EasyNPC");
            Identifier texture = (Identifier)contract.getMethod("getEntityPlayerTexture", npc).invoke(renderer, actor);
            return texture == null ? null : new Prepared(texture, appearance.model(), appearance.skin().get("DisableLayers") == null || !(Boolean)Codec.BOOL.parse(NbtOps.INSTANCE, appearance.skin().get("DisableLayers")).result().orElse(false));
         } catch (LinkageError | RuntimeException | ReflectiveOperationException error) {
            if (!this.reported) {
               this.reported = true;
               LogUtils.getLogger().warn((String)"MineVegas: Easy NPC appearance could not be resolved; keeping its saved profile", (Throwable)error);
            }

            return null;
         }
      } else {
         return null;
      }
   }

   public static Entity createActor(Level level, DealerAppearance appearance) throws ReflectiveOperationException {
      if (!appearance.isEmpty() && EasyNpcApi.available()) {
         EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.tryParse(appearance.entityType()));
         Entity actor = type == null ? null : type.create(level, EntitySpawnReason.COMMAND);
         if (actor != null && EasyNpcApi.isNpc(actor)) {
            actor.setId(-1 - (appearance.hashCode() & 1073741823));
            EasyNpcApi.apply(actor, appearance);
            return actor;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public static DealerAppearance guideAppearance() {
      CompoundTag skin = new CompoundTag();
      skin.putString("Type", "DEFAULT");
      return new DealerAppearance("easy_npc:humanoid", "HUMANOID", "STEVE", skin, "Easy NPC");
   }

   public boolean submit(Prepared appearance, BlackjackDealerModel.State motion, PoseStack pose, SubmitNodeCollector output, int light) {
      if (appearance == null) {
         return false;
      } else {
         BlackjackDealerModel model = appearance.model().equals("VILLAGER") ? this.villager : (appearance.model().equals("HUMANOID_SLIM") ? (appearance.layers() ? this.slim : this.slimBare) : (appearance.layers() ? this.normal : this.normalBare));
         output.submitModel(model, motion, pose, RenderTypes.entityCutout(appearance.texture()), light, OverlayTexture.NO_OVERLAY, 0);
         return true;
      }
   }

   private static LayerDefinition humanoid(boolean slim, boolean layers) {
      MeshDefinition mesh = new MeshDefinition();
      PartDefinition root = mesh.getRoot();
      PartDefinition torso = root.addOrReplaceChild("torso", CubeListBuilder.create(), PartPose.offset(0.0F, 12.0F, 0.0F));
      CubeListBuilder body = CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, -12.0F, -2.0F, 8.0F, 12.0F, 4.0F);
      if (layers) {
         body.texOffs(16, 32).addBox(-4.0F, -12.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.25F));
      }

      torso.addOrReplaceChild("body", body, PartPose.ZERO);
      CubeListBuilder head = CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F);
      if (layers) {
         head.texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.5F));
      }

      torso.addOrReplaceChild("head", head, PartPose.offset(0.0F, -12.0F, 0.0F));
      int width = slim ? 3 : 4;

      for(boolean left : new boolean[]{false, true}) {
         String side = left ? "left" : "right";
         int u = left ? 32 : 40;
         int v = left ? 48 : 16;
         int overlayU = left ? 48 : 40;
         int overlayV = left ? 48 : 32;
         CubeListBuilder upper = CubeListBuilder.create().texOffs(u, v).addBox(-2.0F, -2.0F, -2.0F, (float)width, 6.0F, 4.0F);
         if (layers) {
            upper.texOffs(overlayU, overlayV).addBox(-2.0F, -2.0F, -2.0F, (float)width, 6.0F, 4.0F, new CubeDeformation(0.25F));
         }

         PartDefinition arm = torso.addOrReplaceChild(side + "_arm", upper, PartPose.offset(left ? 6.0F : -6.0F, -10.0F, 0.0F));
         CubeListBuilder lower = CubeListBuilder.create().texOffs(u, v + 6).addBox(-2.0F, 0.0F, -2.0F, (float)width, 6.0F, 4.0F, new CubeDeformation(-0.04F, 0.0F, 0.0F));
         if (layers) {
            lower.texOffs(overlayU, overlayV + 6).addBox(-2.0F, 0.0F, -2.0F, (float)width, 6.0F, 4.0F, new CubeDeformation(0.21F, 0.25F, 0.25F));
         }

         arm.addOrReplaceChild(side + "_forearm", lower, PartPose.offset(0.0F, 4.0F, 0.0F));
         CubeListBuilder leg = CubeListBuilder.create().texOffs(left ? 16 : 0, left ? 48 : 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F);
         if (layers) {
            leg.texOffs(0, left ? 48 : 32).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new CubeDeformation(0.25F));
         }

         root.addOrReplaceChild(side + "_leg", leg, PartPose.offset(left ? 2.0F : -2.0F, 12.0F, 0.0F));
      }

      return LayerDefinition.create(mesh, 64, 64);
   }

   public static record Prepared(Identifier texture, String model, boolean layers) {
   }
}
