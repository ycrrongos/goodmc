package com.goodtpa;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class GoodTpaConfig {

    private final JavaPlugin plugin;
    private int tpaDefaultTimeoutSeconds = 60;
    private int tpaMinTimeoutSeconds = 15;
    private int tpaMaxTimeoutSeconds = 300;
    private boolean cmdTpa = true;
    private boolean cmdTpahere = true;
    private boolean cmdTpaccept = true;
    private boolean cmdTpadeny = true;
    private boolean cmdTpaback = true;
    private boolean cmdTpatimeout = true;
    private boolean cmdTpamenu = true;
    private boolean cmdBack = true;
    private boolean cmdWaypoint = true;
    private boolean cmdWaypointbar = true;

    public GoodTpaConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        tpaDefaultTimeoutSeconds = Math.max(1, config.getInt("tpa.default-timeout-seconds", 60));
        tpaMinTimeoutSeconds = Math.max(1, config.getInt("tpa.min-timeout-seconds", 15));
        tpaMaxTimeoutSeconds = Math.max(tpaMinTimeoutSeconds, config.getInt("tpa.max-timeout-seconds", 300));

        cmdTpa = config.getBoolean("commands.tpa", true);
        cmdTpahere = config.getBoolean("commands.tpahere", true);
        cmdTpaccept = config.getBoolean("commands.tpaccept", true);
        cmdTpadeny = config.getBoolean("commands.tpadeny", true);
        cmdTpaback = config.getBoolean("commands.tpaback", true);
        cmdTpatimeout = config.getBoolean("commands.tpatimeout", true);
        cmdTpamenu = config.getBoolean("commands.tpamenu", true);
        cmdBack = config.getBoolean("commands.back", true);
        cmdWaypoint = config.getBoolean("commands.waypoint", true);
        cmdWaypointbar = config.getBoolean("commands.waypointbar", true);
    }

    public int tpaDefaultTimeoutSeconds() {
        return tpaDefaultTimeoutSeconds;
    }

    public int tpaMinTimeoutSeconds() {
        return tpaMinTimeoutSeconds;
    }

    public int tpaMaxTimeoutSeconds() {
        return tpaMaxTimeoutSeconds;
    }

    public boolean commandEnabled(String name) {
        return switch (name) {
            case "tpa" -> cmdTpa;
            case "tpahere" -> cmdTpahere;
            case "tpaccept" -> cmdTpaccept;
            case "tpadeny" -> cmdTpadeny;
            case "tpaback" -> cmdTpaback;
            case "tpatimeout" -> cmdTpatimeout;
            case "tpamenu" -> cmdTpamenu;
            case "back" -> cmdBack;
            case "waypoint" -> cmdWaypoint;
            case "waypointbar" -> cmdWaypointbar;
            default -> true;
        };
    }
}
