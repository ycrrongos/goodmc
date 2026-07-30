package com.adminvote;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class AdminVotePlugin extends JavaPlugin {

    private AdminVoteConfig pluginConfig;
    private AdminCommandVoteManager voteManager;
    private FabricModBridge modBridge;

    @Override
    public void onEnable() {
        pluginConfig = new AdminVoteConfig(this);
        pluginConfig.load();
        saveResource("server-menu.yml", false);

        modBridge = new FabricModBridge(this);
        modBridge.register();

        voteManager = new AdminCommandVoteManager(this, pluginConfig, modBridge);
        modBridge.setVoteManager(voteManager);

        VoteDialogManager dialogManager = new VoteDialogManager(this, voteManager);
        voteManager.setDialogManager(dialogManager);

        VoteInventoryGui voteGui = new VoteInventoryGui(this, voteManager, dialogManager);
        getServer().getPluginManager().registerEvents(voteGui, this);
        voteManager.setVoteGui(voteGui);

        // Start BossBar progress bar ticker (updates every second)
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (AdminCommandVote vote : voteManager.getActiveVotes()) {
                for (java.util.UUID id : vote.eligibleVoters()) {
                    org.bukkit.entity.Player p = getServer().getPlayer(id);
                    if (p != null) {
                        long timeoutMs = pluginConfig.voteTimeoutSeconds() * 1000L;
                        dialogManager.updateVoteProgressBar(p, vote, timeoutMs);
                    }
                }
            }
        }, 20L, 20L);

        var pm = getServer().getPluginManager();
        pm.registerEvents(new AdminCommandVoteListener(voteManager), this);

        // player join/quit tracking
        pm.registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onJoin(org.bukkit.event.player.PlayerJoinEvent e) {
                voteManager.updateActivity(e.getPlayer().getUniqueId());
            }
            @org.bukkit.event.EventHandler
            public void onQuit(org.bukkit.event.player.PlayerQuitEvent e) {
                voteManager.onPlayerQuit(e.getPlayer().getUniqueId());
            }
            @org.bukkit.event.EventHandler
            public void onDialogClose(org.bukkit.event.inventory.InventoryCloseEvent e) {
                // Close dialog tracking when player closes inventory (also covers dialog close)
                if (e.getPlayer() instanceof Player p) {
                    voteManager.onPlayerQuit(p.getUniqueId());
                }
            }
        }, this);

        if (pluginConfig.adminvoteCommandEnabled()) {
            AdminCommandVoteCommand cmd = new AdminCommandVoteCommand(voteManager);
            registerCommand("adminvote", cmd, cmd);
        }
        if (pluginConfig.tphCommandEnabled()) {
            TphCommand tph = new TphCommand();
            registerCommand("tph", tph, tph);
        }
        if (pluginConfig.votecmdCommandEnabled()) {
            registerCommand("votecmd", new VoteCmdCommand(voteManager), null);
        }
        if (pluginConfig.voteCommandEnabled()) {
            registerCommand("vote", new VoteManageCommand(this, voteManager), null);
        }
        registerCommand("vote_switch_gui", new VoteSwitchGuiCommand(voteManager, dialogManager, voteGui), null);

        getLogger().info("AdminVote enabled (with Fabric mod bridge)");
    }

    @Override
    public void onDisable() {
        if (voteManager != null) {
            voteManager.shutdown();
            voteManager = null;
        }
        if (modBridge != null) {
            modBridge.unregister();
            modBridge = null;
        }
        pluginConfig = null;
    }

    public AdminVoteConfig pluginConfig() { return pluginConfig; }
    public AdminCommandVoteManager voteManager() { return voteManager; }
    public FabricModBridge modBridge() { return modBridge; }

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
