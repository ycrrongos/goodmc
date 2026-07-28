package com.adminvote;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

public final class AdminVotePlugin extends JavaPlugin {

    private AdminVoteConfig pluginConfig;
    private AdminCommandVoteManager voteManager;

    @Override
    public void onEnable() {
        pluginConfig = new AdminVoteConfig(this);
        pluginConfig.load();
        saveResource("server-menu.yml", false);
        voteManager = new AdminCommandVoteManager(this, pluginConfig);

        getServer().getPluginManager().registerEvents(new AdminCommandVoteListener(voteManager), this);

        if (pluginConfig.adminvoteCommandEnabled()) {
            AdminCommandVoteCommand adminVoteCommand = new AdminCommandVoteCommand(voteManager);
            registerCommand("adminvote", adminVoteCommand, adminVoteCommand);
        }

        if (pluginConfig.tphCommandEnabled()) {
            TphCommand tphCommand = new TphCommand();
            registerCommand("tph", tphCommand, tphCommand);
        }

        getLogger().info("AdminVote enabled");
    }

    @Override
    public void onDisable() {
        if (voteManager != null) {
            voteManager.shutdown();
            voteManager = null;
        }
        pluginConfig = null;
    }

    public AdminVoteConfig pluginConfig() {
        return pluginConfig;
    }

    public AdminCommandVoteManager voteManager() {
        return voteManager;
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
