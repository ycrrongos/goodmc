package com.mention;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AtMentionCommand implements CommandExecutor, TabCompleter {

    private static final Title.Times TITLE_TIMES = Title.Times.times(
            Duration.ofMillis(400),
            Duration.ofSeconds(4),
            Duration.ofMillis(400)
    );

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("该指令只能由玩家使用。", NamedTextColor.RED));
            return true;
        }

        if (!player.hasPermission("goodmc.at")) {
            player.sendMessage(Component.text("你没有权限使用该指令。", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("用法: /@ <玩家> <消息>", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(Component.text("玩家不在线或不存在。", NamedTextColor.RED));
            return true;
        }

        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        sendMention(player, target, message);
        return true;
    }

    public void sendMention(Player sender, Player target, String message) {
        if (!sender.hasPermission("goodmc.at")) {
            sender.sendMessage(Component.text("你没有权限使用该指令。", NamedTextColor.RED));
            return;
        }

        // 大字：消息内容；小字：谁 @ 了你
        Component title = Component.text(message, NamedTextColor.WHITE)
                .decorate(TextDecoration.BOLD);
        Component subtitle = Component.text(sender.getName(), NamedTextColor.GOLD)
                .append(Component.text(" @了你", NamedTextColor.GRAY));

        target.showTitle(Title.title(title, subtitle, TITLE_TIMES));

        // 像普通聊天一样让全服看到
        Component chat = Component.text("<", NamedTextColor.WHITE)
                .append(Component.text(sender.getName(), NamedTextColor.WHITE))
                .append(Component.text("> ", NamedTextColor.WHITE))
                .append(Component.text("@" + target.getName() + " ", NamedTextColor.AQUA))
                .append(Component.text(message, NamedTextColor.WHITE));
        Bukkit.getServer().sendMessage(chat);
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length != 1) {
            return List.of();
        }

        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> names = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (sender instanceof Player player && online.equals(player)) {
                continue;
            }
            if (online.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                names.add(online.getName());
            }
        }
        return names;
    }
}
