package me.tuanzi.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.tuanzi.world.scavenger.ScavengerManager;
import me.tuanzi.world.scavenger.team.ScavengerTeamManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.Collection;
import java.util.Collections;

public class CorridorCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("corridor")
                .then(Commands.literal("enter")
                    // 普通玩家自用进入
                    .executes(ctx -> enter(ctx, Collections.singleton(ctx.getSource().getPlayerOrException())))
                    // 管理员指定目标玩家进入
                    .then(Commands.argument("targets", EntityArgument.players())
                        .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                        .executes(ctx -> enter(ctx, EntityArgument.getPlayers(ctx, "targets")))
                    )
                )
                .then(Commands.literal("leave")
                    // 普通玩家自用离开
                    .executes(ctx -> leave(ctx, Collections.singleton(ctx.getSource().getPlayerOrException())))
                    // 管理员指定目标玩家离开
                    .then(Commands.argument("targets", EntityArgument.players())
                        .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                        .executes(ctx -> leave(ctx, EntityArgument.getPlayers(ctx, "targets")))
                    )
                )
                .then(Commands.literal("team")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        ScavengerTeamManager.showTeamInfo(player);
                        return 1;
                    })
                    .then(Commands.literal("info")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            ScavengerTeamManager.showTeamInfo(player);
                            return 1;
                        })
                    )
                    .then(Commands.literal("create")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            return ScavengerTeamManager.createTeam(player) ? 1 : 0;
                        })
                    )
                    .then(Commands.literal("invite")
                        .then(Commands.argument("target", EntityArgument.player())
                            .executes(ctx -> {
                                ServerPlayer player = ctx.getSource().getPlayerOrException();
                                ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                return ScavengerTeamManager.invitePlayer(player, target) ? 1 : 0;
                            })
                        )
                    )
                    .then(Commands.literal("accept")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            return ScavengerTeamManager.acceptInvite(player) ? 1 : 0;
                        })
                    )
                    .then(Commands.literal("deny")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            return ScavengerTeamManager.denyInvite(player) ? 1 : 0;
                        })
                    )
                    .then(Commands.literal("leave")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            return ScavengerTeamManager.leaveTeam(player) ? 1 : 0;
                        })
                    )
                    .then(Commands.literal("kick")
                        .then(Commands.argument("target", EntityArgument.player())
                            .executes(ctx -> {
                                ServerPlayer player = ctx.getSource().getPlayerOrException();
                                ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                return ScavengerTeamManager.kickMember(player, target) ? 1 : 0;
                            })
                        )
                    )
                    .then(Commands.literal("disband")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            return ScavengerTeamManager.disbandTeam(player) ? 1 : 0;
                        })
                    )
                    .then(Commands.literal("color")
                        .then(Commands.argument("color", net.minecraft.commands.arguments.TeamColorArgument.teamColor())
                            .executes(ctx -> {
                                ServerPlayer player = ctx.getSource().getPlayerOrException();
                                net.minecraft.world.scores.TeamColor color = net.minecraft.commands.arguments.TeamColorArgument.getTeamColor(ctx, "color");
                                return ScavengerTeamManager.setTeamColor(player, color) ? 1 : 0;
                            })
                        )
                    )
                    .then(Commands.literal("config")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            ScavengerTeamManager.showTeamConfig(player);
                            return 1;
                        })
                        .then(Commands.literal("wind_charge")
                            .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    boolean enabled = BoolArgumentType.getBool(ctx, "enabled");
                                    return ScavengerTeamManager.setWindChargeAffectsTeammates(player, enabled) ? 1 : 0;
                                })
                            )
                        )
                        .then(Commands.literal("fishing_rod")
                            .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    boolean enabled = BoolArgumentType.getBool(ctx, "enabled");
                                    return ScavengerTeamManager.setFishingHookAffectsTeammates(player, enabled) ? 1 : 0;
                                })
                            )
                        )
                        .then(Commands.literal("friendly_physics")
                            .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    boolean enabled = BoolArgumentType.getBool(ctx, "enabled");
                                    return ScavengerTeamManager.setFriendlyPhysics(player, enabled) ? 1 : 0;
                                })
                            )
                        )
                    )
                )
                .then(Commands.literal("loot")
                    .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                    .then(Commands.literal("reset")
                        .then(Commands.argument("pos", net.minecraft.commands.arguments.coordinates.BlockPosArgument.blockPos())
                            .executes(ctx -> resetLootAt(ctx, net.minecraft.commands.arguments.coordinates.BlockPosArgument.getLoadedBlockPos(ctx, "pos")))
                        )
                    )
                )
        );
    }

    private static int enter(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> players) {
        int count = 0;
        for (ServerPlayer player : players) {
            if (ScavengerManager.enterCorridor(player)) {
                count++;
            }
        }
        if (count > 0 && players.size() > 1) {
            final int finalCount = count;
            context.getSource().sendSuccess(() -> Component.literal("§a成功将 " + finalCount + " 位玩家传送进入拾荒回廊。"), true);
        }
        return count;
    }

    private static int leave(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> players) {
        int count = 0;
        for (ServerPlayer player : players) {
            if (ScavengerManager.leaveCorridor(player)) {
                count++;
            }
        }
        if (count > 0 && players.size() > 1) {
            final int finalCount = count;
            context.getSource().sendSuccess(() -> Component.literal("§a成功将 " + finalCount + " 位玩家带出拾荒回廊。"), true);
        }
        return count;
    }

    private static int resetLootAt(CommandContext<CommandSourceStack> context, net.minecraft.core.BlockPos pos) {
        net.minecraft.server.level.ServerLevel level = context.getSource().getLevel();
        if (level.getBlockEntity(pos) instanceof me.tuanzi.world.scavenger.loot.container.IScavengerLootContainer lootContainer) {
            lootContainer.scavengerLootState().resetForAdmin();
            lootContainer.markContainerChanged();
            context.getSource().sendSuccess(() -> Component.literal("§a成功重置方块 (" + pos.toShortString() + ") 的搜刮战利品箱状态！"), true);
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("§c目标位置 (" + pos.toShortString() + ") 并非合法的拾荒搜刮容器！"));
            return 0;
        }
    }
}
