package fr.minevegas.casino;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public final class CasinoGuideItem extends Item {
   public CasinoGuideItem(Item.Properties properties) {
      super(properties);
   }

   public InteractionResult use(Level level, Player player, InteractionHand hand) {
      return InteractionResult.SUCCESS;
   }
}
