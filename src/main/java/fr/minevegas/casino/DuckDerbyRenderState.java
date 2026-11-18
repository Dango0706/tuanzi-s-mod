package fr.minevegas.casino;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

public final class DuckDerbyRenderState extends CasinoNamedRenderState {
   public Direction facing;
   public final DuckDerbyRace.Pose[] poses;
   public float gate;
   public double lightTime;
   public float panelVisibility;
   public float enrollmentProgress;
   public boolean shared;
   public boolean enrolling;
   public int members;
   public int seconds;
   public int bettorCount;
   public final List<Bettor> bettors;
   public final ItemStackRenderState[] items;
   public final TextureAtlasSprite[] iconSprites;
   public final int[] counts;
   public final boolean[] blockIcons;
   public int winnerShown;
   public int secondShown;

   public DuckDerbyRenderState() {
      this.facing = Direction.SOUTH;
      this.poses = new DuckDerbyRace.Pose[4];
      this.bettors = new ArrayList();
      this.items = new ItemStackRenderState[12];
      this.iconSprites = new TextureAtlasSprite[12];
      this.counts = new int[12];
      this.blockIcons = new boolean[12];
      this.winnerShown = -1;
      this.secondShown = -1;

      for(int i = 0; i < 8; ++i) {
         this.bettors.add(new Bettor());
      }

      for(int i = 0; i < this.items.length; ++i) {
         this.items[i] = new CasinoBetIcons.Stake();
      }

      for(int i = 0; i < this.poses.length; ++i) {
         this.poses[i] = new DuckDerbyRace.Pose();
      }

   }

   public static final class Bettor {
      public String name = "";
      public int lane;
      public Identifier skin;
      public final ItemStackRenderState[] items = new ItemStackRenderState[]{new CasinoBetIcons.Stake(), new CasinoBetIcons.Stake(), new CasinoBetIcons.Stake()};
      public final TextureAtlasSprite[] sprites = new TextureAtlasSprite[3];
      public final boolean[] blocks = new boolean[3];
      public final int[] counts = new int[3];
   }
}
