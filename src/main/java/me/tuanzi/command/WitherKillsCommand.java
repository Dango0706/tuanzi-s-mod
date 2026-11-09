package me.tuanzi.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.tuanzi.util.ModLog;
import me.tuanzi.util.WitherKillManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public class WitherKillsCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("witherkills")
            .requires(source -> true)
            .executes(WitherKillsCommand::querySelf)
            .then(Commands.literal("query")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("targets", EntityArgument.players())
                    .executes(WitherKillsCommand::queryOthers)
                )
            )
            .then(Commands.literal("add")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("targets", EntityArgument.players())
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                        .executes(WitherKillsCommand::addKills)
                    )
                )
            )
            .then(Commands.literal("set")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("targets", EntityArgument.players())
                    .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                        .executes(WitherKillsCommand::setKills)
                    )
                )
            )
            .then(Commands.literal("reset")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("targets", EntityArgument.players())
                    .executes(WitherKillsCommand::resetKills)
                )
            )
        );
    }

    private static int querySelf(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            Component failMsg = Component.translatableWithFallback(
                "commands.tuanzis_mod.witherkills.player_only",
                "该指令只能由玩家执行！"
            );
            source.sendFailure(failMsg);
            return 0;
        }

        final int kills = WitherKillManager.getKills(player);
        int mod = kills % 1000;
        final int remaining = (mod == 0 && kills > 0) ? 1000 : (1000 - mod);

        Component msg = Component.translatableWithFallback(
            "commands.tuanzis_mod.witherkills.query.self",
            "§a当前已累计击杀凋灵（玩家总体伤害占比≥90%%）：§e%s§a 只。距离下一本灵魂绑定附魔书还差：§e%s§a 只。",
            kills, remaining
        );

        source.sendSuccess(() -> msg, false);
        ModLog.debug(player, null, "【指令查询】玩家 " + player.getScoreboardName() + " 查询自身凋灵击杀数: " + kills + "，剩余: " + remaining);
        return kills;
    }

    private static int queryOthers(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");

        for (ServerPlayer target : targets) {
            final int kills = WitherKillManager.getKills(target);
            int mod = kills % 1000;
            final int remaining = (mod == 0 && kills > 0) ? 1000 : (1000 - mod);

            Component msg = Component.translatableWithFallback(
                "commands.tuanzis_mod.witherkills.query.other",
                "§a玩家 §b" + target.getScoreboardName() + "§a 当前已累计击杀凋灵：§e" + kills + "§a 只，距离下一本灵魂绑定附魔书还差：§e" + remaining + "§a 只。",
                target.getScoreboardName(), kills, remaining
            );
            source.sendSuccess(() -> msg, false);
            ModLog.debug(source.getEntity(), target, "【管理员查询】查询玩家 " + target.getScoreboardName() + " 凋灵击杀数: " + kills + "，剩余: " + remaining);
        }
        return targets.size();
    }

    private static int addKills(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int amount = IntegerArgumentType.getInteger(context, "amount");

        for (ServerPlayer target : targets) {
            int newKills = WitherKillManager.addKills(target, amount);
            Component msg = Component.translatableWithFallback(
                "commands.tuanzis_mod.witherkills.add.success",
                "§a已为玩家 §b" + target.getScoreboardName() + "§a 增加 §e" + amount + "§a 只凋灵击杀数，当前总数：§e" + newKills + "§a 只。",
                target.getScoreboardName(), amount, newKills
            );
            source.sendSuccess(() -> msg, true);
            ModLog.debug(source.getEntity(), target, "【管理员调整】为玩家 " + target.getScoreboardName() + " 增加凋灵击杀数: " + amount + "，当前总数: " + newKills);
        }
        return targets.size();
    }

    private static int setKills(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int amount = IntegerArgumentType.getInteger(context, "amount");

        for (ServerPlayer target : targets) {
            WitherKillManager.setKills(target, amount);
            Component msg = Component.translatableWithFallback(
                "commands.tuanzis_mod.witherkills.set.success",
                "§a已将玩家 §b" + target.getScoreboardName() + "§a 的凋灵击杀数设置为：§e" + amount + "§a 只。",
                target.getScoreboardName(), amount
            );
            source.sendSuccess(() -> msg, true);
            ModLog.debug(source.getEntity(), target, "【管理员调整】将玩家 " + target.getScoreboardName() + " 凋灵击杀数设置为: " + amount);
        }
        return targets.size();
    }

    private static int resetKills(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");

        for (ServerPlayer target : targets) {
            WitherKillManager.setKills(target, 0);
            Component msg = Component.translatableWithFallback(
                "commands.tuanzis_mod.witherkills.reset.success",
                "§a已重置玩家 §b" + target.getScoreboardName() + "§a 的凋灵击杀数为 0。",
                target.getScoreboardName()
            );
            source.sendSuccess(() -> msg, true);
            ModLog.debug(source.getEntity(), target, "【管理员重置】已重置玩家 " + target.getScoreboardName() + " 凋灵击杀数为 0");
        }
        return targets.size();
    }
}
