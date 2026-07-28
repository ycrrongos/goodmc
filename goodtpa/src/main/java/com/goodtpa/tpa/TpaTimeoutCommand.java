package com.goodtpa.tpa;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TpaTimeoutCommand implements CommandExecutor, TabCompleter {

    private final TpaSettingsManager settingsManager;

    public TpaTimeoutCommand(TpaSettingsManager settingsManager) {
        this.settingsManager = settingsManager;
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

        if (!player.hasPermission("goodmc.tpa")) {
            player.sendMessage(Component.text("你没有权限使用传送指令。", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            int seconds = settingsManager.getTimeoutSeconds(player.getUniqueId());
            player.sendMessage(Component.text(
                    "你的传送请求失效时间为 " + seconds + " 秒。",
                    NamedTextColor.AQUA
            ));
            player.sendMessage(Component.text(
                    "用法: /tpatimeout <"
                            + settingsManager.minTimeoutSeconds()
                            + "-"
                            + settingsManager.maxTimeoutSeconds()
                            + ">",
                    NamedTextColor.GRAY
            ));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(Component.text(
                    "用法: /tpatimeout <"
                            + settingsManager.minTimeoutSeconds()
                            + "-"
                            + settingsManager.maxTimeoutSeconds()
                            + ">",
                    NamedTextColor.RED
            ));
            return true;
        }

        int seconds;
        try {
            seconds = Integer.parseInt(args[0]);
        } catch (NumberFormatException exception) {
            player.sendMessage(Component.text("请输入有效的秒数。", NamedTextColor.RED));
            return true;
        }

        if (seconds < settingsManager.minTimeoutSeconds() || seconds > settingsManager.maxTimeoutSeconds()) {
            player.sendMessage(Component.text(
                    "失效时间必须在 "
                            + settingsManager.minTimeoutSeconds()
                            + " 到 "
                            + settingsManager.maxTimeoutSeconds()
                            + " 秒之间。",
                    NamedTextColor.RED
            ));
            return true;
        }

        int saved = settingsManager.setTimeoutSeconds(player, seconds);
        player.sendMessage(Component.text(
                "已设置你的传送请求失效时间为 " + saved + " 秒。",
                NamedTextColor.GREEN
        ));
        player.sendMessage(Component.text(
                "他人向你发起 /tpa 或 /tpahere 时，将按此时间显示失效进度条。",
                NamedTextColor.GRAY
        ));
        return true;
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

        List<String> suggestions = new ArrayList<>();
        int[] presets = {30, 60, 120, 180, 300};
        String prefix = args[0];
        for (int preset : presets) {
            if (preset < settingsManager.minTimeoutSeconds() || preset > settingsManager.maxTimeoutSeconds()) {
                continue;
            }
            String value = Integer.toString(preset);
            if (value.startsWith(prefix)) {
                suggestions.add(value);
            }
        }
        return suggestions;
    }
}
