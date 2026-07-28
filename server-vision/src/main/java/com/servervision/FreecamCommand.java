package com.servervision;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class FreecamCommand implements CommandExecutor {

    private final FreecamManager freecamManager;

    public FreecamCommand(FreecamManager freecamManager) {
        this.freecamManager = freecamManager;
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

        if (args.length != 0) {
            player.sendMessage(Component.text("用法: /freecam", NamedTextColor.RED));
            return true;
        }

        freecamManager.toggle(player);
        return true;
    }
}
