package com.servervision;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

public final class ServerVisionPlugin extends JavaPlugin {

    private ServerVisionConfig pluginConfig;
    private FreecamManager freecamManager;

    @Override
    public void onEnable() {
        pluginConfig = new ServerVisionConfig(this);
        pluginConfig.load();
        saveResource("server-menu.yml", false);
        freecamManager = new FreecamManager();

        var pm = getServer().getPluginManager();

        if (pluginConfig.freecamCommandEnabled()) {
            pm.registerEvents(new FreecamListener(freecamManager), this);
            pm.registerEvents(
                    new FreecamCommandBlockListener(freecamManager, pluginConfig.blockedCommands()),
                    this
            );
            registerCommand("freecam", new FreecamCommand(freecamManager), null);
        }

        if (pluginConfig.fullbrightCommandEnabled()) {
            FullBrightCommand fullBrightCommand = new FullBrightCommand();
            registerCommand("fullbright", fullBrightCommand, null);
            pm.registerEvents(fullBrightCommand, this);
        }

        getLogger().info("Server-Vision enabled");
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
        if (freecamManager != null) {
            freecamManager.restoreAllOnlinePlayers(getServer().getOnlinePlayers());
            freecamManager = null;
        }
        pluginConfig = null;
    }

    public ServerVisionConfig pluginConfig() {
        return pluginConfig;
    }

    public FreecamManager freecamManager() {
        return freecamManager;
    }

    private void registerCommand(String name, CommandExecutor executor, TabCompleter tabCompleter) {
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
