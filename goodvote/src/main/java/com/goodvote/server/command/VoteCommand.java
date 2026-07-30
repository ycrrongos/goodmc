package com.goodvote.server.command;

import com.goodvote.network.GoodVotePackets;
import com.goodvote.server.GoodVoteServer;
import com.goodvote.vote.VoteSession;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * /vote command - main admin command for managing votes.
 */
public class VoteCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, GoodVoteServer server) {
        dispatcher.register(CommandManager.literal("vote")
                .requires(source -> source.hasPermissionLevel(2))
                // /vote config - open config GUI
                .then(CommandManager.literal("config")
                        .executes(ctx -> openConfig(ctx, server)))
                // /vote reload - reload config
                .then(CommandManager.literal("reload")
                        .executes(ctx -> reloadConfig(ctx, server)))
                // /vote status - show active votes
                .then(CommandManager.literal("status")
                        .executes(ctx -> showStatus(ctx, server)))
                // /vote command <command> - start a vote for a command
                .then(CommandManager.literal("command")
                        .then(CommandManager.argument("command", StringArgumentType.greedyString())
                                .executes(ctx -> startCommandVote(ctx, server))))
        );
    }

    private static int openConfig(CommandContext<ServerCommandSource> ctx, GoodVoteServer server) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("仅玩家可执行此命令"));
            return 0;
        }

        // Send config data first
        GoodVotePackets.ConfigSyncS2CPayload configPayload =
                new GoodVotePackets.ConfigSyncS2CPayload(
                        GoodVotePackets.ConfigData.fromConfig(server.getConfig())
                );
        ServerPlayNetworking.send(player, configPayload);

        // Then open config screen
        ServerPlayNetworking.send(player, new GoodVotePackets.OpenConfigPayload());

        player.sendMessage(Text.literal("已打开投票配置界面").styled(s -> s.withColor(0x55FF55)));
        return 1;
    }

    private static int reloadConfig(CommandContext<ServerCommandSource> ctx, GoodVoteServer server) {
        server.reloadConfig();
        ctx.getSource().sendFeedback(() -> Text.literal("GoodVote 配置已重载"), true);
        return 1;
    }

    private static int showStatus(CommandContext<ServerCommandSource> ctx, GoodVoteServer server) {
        var sessions = server.getVoteManager().getActiveSessions();
        if (sessions.isEmpty()) {
            ctx.getSource().sendFeedback(() -> Text.literal("当前没有进行中的投票"), false);
            return 1;
        }

        ctx.getSource().sendFeedback(() -> Text.literal("=== 进行中的投票 ===").styled(s -> s.withColor(0xFFAA00)), false);
        for (VoteSession session : sessions) {
            String status = String.format("[%s] %s - %s (同意:%d/%d 拒绝:%d 弃权:%d 剩余:%ds)",
                    session.getType().name(),
                    session.getInitiatorName(),
                    session.getContent(),
                    session.getAcceptCount(),
                    server.getConfig().requiredAccepts(session.getEligibleCount()),
                    session.getRejectCount(),
                    session.getAbstainCount(),
                    session.getRemainingMs() / 1000
            );
            ctx.getSource().sendFeedback(() -> Text.literal(status), false);
        }
        return 1;
    }

    private static int startCommandVote(CommandContext<ServerCommandSource> ctx, GoodVoteServer server) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("仅玩家可执行此命令"));
            return 0;
        }

        String command = StringArgumentType.getString(ctx, "command");
        if (!command.startsWith("/")) command = "/" + command;

        VoteSession session = server.startAdminCommandVote(player, command);
        if (session != null) {
            player.sendMessage(Text.literal("已发起投票: " + command).styled(s -> s.withColor(0xFFAA00)));
        }
        return session != null ? 1 : 0;
    }
}
