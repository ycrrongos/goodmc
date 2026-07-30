package com.goodvote.server.command;

import com.goodvote.server.GoodVoteServer;
import com.goodvote.vote.VoteSession;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * /votecmd command - allows regular players to request execution of admin commands via voting.
 */
public class PlayerRequestCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, GoodVoteServer server) {
        dispatcher.register(CommandManager.literal("votecmd")
                .then(CommandManager.argument("command", StringArgumentType.greedyString())
                        .executes(ctx -> requestCommand(ctx, server)))
        );
    }

    private static int requestCommand(CommandContext<ServerCommandSource> ctx, GoodVoteServer server) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("仅玩家可执行此命令"));
            return 0;
        }

        if (!server.getConfig().isPlayerRequestEnabled()) {
            player.sendMessage(Text.literal("玩家请求执行命令功能已被禁用").styled(s -> s.withColor(0xFF5555)));
            return 0;
        }

        String command = StringArgumentType.getString(ctx, "command");
        if (!command.startsWith("/")) command = "/" + command;

        // Check if the command is allowed for player requests
        if (!server.getConfig().isPlayerRequestAllowed(command)) {
            player.sendMessage(Text.literal("该命令不允许被请求执行").styled(s -> s.withColor(0xFF5555)));
            return 0;
        }

        // Check target selectors
        if (!server.getConfig().canEnterVoting(command)) {
            player.sendMessage(Text.literal("该命令包含目标选择器，已被禁止").styled(s -> s.withColor(0xFF5555)));
            return 0;
        }

        VoteSession session = server.startPlayerRequestVote(player, command);
        if (session != null) {
            player.sendMessage(Text.literal("已发起请求投票: " + command).styled(s -> s.withColor(0xFFAA00)));
            return 1;
        }
        return 0;
    }
}
