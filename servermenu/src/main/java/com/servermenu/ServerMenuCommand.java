package com.servermenu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class ServerMenuCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final MenuGuiService guiService;
    private final MenuButtonRegistry registry;

    public ServerMenuCommand(JavaPlugin plugin, MenuGuiService guiService, MenuButtonRegistry registry) {
        this.plugin = plugin;
        this.guiService = guiService;
        this.registry = registry;
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

        if (!player.hasPermission("goodmc.menu")) {
            player.sendMessage(Component.text("你没有权限使用服务器菜单。", NamedTextColor.RED));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("goodmc.menu.reload")) {
                player.sendMessage(Component.text("你没有权限重载菜单。", NamedTextColor.RED));
                return true;
            }
            plugin.reloadConfig();
            registry.reload();
            player.sendMessage(Component.text("服务器菜单已重载。", NamedTextColor.GREEN));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("open")) {
            guiService.openMain(player);
            return true;
        }

        player.getInventory().addItem(MenuItems.createMenuItem(plugin));
        player.sendMessage(Component.text("已领取服务器菜单物品，右键打开。", NamedTextColor.GREEN));
        return true;
    }
}
