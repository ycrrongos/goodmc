package com.mention;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class MentionPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        saveResource("server-menu.yml", false);
        FileConfiguration config = getConfig();
        boolean atEnabled = config.getBoolean("commands.at", true);

        AtMentionCommand atMentionCommand = new AtMentionCommand();
        registerCommand("@", atEnabled, atMentionCommand, atMentionCommand);

        getLogger().info("Mention enabled");
    }

    private void registerCommand(String name, boolean enabled, CommandExecutor executor, TabCompleter tabCompleter) {
        if (!enabled) {
            getLogger().info("Command disabled by config: " + name);
            return;
        }
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Command not found in plugin.yml: " + name);
            return;
        }
        command.setExecutor(executor);
        if (tabCompleter != null) {
            command.setTabCompleter(tabCompleter);
        }
    }
}
