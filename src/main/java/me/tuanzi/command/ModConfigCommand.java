package me.tuanzi.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.tuanzi.config.CasinoRecipeControl;
import me.tuanzi.config.ModConfig;
import me.tuanzi.util.ModLog;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * 模组配置管理指令 (/tuanzis_mod config ...)。
 */
public final class ModConfigCommand {
    private static final String KEY_ENABLE_CASINO_RECIPES = "enableCasinoRecipes";

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
                                        .executes(ModConfigCommand::queryCasinoRecipes)))
                        .then(Commands.literal("set")
                                .then(Commands.literal(KEY_ENABLE_CASINO_RECIPES)
                                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                .executes(ModConfigCommand::setCasinoRecipes))))
                        .then(Commands.literal("reload")
                                .executes(ModConfigCommand::reloadConfig))));
    }

    private static int queryAll(CommandContext<CommandSourceStack> context) {
        return queryCasinoRecipes(context);
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
        CasinoRecipeControl.reloadServerResources(context.getSource().getServer());

        boolean enabled = ModConfig.isCasinoRecipesEnabled();
        Component status = Component.translatable(
                enabled ? "commands.tuanzis_mod.config.status_on" : "commands.tuanzis_mod.config.status_off"
        );
        context.getSource().sendSuccess(
                () -> Component.translatable("commands.tuanzis_mod.config.reloaded", status),
                true
        );
        ModLog.debug(context.getSource().getEntity(), null,
                "【管理员配置指令】从磁盘重载配置并同步配方 (enableCasinoRecipes=" + enabled + ")");
        return 1;
    }
}
