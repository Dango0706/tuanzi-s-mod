package me.tuanzi.config;

import me.tuanzi.util.ModLog;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * 赌场核心玩法配方与获取途径控制。
 * <p>
 * 仅控制 7 种游戏机台、专属机械组件、荷官工作台及赌场指南；
 * 座椅、电视、音箱、casinodeco 装饰方块与本模组筹码/钱币不受影响。
 */
public final class CasinoRecipeControl {
    private static final String ROULETTE_NAMESPACE = "roulettecasino";
    private static final String DUCK_DERBY_ORDER_PREFIX = "duck_derby_mechanism_order_";

    private static final Set<String> CORE_CASINO_RECIPE_PATHS = Set.of(
            "roulette_table",
            "roulette_mechanism",
            "blackjack_table",
            "poker_table",
            "duck_derby",
            "duck_derby_mechanism",
            "derby_duck_red",
            "derby_duck_yellow",
            "derby_duck_black",
            "derby_duck_white",
            "slot_machine",
            "slot_machine_mechanism",
            "slot_machine_reel",
            "fortune_wheel",
            "plinko",
            "dealer_station",
            "casino_guide"
    );

    private CasinoRecipeControl() {
    }

    public static boolean isCoreCasinoRecipe(Identifier recipeId) {
        if (recipeId == null || !ROULETTE_NAMESPACE.equals(recipeId.getNamespace())) {
            return false;
        }
        String path = recipeId.getPath();
        return CORE_CASINO_RECIPE_PATHS.contains(path) || path.startsWith(DUCK_DERBY_ORDER_PREFIX);
    }

    public static boolean isRecipeBlocked(Identifier recipeId) {
        return !ModConfig.isCasinoRecipesEnabled() && isCoreCasinoRecipe(recipeId);
    }

    public static CompletableFuture<Void> reloadServerResources(MinecraftServer server) {
        ModLog.debug("触发服务端数据包重载以应用赌场配方开关 (enableCasinoRecipes="
                + ModConfig.isCasinoRecipesEnabled() + ")");
        return server.reloadResources(server.getPackRepository().getSelectedIds())
                .exceptionally(throwable -> {
                    ModLog.error("[CasinoRecipeControl] 重载服务端数据包失败: " + throwable.getMessage(), throwable);
                    return null;
                });
    }
}
