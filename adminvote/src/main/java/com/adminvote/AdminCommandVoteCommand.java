package com.adminvote;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AdminCommandVoteCommand implements CommandExecutor, TabCompleter {

    private final AdminCommandVoteManager voteManager;

    public AdminCommandVoteCommand(AdminCommandVoteManager voteManager) {
        this.voteManager = voteManager;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length != 2) {
            sender.sendMessage(Component.text("用法: /adminvote <yes|no> <管理员>", NamedTextColor.RED));
            return true;
        }

        Boolean accepted = parseVote(args[0]);
        if (accepted == null) {
            sender.sendMessage(Component.text("请选择 yes（同意）或 no（拒绝）。", NamedTextColor.RED));
            return true;
        }

        Player admin = Bukkit.getPlayerExact(args[1]);
        if (admin == null) {
            sender.sendMessage(Component.text("该管理员不在线。", NamedTextColor.RED));
            return true;
        }

        AdminCommandVoteManager.VoteResult result;
        if (sender instanceof ConsoleCommandSender) {
            result = voteManager.recordConsoleVote(sender, admin.getUniqueId(), accepted);
        } else if (sender instanceof Player voter) {
            result = voteManager.recordVote(voter, admin.getUniqueId(), accepted);
        } else {
            sender.sendMessage(Component.text("请由玩家或控制台执行该指令。", NamedTextColor.RED));
            return true;
        }

        sendResultMessage(sender, result, accepted);
        return true;
    }

    private void sendResultMessage(CommandSender sender, AdminCommandVoteManager.VoteResult result, boolean accepted) {
        switch (result) {
            case RECORDED -> sender.sendMessage(
                    Component.text("已记录你的投票：", NamedTextColor.GREEN)
                            .append(Component.text(accepted ? "同意" : "拒绝", accepted ? NamedTextColor.GREEN : NamedTextColor.RED))
            );
            case PASSED -> sender.sendMessage(Component.text("指令投票已通过。", NamedTextColor.GREEN));
            case FAILED -> sender.sendMessage(Component.text("指令投票未通过。", NamedTextColor.YELLOW));
            case NOT_FOUND -> sender.sendMessage(Component.text("该管理员当前没有进行中的指令投票。", NamedTextColor.RED));
            case ALREADY_VOTED -> sender.sendMessage(Component.text("你已经投过票了。", NamedTextColor.RED));
            case SELF_VOTE -> sender.sendMessage(Component.text("不能给自己发起的投票投票。", NamedTextColor.RED));
            case CONSOLE_ONLY -> sender.sendMessage(Component.text("该投票只能由控制台处理。", NamedTextColor.RED));
            case CONSOLE_YES_ONLY -> sender.sendMessage(Component.text("控制台只能执行同意投票。", NamedTextColor.RED));
            case CONSOLE_NO_PLAYERS_ONLY -> sender.sendMessage(
                    Component.text("控制台仅能在没有其他玩家在线时批准投票。", NamedTextColor.RED)
            );
        }
    }

    private static Boolean parseVote(String input) {
        return switch (input.toLowerCase(Locale.ROOT)) {
            case "yes", "y", "同意", "accept" -> true;
            case "no", "n", "拒绝", "deny" -> false;
            default -> null;
        };
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> options = new ArrayList<>();
            for (String option : List.of("yes", "no")) {
                if (option.startsWith(prefix)) {
                    options.add(option);
                }
            }
            return options;
        }
        if (args.length == 2) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            List<String> names = new ArrayList<>();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                    names.add(online.getName());
                }
            }
            return names;
        }
        return List.of();
    }
}
