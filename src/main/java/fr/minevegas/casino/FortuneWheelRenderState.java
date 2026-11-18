package fr.minevegas.casino;

import java.util.Arrays;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

public final class FortuneWheelRenderState extends CasinoNamedRenderState {
   public Direction facing;
   public double turn;
   public double speed;
   public double flapper;
   public float brightness;
   public final float[] bulbs;
   public float panelVisibility;
   public final ItemStackRenderState[] items;
   public final TextureAtlasSprite[] iconSprites;
   public final int[] counts;
   public final boolean[] blockIcons;
   public boolean resultShown;
   public int quarters;
   public FortuneWheelGame.Kind kind;

   public FortuneWheelRenderState() {
      this.facing = Direction.SOUTH;
      this.brightness = 1.0F;
      this.bulbs = new float[16];
      this.items = new ItemStackRenderState[3];
      this.iconSprites = new TextureAtlasSprite[3];
      this.counts = new int[3];
      this.blockIcons = new boolean[3];
      this.kind = FortuneWheelGame.Kind.NONE;
      Arrays.fill(this.bulbs, 1.0F);

      for(int i = 0; i < this.items.length; ++i) {
         this.items[i] = new CasinoBetIcons.Stake();
      }

   }
}
