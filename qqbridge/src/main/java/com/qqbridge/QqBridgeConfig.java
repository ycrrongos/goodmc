package com.qqbridge;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class QqBridgeConfig {

    private final JavaPlugin plugin;
    private boolean commandQqEnabled = true;
    private boolean qqBridgeEnabled = true;
    private String qqBridgeApiUrl = "http://127.0.0.1:8765";
    private String qqBridgeApiKey = "";
    private int qqBridgePollIntervalSeconds = 3;
    private int qqBridgePollLimit = 50;
    private boolean qqBridgeShowGroup = true;
    private String qqBridgeDefaultGroup = "RongMC&ROM交流群";
    private String qqBridgeReceiveGroupContains = "RongMC&ROM交流群";
    private boolean qqBridgeForwardJoinQuit = true;

    public QqBridgeConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        commandQqEnabled = config.getBoolean("commands.qq", true);
        qqBridgeEnabled = config.getBoolean("qq-bridge.enabled", true);
        qqBridgeApiUrl = config.getString("qq-bridge.api-url", "http://127.0.0.1:8765");
        qqBridgeApiKey = config.getString("qq-bridge.api-key", "");
        qqBridgePollIntervalSeconds = Math.max(1, config.getInt("qq-bridge.poll-interval-seconds", 3));
        qqBridgePollLimit = Math.max(1, Math.min(100, config.getInt("qq-bridge.poll-limit", 50)));
        qqBridgeShowGroup = config.getBoolean("qq-bridge.show-group", true);
        qqBridgeDefaultGroup = config.getString("qq-bridge.default-group", "RongMC&ROM交流群");
        qqBridgeReceiveGroupContains = config.getString("qq-bridge.receive-group-contains", "RongMC&ROM交流群");
        qqBridgeForwardJoinQuit = config.getBoolean("qq-bridge.forward-join-quit", true);
    }

    public boolean commandQqEnabled() {
        return commandQqEnabled;
    }

    public boolean qqBridgeEnabled() {
        return qqBridgeEnabled;
    }

    public String qqBridgeApiUrl() {
        return qqBridgeApiUrl;
    }

    public String qqBridgeApiKey() {
        return qqBridgeApiKey;
    }

    public int qqBridgePollIntervalSeconds() {
        return qqBridgePollIntervalSeconds;
    }

    public int qqBridgePollLimit() {
        return qqBridgePollLimit;
    }

    public boolean qqBridgeShowGroup() {
        return qqBridgeShowGroup;
    }

    public String qqBridgeDefaultGroup() {
        return qqBridgeDefaultGroup;
    }

    public String qqBridgeReceiveGroupContains() {
        return qqBridgeReceiveGroupContains;
    }

    public boolean qqBridgeForwardJoinQuit() {
        return qqBridgeForwardJoinQuit;
    }
}
