package fr.minevegas.casino;

import com.mojang.logging.LogUtils;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import me.tuanzi.config.ModConfig;
import me.tuanzi.util.ModLog;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

public final class CroupierOffer {
   private static final Set<UUID> REPORTED = ConcurrentHashMap.newKeySet();

   private CroupierOffer() {
   }

   public static MerchantOffer create() {
      return new MerchantOffer(new ItemCost(Items.EMERALD, 24), new ItemStack(RouletteMod.HIRING_CARD), 64, 2, 0.0F);
   }

   public static void keep(Villager villager) {
      if (villager.level() instanceof ServerLevel && CroupierHiring.isCroupier(villager)) {
         MerchantOffers offers = villager.getOffers();
         if (!ModConfig.isCasinoRecipesEnabled()) {
            boolean removed = offers.removeIf((offer) -> offer.getResult().is(RouletteMod.HIRING_CARD));
            if (removed) {
               ModLog.debug("赌场配方开关已关闭，已移除荷官村民的雇佣卡交易: " + villager.getUUID());
            }

            return;
         }

         for(MerchantOffer offer : offers) {
            if (offer.getResult().is(RouletteMod.HIRING_CARD)) {
               return;
            }
         }

         offers.add(create());
         if (REPORTED.add(villager.getUUID())) {
            LogUtils.getLogger().warn((String)"[MineVegas Casino] The croupier {} had lost his Croupier Hiring Card offer: another mod removed it. The offer is back.", (Object)villager.getUUID());
         }

      }
   }
}
