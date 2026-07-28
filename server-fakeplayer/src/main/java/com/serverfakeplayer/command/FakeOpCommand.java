package com.serverfakeplayer.command;

import com.serverfakeplayer.permission.FakePlayerPermissionStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * /fakeop &lt;player&gt; — grant fake-player access (like /op).
 * /fakedeop &lt;player&gt; — revoke.
 * /fakeop list — list allowlisted players.
 */
public final class FakeOpCommand implements CommandExecutor, TabCompleter {

    private final FakePlayerPermissionStore store;
    private final boolean grant;

    public FakeOpCommand(FakePlayerPermissionStore store, boolean grant) {
        this.store = store;
        this.grant = grant;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!sender.isOp() && !sender.hasPermission("serverfakeplayer.admin")) {
            sender.sendMessage(Component.text("没有权限。需要 OP 或 serverfakeplayer.admin。", NamedTextColor.RED));
            return true;
        }

        if (grant && args.length == 1 && args[0].equalsIgnoreCase("list")) {
            if (store.allowed().isEmpty()) {
                sender.sendMessage(Component.text("假人授权名单为空。（OP 默认可用）", NamedTextColor.YELLOW));
                return true;
            }
            sender.sendMessage(Component.text("假人授权名单:", NamedTextColor.GOLD));
            for (UUID uuid : store.allowed()) {
                sender.sendMessage(Component.text(" - " + store.displayName(uuid), NamedTextColor.GRAY));
            }
            return true;
        }

        if (args.length < 1) {
            if (grant) {
                sender.sendMessage(Component.text("用法: /fakeop <玩家> | /fakeop list", NamedTextColor.RED));
            } else {
                sender.sendMessage(Component.text("用法: /fakedeop <玩家>", NamedTextColor.RED));
            }
            return true;
        }

        OfflinePlayer target = FakePlayerPermissionStore.resolvePlayer(args[0]);
        String name = target.getName() != null ? target.getName() : args[0];

        if (grant) {
            if (store.grant(target)) {
                sender.sendMessage(Component.text("已授予 " + name + " 假人命令权限。", NamedTextColor.GREEN));
                if (target.isOnline() && target.getPlayer() != null) {
                    target.getPlayer().sendMessage(Component.text("你已获得假人命令权限（/player）。", NamedTextColor.GREEN));
                }
            } else {
                sender.sendMessage(Component.text(name + " 已在假人授权名单中。", NamedTextColor.YELLOW));
            }
        } else {
            if (store.revoke(target)) {
                sender.sendMessage(Component.text("已撤销 " + name + " 的假人命令权限。", NamedTextColor.GREEN));
                if (target.isOnline() && target.getPlayer() != null) {
                    target.getPlayer().sendMessage(Component.text("你的假人命令权限已被撤销。", NamedTextColor.RED));
                }
            } else {
                sender.sendMessage(Component.text(name + " 不在假人授权名单中。", NamedTextColor.YELLOW));
            }
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (!sender.isOp() && !sender.hasPermission("serverfakeplayer.admin")) {
            return List.of();
        }
        if (args.length != 1) {
            return List.of();
        }
        List<String> out = new ArrayList<>(store.tabCompleteNames(args[0]));
        if (grant) {
            String p = args[0].toLowerCase(Locale.ROOT);
            if ("list".startsWith(p)) {
                out.add("list");
            }
        }
        return out;
    }
}
