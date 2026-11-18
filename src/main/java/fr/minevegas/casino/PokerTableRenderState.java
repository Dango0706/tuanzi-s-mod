package fr.minevegas.casino;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

public final class PokerTableRenderState extends CasinoNamedRenderState {
   public static final int STAKES = 3;
   public Direction facing;
   public final BlackjackDealerModel.State dealer;
   public EasyNpcDealerRenderer.Prepared easyNpcDealer;
   public int dealerLight;
   public PokerTableState view;
   public double now;
   public float panelVisibility;
   public String owner;
   public final ItemStackRenderState[] stakeItems;
   public final TextureAtlasSprite[] stakeSprites;
   public final int[] stakeCounts;
   public final boolean[] stakeBlocks;
   public int stakeKinds;
   public int playerHand;
   public int dealerHand;
   public boolean dealerQualifies;
   public int result;

   public PokerTableRenderState() {
      this.facing = Direction.SOUTH;
      this.dealer = new BlackjackDealerModel.State();
      this.owner = "";
      this.stakeItems = new ItemStackRenderState[3];
      this.stakeSprites = new TextureAtlasSprite[3];
      this.stakeCounts = new int[3];
      this.stakeBlocks = new boolean[3];
      this.playerHand = -1;
      this.dealerHand = -1;

      for(int i = 0; i < 3; ++i) {
         this.stakeItems[i] = new CasinoBetIcons.Stake();
      }

   }
}
