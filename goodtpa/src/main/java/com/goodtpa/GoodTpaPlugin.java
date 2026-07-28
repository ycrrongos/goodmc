package com.goodtpa;

import com.goodtpa.deathback.DeathBackCommand;
import com.goodtpa.deathback.DeathBackManager;
import com.goodtpa.deathback.DeathListener;
import com.goodtpa.menu.TpaMenuGuiService;
import com.goodtpa.menu.TpaMenuListener;
import com.goodtpa.tpa.TpaActions;
import com.goodtpa.tpa.TpaCommand;
import com.goodtpa.tpa.TpaManager;
import com.goodtpa.tpa.TpaQuitListener;
import com.goodtpa.tpa.TpaSettingsManager;
import com.goodtpa.tpa.TpaTimeoutCommand;
import com.goodtpa.util.TeleportEffects;
import com.goodtpa.waypoint.WaypointCommand;
import com.goodtpa.waypoint.WaypointGuiHolder;
import com.goodtpa.waypoint.WaypointGuiService;
import com.goodtpa.waypoint.WaypointListener;
import com.goodtpa.waypoint.WaypointManager;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class GoodTpaPlugin extends JavaPlugin {

    private TpaManager tpaManager;
    private TpaSettingsManager tpaSettingsManager;
    private DeathBackManager deathBackManager;
    private WaypointManager waypointManager;
    private GoodTpaConfig pluginConfig;

    @Override
    public void onEnable() {
        pluginConfig = new GoodTpaConfig(this);
        pluginConfig.load();
        saveResource("server-menu.yml", false);

        TeleportEffects teleportEffects = new TeleportEffects(this);
        tpaSettingsManager = new TpaSettingsManager(this, pluginConfig);
        tpaSettingsManager.load();
        tpaManager = new TpaManager(this, tpaSettingsManager, teleportEffects);
        TpaActions tpaActions = new TpaActions(tpaManager);
        deathBackManager = new DeathBackManager();
        waypointManager = new WaypointManager(this, teleportEffects);
        WaypointGuiService waypointGuiService = new WaypointGuiService(waypointManager);
        TpaMenuGuiService tpaMenuGuiService = new TpaMenuGuiService(tpaManager);
        TpaMenuListener tpaMenuListener = new TpaMenuListener(
                this,
                tpaMenuGuiService,
                tpaActions,
                tpaSettingsManager,
                deathBackManager
        );

        var pm = getServer().getPluginManager();
        pm.registerEvents(new TpaQuitListener(tpaManager), this);
        pm.registerEvents(tpaMenuListener, this);
        pm.registerEvents(new DeathListener(deathBackManager), this);
        pm.registerEvents(new WaypointListener(this, waypointManager, waypointGuiService), this);

        TpaCommand tpaCommand = new TpaCommand(tpaActions, tpaMenuListener::openMain);
        registerIfEnabled("tpa", tpaCommand, tpaCommand);
        registerIfEnabled("tpahere", tpaCommand, tpaCommand);
        registerIfEnabled("tpaccept", tpaCommand, tpaCommand);
        registerIfEnabled("tpadeny", tpaCommand, tpaCommand);
        registerIfEnabled("tpaback", tpaCommand, tpaCommand);
        registerIfEnabled("tpatimeout", new TpaTimeoutCommand(tpaSettingsManager), null);
        registerIfEnabled("tpamenu", (sender, command, label, args) -> {
            if (sender instanceof Player player) {
                tpaMenuListener.openMain(player);
            } else {
                sender.sendMessage("该指令只能由玩家使用。");
            }
            return true;
        }, null);
        registerIfEnabled("back", new DeathBackCommand(deathBackManager), null);

        WaypointCommand waypointCommand = new WaypointCommand(waypointGuiService, waypointManager);
        registerIfEnabled("waypoint", waypointCommand, null);
        registerIfEnabled("waypointbar", waypointCommand, null);

        getLogger().info("GoodTPA enabled");
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
        if (waypointManager != null) {
            for (Player player : getServer().getOnlinePlayers()) {
                if (player.getOpenInventory().getTopInventory().getHolder() instanceof WaypointGuiHolder) {
                    player.closeInventory();
                }
                waypointManager.clearSession(player.getUniqueId());
                waypointManager.clearPlayerState(player.getUniqueId());
            }
            waypointManager = null;
        }
        tpaManager = null;
        tpaSettingsManager = null;
        deathBackManager = null;
    }

    private void registerIfEnabled(String name, CommandExecutor executor, TabCompleter tabCompleter) {
        if (!pluginConfig.commandEnabled(name)) {
            getLogger().info("Command disabled: /" + name);
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
