package com.goodtpa.deathback;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class DeathBackCommand implements CommandExecutor {

    private final DeathBackManager deathBackManager;

    public DeathBackCommand(DeathBackManager deathBackManager) {
        this.deathBackManager = deathBackManager;
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

        if (!player.hasPermission("goodmc.back")) {
            player.sendMessage(Component.text("你没有权限使用该指令。", NamedTextColor.RED));
            return true;
        }

        if (args.length != 0) {
            player.sendMessage(Component.text("用法: /back", NamedTextColor.RED));
            return true;
        }

        var deathLocation = deathBackManager.getDeathLocation(player.getUniqueId());
        if (deathLocation.isEmpty()) {
            player.sendMessage(Component.text("没有可返回的死亡点记录。", NamedTextColor.RED));
            return true;
        }

        deathBackManager.clearDeathLocation(player.getUniqueId());
        player.teleportAsync(deathLocation.get()).thenAccept(success -> {
            if (success) {
                player.sendMessage(Component.text("已返回死亡点。", NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("传送失败，请稍后再试。", NamedTextColor.RED));
            }
        });
        return true;
    }
}
