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
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TphCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!(sender instanceof Player executor)) {
            sender.sendMessage(Component.text("该指令只能由玩家使用。", NamedTextColor.RED));
            return true;
        }
        if (!executor.hasPermission("goodmc.tph")) {
            executor.sendMessage(Component.text("你没有权限使用 /tph。", NamedTextColor.RED));
            return true;
        }

        if (args.length == 1) {
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                executor.sendMessage(Component.text("玩家不在线或不存在。", NamedTextColor.RED));
                return true;
            }
            teleportSilently(executor, target.getLocation(), executor, target.getName());
            return true;
        }

        if (args.length == 2) {
            Player from = Bukkit.getPlayerExact(args[0]);
            Player to = Bukkit.getPlayerExact(args[1]);
            if (from == null || to == null) {
                executor.sendMessage(Component.text("玩家不在线或不存在。", NamedTextColor.RED));
                return true;
            }
            teleportSilently(from, to.getLocation(), executor, from.getName() + " -> " + to.getName());
            return true;
        }

        executor.sendMessage(Component.text("用法: /tph <玩家> 或 /tph <玩家A> <玩家B>", NamedTextColor.RED));
        return true;
    }

    private void teleportSilently(Player teleported, org.bukkit.Location destination, Player executor, String description) {
        teleported.teleportAsync(destination).thenAccept(success -> {
            if (success) {
                executor.sendMessage(
                        Component.text("已静默传送：", NamedTextColor.GREEN)
                                .append(Component.text(description, NamedTextColor.AQUA))
                );
            } else {
                executor.sendMessage(Component.text("传送失败，请稍后再试。", NamedTextColor.RED));
            }
        });
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!sender.hasPermission("goodmc.tph")) {
            return List.of();
        }

        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        List<String> names = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                names.add(online.getName());
            }
        }
        return names;
    }
}
