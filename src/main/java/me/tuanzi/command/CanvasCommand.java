package me.tuanzi.command;

import com.mojang.brigadier.CommandDispatcher;
import me.tuanzi.world.canvas.CanvasSpaceManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

/**
 * 画布独立编辑空间指令 (/canvas ...)。
 */
public final class CanvasCommand {
    private CanvasCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("canvas")
                .then(Commands.literal("exit")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            return CanvasSpaceManager.exitCanvasSpace(player, false) ? 1 : 0;
                        }))
                .then(Commands.literal("border")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            return CanvasSpaceManager.toggleBorder(player, null) ? 1 : 0;
                        })
                        .then(Commands.literal("on")
                                .executes(ctx -> CanvasSpaceManager.toggleBorder(ctx.getSource().getPlayerOrException(), true) ? 1 : 0))
                        .then(Commands.literal("off")
                                .executes(ctx -> CanvasSpaceManager.toggleBorder(ctx.getSource().getPlayerOrException(), false) ? 1 : 0)))
                .then(Commands.literal("noclip")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            return CanvasSpaceManager.toggleNoclip(player, null) ? 1 : 0;
                        })
                        .then(Commands.literal("on")
                                .executes(ctx -> CanvasSpaceManager.toggleNoclip(ctx.getSource().getPlayerOrException(), true) ? 1 : 0))
                        .then(Commands.literal("off")
                                .executes(ctx -> CanvasSpaceManager.toggleNoclip(ctx.getSource().getPlayerOrException(), false) ? 1 : 0)))
                .then(Commands.literal("base")
                        .then(Commands.literal("clear")
                                .executes(ctx -> CanvasSpaceManager.clearOrRestoreSystemBase(ctx.getSource().getPlayerOrException(), false) ? 1 : 0))
                        .then(Commands.literal("restore")
                                .executes(ctx -> CanvasSpaceManager.clearOrRestoreSystemBase(ctx.getSource().getPlayerOrException(), true) ? 1 : 0)))
                .then(Commands.literal("allow")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> CanvasSpaceManager.grantOrRevokeEditor(
                                        ctx.getSource().getPlayerOrException(),
                                        EntityArgument.getPlayer(ctx, "player"),
                                        true) ? 1 : 0)))
                .then(Commands.literal("revoke")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> CanvasSpaceManager.grantOrRevokeEditor(
                                        ctx.getSource().getPlayerOrException(),
                                        EntityArgument.getPlayer(ctx, "player"),
                                        false) ? 1 : 0))));
    }
}
