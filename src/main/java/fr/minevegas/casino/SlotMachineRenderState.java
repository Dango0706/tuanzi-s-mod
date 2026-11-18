package fr.minevegas.casino;

import java.util.Arrays;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

public final class SlotMachineRenderState extends CasinoNamedRenderState {
   public Direction facing;
   public final double[] turn;
   public final double[] speed;
   public float drumBrightness;
   public double lever;
   public float lamp;
   public final float[] bulbs;
   public float panelVisibility;
   public final ItemStackRenderState[] items;
   public final TextureAtlasSprite[] iconSprites;
   public final int[] counts;
   public final boolean[] blockIcons;
   public boolean resultShown;
   public final int[] symbols;
   public int multiplier;
   public SlotMachineGame.Kind kind;

   public SlotMachineRenderState() {
      this.facing = Direction.SOUTH;
      this.turn = new double[3];
      this.speed = new double[3];
      this.drumBrightness = 1.0F;
      this.lever = (double)-14.0F;
      this.lamp = 1.0F;
      this.bulbs = new float[15];
      this.items = new ItemStackRenderState[1];
      this.iconSprites = new TextureAtlasSprite[1];
      this.counts = new int[1];
      this.blockIcons = new boolean[1];
      this.symbols = new int[3];
      this.kind = SlotMachineGame.Kind.NONE;

      for(int i = 0; i < this.items.length; ++i) {
         this.items[i] = new CasinoBetIcons.Stake();
      }

      Arrays.fill(this.bulbs, 1.0F);
   }
}
