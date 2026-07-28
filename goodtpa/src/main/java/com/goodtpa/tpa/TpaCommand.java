package com.goodtpa.tpa;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
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

public final class TpaCommand implements CommandExecutor, TabCompleter {

    private final TpaActions tpaActions;
    private final Consumer<Player> openMenu;

    public TpaCommand(TpaActions tpaActions, Consumer<Player> openMenu) {
        this.tpaActions = tpaActions;
        this.openMenu = openMenu;
    }

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

        return switch (label.toLowerCase(Locale.ROOT)) {
            case "tpa" -> handleTpa(player, args, TpaType.TO_TARGET);
            case "tpahere" -> handleTpa(player, args, TpaType.TO_REQUESTER);
            case "tpaccept" -> handleAccept(player, args);
            case "tpadeny" -> handleDeny(player, args);
            case "tpaback" -> handleBack(player);
            default -> false;
        };
    }

    private boolean handleTpa(Player requester, String[] args, TpaType type) {
        if (args.length == 0 && type == TpaType.TO_TARGET && openMenu != null) {
            openMenu.accept(requester);
            return true;
        }

        if (args.length != 1) {
            requester.sendMessage(Component.text("用法: /" + commandName(type) + " <玩家名>", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            requester.sendMessage(Component.text("玩家不在线或不存在。", NamedTextColor.RED));
            return true;
        }

        tpaActions.requestTeleport(requester, target, type);
        return true;
    }

    private boolean handleAccept(Player accepter, String[] args) {
        if (args.length != 1) {
            accepter.sendMessage(Component.text("用法: /tpaccept <玩家名>", NamedTextColor.RED));
            return true;
        }

        Player requester = Bukkit.getPlayerExact(args[0]);
        if (requester == null) {
            accepter.sendMessage(Component.text("玩家不在线或不存在。", NamedTextColor.RED));
            return true;
        }

        tpaActions.acceptRequest(accepter, requester);
        return true;
    }

    private boolean handleDeny(Player denier, String[] args) {
        if (args.length != 1) {
            denier.sendMessage(Component.text("用法: /tpadeny <玩家名>", NamedTextColor.RED));
            return true;
        }

        Player requester = Bukkit.getPlayerExact(args[0]);
        if (requester == null) {
            denier.sendMessage(Component.text("玩家不在线或不存在。", NamedTextColor.RED));
            return true;
        }

        tpaActions.denyRequest(denier, requester);
        return true;
    }

    private boolean handleBack(Player player) {
        tpaActions.teleportBack(player);
        return true;
    }

    private static String commandName(TpaType type) {
        return type == TpaType.TO_TARGET ? "tpa" : "tpahere";
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
