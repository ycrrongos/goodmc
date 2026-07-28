package com.goodmc;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class GoodMCPluginConfig {

    private final JavaPlugin plugin;
    private String joinHintMessage = "输入 /sc 查看服务器命令与 GoodMC 功能说明。";
    private boolean compatibleEnchantsEnabled = true;
    private boolean cmdKill = true;
    private boolean cmdSeed = true;
    private boolean cmdSc = true;

    public GoodMCPluginConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        joinHintMessage = config.getString(
                "join.hint-message",
                "输入 /sc 查看服务器命令与 GoodMC 功能说明。"
        );
        compatibleEnchantsEnabled = config.getBoolean("compatible-enchants.enabled", true);
        cmdKill = config.getBoolean("commands.kill", true);
        cmdSeed = config.getBoolean("commands.seed", true);
        cmdSc = config.getBoolean("commands.sc", true);
    }

    public String joinHintMessage() {
        return joinHintMessage;
    }

    public boolean compatibleEnchantsEnabled() {
        return compatibleEnchantsEnabled;
    }

    public boolean commandEnabled(String name) {
        return switch (name) {
            case "kill" -> cmdKill;
            case "seed" -> cmdSeed;
            case "sc" -> cmdSc;
            default -> true;
        };
    }
}
