package com.goodtpa.waypoint;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class WaypointCommand implements CommandExecutor {

    private final WaypointGuiService guiService;
    private final WaypointManager waypointManager;

    public WaypointCommand(WaypointGuiService guiService, WaypointManager waypointManager) {
        this.guiService = guiService;
        this.waypointManager = waypointManager;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (label.equalsIgnoreCase("waypointbar")) {
            return forwardVanillaWaypoint(sender, args);
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("该指令只能由玩家使用。", NamedTextColor.RED));
            return true;
        }

        if (!player.hasPermission("goodmc.waypoint")) {
            player.sendMessage(Component.text("你没有权限使用路径点功能。", NamedTextColor.RED));
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("back")) {
            waypointManager.teleportBack(player);
            return true;
        }

        guiService.open(player, WaypointTab.PUBLIC);
        return true;
    }

    private boolean forwardVanillaWaypoint(CommandSender sender, String[] args) {
        StringBuilder commandLine = new StringBuilder("minecraft:waypoint");
        for (String arg : args) {
            commandLine.append(' ').append(arg);
        }
        if (sender instanceof Player player) {
            player.performCommand(commandLine.toString());
        } else {
            sender.getServer().dispatchCommand(sender, commandLine.toString());
        }
        return true;
    }
}
