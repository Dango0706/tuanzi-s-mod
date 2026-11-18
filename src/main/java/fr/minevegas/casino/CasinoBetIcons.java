package fr.minevegas.casino;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix3f;
import org.joml.Matrix3fc;

public final class CasinoBetIcons {
   private static final double FLAT_SHARE = 0.1;
   private static final Matrix3fc FULL_LIGHT = new Matrix3f(1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 20.0F, 100.0F, -70.0F);
   private static final float DEPTH = 0.125F;

   private CasinoBetIcons() {
   }

   public static TextureAtlasSprite flatIcon(ItemStack stack, ItemStackRenderState item, RandomSource random) {
      Material.Baked material = item.pickParticleMaterial(random);
      if (material == null) {
         return null;
      } else {
         TextureAtlasSprite sprite = material.sprite();
         if (MissingTextureAtlasSprite.getLocation().equals(sprite.contents().name())) {
            return null;
         } else {
            if (item instanceof Stake) {
               Stake picture = (Stake)item;
               if (!picture.plain()) {
                  return null;
               }
            }

            if (drawnByFace(stack)) {
               return sprite;
            } else {
               AABB box = item.getModelBoundingBox();
               return box.getZsize() <= 0.1 * Math.max(box.getXsize(), box.getYsize()) ? sprite : null;
            }
         }
      }
   }

   public static boolean drawnByFace(ItemStack stack) {
      Item var2 = stack.getItem();
      if (var2 instanceof BlockItem item) {
         if (!(item.getBlock() instanceof EntityBlock)) {
            BlockState state = item.getBlock().defaultBlockState();

            try {
               return Block.isShapeFullBlock(state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) && Minecraft.getInstance().getBlockColors().getTintSources(state).isEmpty();
            } catch (RuntimeException var4) {
               return false;
            }
         }
      }

      return false;
   }

   public static void fullLight(PoseStack pose) {
      pose.last().normal().set(FULL_LIGHT);
   }

   public static void inFrontOfPanel(PoseStack pose) {
      pose.last().pose().scale(1.0F, 1.0F, 0.125F);
   }

   public static final class Stake extends ItemStackRenderState {
      private final List<ItemStackRenderState.LayerRenderState> layers = new ArrayList();

      public ItemStackRenderState.LayerRenderState newLayer() {
         ItemStackRenderState.LayerRenderState layer = super.newLayer();
         this.layers.add(layer);
         return layer;
      }

      public void clear() {
         super.clear();
         this.layers.clear();
      }

      boolean plain() {
         if (this.layers.size() != 1) {
            return false;
         } else {
            IntList tints = ((ItemStackRenderState.LayerRenderState)this.layers.get(0)).tintLayers();

            for(int i = 0; i < tints.size(); ++i) {
               if (tints.getInt(i) != -1) {
                  return false;
               }
            }

            return true;
         }
      }
   }
}
