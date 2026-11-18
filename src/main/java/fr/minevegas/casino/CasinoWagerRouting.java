package fr.minevegas.casino;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class CasinoWagerRouting {
   private CasinoWagerRouting() {
   }

   public static boolean mayUse(BlockEntity machine, SimpleContainer bets, Player player) {
      if (!bets.isEmpty() && machine instanceof CasinoPayoutMachine routed) {
         String owner = routed.payoutTarget().owner();
         if (player == null) {
            return owner.isEmpty();
         } else if (!(player instanceof ServerPlayer)) {
            return true;
         } else {
            return owner.isEmpty() || owner.equals(player.getUUID().toString());
         }
      } else {
         return true;
      }
   }

   public static void changed(BlockEntity machine, SimpleContainer bets, Player player, boolean wasEmpty) {
      if (player instanceof ServerPlayer && machine instanceof CasinoPayoutMachine) {
         CasinoPayoutMachine routed = (CasinoPayoutMachine)machine;
         if (!bets.isEmpty() && (wasEmpty || routed.payoutTarget().owner().isEmpty() || routed.payoutTarget().owner().equals(player.getUUID().toString()))) {
            routed.payoutTarget(CasinoPayoutTarget.of(player));
         }
      }

   }
}
