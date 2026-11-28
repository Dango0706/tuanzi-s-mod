package me.tuanzi.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.tuanzi.config.CasinoRecipeControl;
import me.tuanzi.config.ModConfig;
import me.tuanzi.config.OverworldBuildHeightControl;
import me.tuanzi.util.ModLog;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * 模组配置管理指令 (/tuanzis_mod config ...)。
 */
public final class ModConfigCommand {
    private static final String KEY_ENABLE_CASINO_RECIPES = "enableCasinoRecipes";
    private static final String KEY_TRAUMA_HUD_CONFLICT_MODE = "traumaHudConflictMode";
    private static final String KEY_OVERWORLD_MAX_BUILD_HEIGHT = "overworldMaxBuildHeight";
    private static final String KEY_CANVAS_CREATIVE_MODE = "canvasCreativeMode";
    private static final String KEY_ALLOW_PUBLIC_CANVAS = "allowPublicCanvas";
    private static final String KEY_ALLOW_CANVAS_COPYING = "allowCanvasCopying";

    private ModConfigCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tuanzis_mod")
                .then(Commands.literal("config")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(ModConfigCommand::queryAll)
                        .then(Commands.literal("get")
                                .executes(ModConfigCommand::queryAll)
                                .then(Commands.literal(KEY_ENABLE_CASINO_RECIPES)
                                        .executes(ModConfigCommand::queryCasinoRecipes))
                                .then(Commands.literal(KEY_TRAUMA_HUD_CONFLICT_MODE)
                                        .executes(ModConfigCommand::queryTraumaHudMode))
                                .then(Commands.literal(KEY_OVERWORLD_MAX_BUILD_HEIGHT)
                                        .executes(ModConfigCommand::queryOverworldMaxBuildHeight)))
                        .then(Commands.literal("set")
                                .then(Commands.literal(KEY_ENABLE_CASINO_RECIPES)
                                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                .executes(ModConfigCommand::setCasinoRecipes)))
                                .then(Commands.literal(KEY_TRAUMA_HUD_CONFLICT_MODE)
                                        .then(Commands.literal("above_conflict")
                                                .executes(ctx -> setTraumaHudMode(ctx, ModConfig.TraumaHudConflictMode.ABOVE_CONFLICT)))
                                        .then(Commands.literal("right_center")
                                                .executes(ctx -> setTraumaHudMode(ctx, ModConfig.TraumaHudConflictMode.RIGHT_CENTER))))
                                .then(Commands.literal(KEY_OVERWORLD_MAX_BUILD_HEIGHT)
                                        .then(Commands.argument(
                                                        "height",
                                                        IntegerArgumentType.integer(
                                                                ModConfig.MIN_OVERWORLD_MAX_BUILD_HEIGHT,
                                                                ModConfig.MAX_OVERWORLD_MAX_BUILD_HEIGHT
                                                        )
                                                )
                                                .executes(ModConfigCommand::setOverworldMaxBuildHeight)))
                                .then(Commands.literal(KEY_CANVAS_CREATIVE_MODE)
                                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                .executes(ctx -> {
                                                    boolean val = BoolArgumentType.getBool(ctx, "enabled");
                                                    ModConfig.setCanvasCreativeMode(val);
                                                    me.tuanzi.world.canvas.CanvasSpaceManager.handleConfigHotReload(ctx.getSource().getServer());
                                                    ctx.getSource().sendSuccess(() -> Component.literal("canvasCreativeMode=" + val), true);
                                                    return 1;
                                                })))
                                .then(Commands.literal(KEY_ALLOW_PUBLIC_CANVAS)
                                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                .executes(ctx -> {
                                                    boolean val = BoolArgumentType.getBool(ctx, "enabled");
                                                    ModConfig.setAllowPublicCanvas(val);
                                                    ctx.getSource().sendSuccess(() -> Component.literal("allowPublicCanvas=" + val), true);
                                                    return 1;
                                                })))
                                .then(Commands.literal(KEY_ALLOW_CANVAS_COPYING)
                                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                .executes(ctx -> {
                                                    boolean val = BoolArgumentType.getBool(ctx, "enabled");
                                                    ModConfig.setAllowCanvasCopying(val);
                                                    ctx.getSource().sendSuccess(() -> Component.literal("allowCanvasCopying=" + val), true);
                                                    return 1;
                                                }))))
                        .then(Commands.literal("reload")
                                .executes(ModConfigCommand::reloadConfig))));
    }

    private static int queryAll(CommandContext<CommandSourceStack> context) {
        queryCasinoRecipes(context);
        queryTraumaHudMode(context);
        return queryOverworldMaxBuildHeight(context);
    }

    private static int queryOverworldMaxBuildHeight(CommandContext<CommandSourceStack> context) {
        int height = ModConfig.getOverworldMaxBuildHeight();
        int maxY = height - 1;
        context.getSource().sendSuccess(
                () -> Component.translatable("commands.tuanzis_mod.config.query_overworld_max_build_height", height, maxY),
                false
        );
        return height;
    }

    private static int setOverworldMaxBuildHeight(CommandContext<CommandSourceStack> context) {
        int height = IntegerArgumentType.getInteger(context, "height");
        ModConfig.setOverworldMaxBuildHeight(height);
        int appliedHeight = ModConfig.getOverworldMaxBuildHeight();
        int maxY = appliedHeight - 1;
        OverworldBuildHeightControl.syncToAllPlayers(context.getSource().getServer());
        context.getSource().sendSuccess(
                () -> Component.translatable("commands.tuanzis_mod.config.set_overworld_max_build_height", appliedHeight, maxY),
                true
        );
        ModLog.debug(context.getSource().getEntity(), null,
                "【管理员配置指令】设置 overworldMaxBuildHeight=" + appliedHeight + " (maxY=" + maxY + ")");
        return appliedHeight;
    }

    private static int queryTraumaHudMode(CommandContext<CommandSourceStack> context) {
        ModConfig.TraumaHudConflictMode mode = ModConfig.getTraumaHudConflictMode();
        Component modeLabel = Component.translatable(
                mode == ModConfig.TraumaHudConflictMode.RIGHT_CENTER
                        ? "gui.tuanzis_mod.config.trauma_hud_conflict_mode.right_center"
                        : "gui.tuanzis_mod.config.trauma_hud_conflict_mode.above_conflict"
        );
        context.getSource().sendSuccess(
                () -> Component.translatable("commands.tuanzis_mod.config.query_trauma_hud_mode", modeLabel),
                false
        );
        return 1;
    }

    private static int setTraumaHudMode(CommandContext<CommandSourceStack> context, ModConfig.TraumaHudConflictMode mode) {
        ModConfig.setTraumaHudConflictMode(mode);
        Component modeLabel = Component.translatable(
                mode == ModConfig.TraumaHudConflictMode.RIGHT_CENTER
                        ? "gui.tuanzis_mod.config.trauma_hud_conflict_mode.right_center"
                        : "gui.tuanzis_mod.config.trauma_hud_conflict_mode.above_conflict"
        );
        context.getSource().sendSuccess(
                () -> Component.translatable("commands.tuanzis_mod.config.set_trauma_hud_mode", modeLabel),
                true
        );
        ModLog.debug(context.getSource().getEntity(), null,
                "【管理员配置指令】设置 traumaHudConflictMode=" + mode);
        return 1;
    }

    private static int queryCasinoRecipes(CommandContext<CommandSourceStack> context) {
        boolean enabled = ModConfig.isCasinoRecipesEnabled();
        Component status = Component.translatable(
                enabled ? "commands.tuanzis_mod.config.status_on" : "commands.tuanzis_mod.config.status_off"
        );
        context.getSource().sendSuccess(
                () -> Component.translatable("commands.tuanzis_mod.config.query_casino_recipes", status),
                false
        );
        return enabled ? 1 : 0;
    }

    private static int setCasinoRecipes(CommandContext<CommandSourceStack> context) {
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        ModConfig.setCasinoRecipesEnabled(enabled);
        CasinoRecipeControl.reloadServerResources(context.getSource().getServer());

        Component status = Component.translatable(
                enabled ? "commands.tuanzis_mod.config.status_on" : "commands.tuanzis_mod.config.status_off"
        );
        context.getSource().sendSuccess(
                () -> Component.translatable("commands.tuanzis_mod.config.set_casino_recipes", status),
                true
        );
        ModLog.debug(context.getSource().getEntity(), null,
                "【管理员配置指令】设置 enableCasinoRecipes=" + enabled + " 并触发配方重载");
        return 1;
    }

    private static int reloadConfig(CommandContext<CommandSourceStack> context) {
        ModConfig.load();
        OverworldBuildHeightControl.syncToAllPlayers(context.getSource().getServer());
        CasinoRecipeControl.reloadServerResources(context.getSource().getServer());
        me.tuanzi.world.canvas.CanvasSpaceManager.handleConfigHotReload(context.getSource().getServer());

        boolean enabled = ModConfig.isCasinoRecipesEnabled();
        int maxBuildHeight = ModConfig.getOverworldMaxBuildHeight();
        Component status = Component.translatable(
                enabled ? "commands.tuanzis_mod.config.status_on" : "commands.tuanzis_mod.config.status_off"
        );
        context.getSource().sendSuccess(
                () -> Component.translatable("commands.tuanzis_mod.config.reloaded", status, maxBuildHeight, maxBuildHeight - 1),
                true
        );
        ModLog.debug(context.getSource().getEntity(), null,
                "【管理员配置指令】从磁盘重载配置并同步配方与主世界建筑高度 (enableCasinoRecipes=" + enabled
                        + ", overworldMaxBuildHeight=" + maxBuildHeight + ")");
        return 1;
    }
}
