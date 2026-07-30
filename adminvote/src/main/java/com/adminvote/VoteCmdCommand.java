package com.adminvote;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * /votecmd <command> - 普通玩家请求执行管理指令，发起投票
 */
public final class VoteCmdCommand implements CommandExecutor {

    private final AdminCommandVoteManager voteManager;

    public VoteCmdCommand(AdminCommandVoteManager voteManager) {
        this.voteManager = voteManager;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("该命令只能由玩家使用。", NamedTextColor.RED));
            return true;
        }

        if (!voteManager.config().playerRequestEnabled()) {
            player.sendMessage(Component.text("玩家请求执行命令功能已被禁用。", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(Component.text("用法: /votecmd <指令>", NamedTextColor.RED));
            return true;
        }

        // reconstruct command line
        StringBuilder sb = new StringBuilder();
        for (String arg : args) sb.append(arg).append(' ');
        String cmdLine = "/" + sb.toString().trim();

        if (!voteManager.config().isPlayerRequestAllowed(cmdLine)) {
            player.sendMessage(Component.text("该命令不允许被请求执行。", NamedTextColor.RED));
            return true;
        }

        if (!voteManager.config().canEnterVoting(cmdLine)) {
            player.sendMessage(Component.text("该命令包含目标选择器，已被禁止。", NamedTextColor.RED));
            return true;
        }

        // check if player already has an active vote
        if (voteManager.getVote(player.getUniqueId()).isPresent()) {
            player.sendMessage(Component.text("你已经有一个进行中的投票。", NamedTextColor.RED));
            return true;
        }

        var vote = voteManager.beginVote(player, cmdLine, AdminCommandVote.VoteType.PLAYER_REQUEST);
        if (vote.isPresent()) {
            player.sendMessage(Component.text("已发起请求投票: ", NamedTextColor.YELLOW)
                    .append(Component.text(cmdLine, NamedTextColor.AQUA)));
        }
        return true;
    }
}
