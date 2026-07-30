package com.adminvote;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * /vote status | /vote reload - 管理员管理投票
 */
public final class VoteManageCommand implements CommandExecutor {

    private final AdminCommandVoteManager voteManager;
    private final AdminVotePlugin plugin;

    public VoteManageCommand(AdminVotePlugin plugin, AdminCommandVoteManager voteManager) {
        this.plugin = plugin;
        this.voteManager = voteManager;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!sender.isOp()) {
            sender.sendMessage(Component.text("你没有权限使用此命令。", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Component.text("用法: /vote <status|reload>", NamedTextColor.RED));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "status" -> {
                sender.sendMessage(voteManager.formatStatus());
            }
            case "reload" -> {
                plugin.pluginConfig().load();
                voteManager.reloadConfig(plugin.pluginConfig());
                sender.sendMessage(Component.text("AdminVote 配置已重载。", NamedTextColor.GREEN));
            }
            default -> sender.sendMessage(Component.text("未知子命令。用法: /vote <status|reload>", NamedTextColor.RED));
        }
        return true;
    }
}
