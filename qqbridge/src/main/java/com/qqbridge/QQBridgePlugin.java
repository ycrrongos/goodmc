package com.qqbridge;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

public final class QQBridgePlugin extends JavaPlugin {

    private QqBridgeConfig pluginConfig;
    private QqBridgeService qqBridgeService;

    @Override
    public void onEnable() {
        pluginConfig = new QqBridgeConfig(this);
        pluginConfig.load();
        saveResource("server-menu.yml", false);

        qqBridgeService = new QqBridgeService(this, pluginConfig);
        qqBridgeService.start();

        registerCommand("qq", pluginConfig.commandQqEnabled(), new QqCommand(pluginConfig, qqBridgeService), null);
        getServer().getPluginManager().registerEvents(new QqBridgeListener(pluginConfig, qqBridgeService), this);

        getLogger().info("QQBridge enabled");
    }

    @Override
    public void onDisable() {
        if (qqBridgeService != null) {
            qqBridgeService.stop();
            qqBridgeService = null;
        }
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
