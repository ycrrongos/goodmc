package com.goodmc.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class SeedCommand implements CommandExecutor {

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!sender.hasPermission("goodmc.seed")) {
            sender.sendMessage(Component.text("你没有权限查看种子。", NamedTextColor.RED));
            return true;
        }

        World world;
        if (sender instanceof Player player) {
            world = player.getWorld();
        } else {
            world = sender.getServer().getWorlds().getFirst();
        }

        if (world == null) {
            sender.sendMessage(Component.text("无法获取世界种子。", NamedTextColor.RED));
            return true;
        }

        sender.sendMessage(
                Component.text("世界 ", NamedTextColor.GRAY)
                        .append(Component.text(world.getName(), NamedTextColor.AQUA))
                        .append(Component.text(" 的种子：", NamedTextColor.GRAY))
                        .append(Component.text(String.valueOf(world.getSeed()), NamedTextColor.GOLD))
        );
        return true;
    }
}
