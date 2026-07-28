package com.serverheldlight;

import org.bukkit.plugin.java.JavaPlugin;

public final class ServerHeldLightPlugin extends JavaPlugin {

    private HeldLightManager heldLightManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        saveResource("server-menu.yml", false);

        boolean enabled = getConfig().getBoolean("enabled", true);
        if (!enabled) {
            getLogger().info("Server-HeldLight disabled by config.");
            return;
        }

        heldLightManager = new HeldLightManager(this);
        getServer().getPluginManager().registerEvents(heldLightManager, this);
        heldLightManager.start();

        getLogger().info("Server-HeldLight enabled");
    }

    @Override
    public void onDisable() {
        if (heldLightManager != null) {
            heldLightManager.stop();
            heldLightManager = null;
        }
    }
}
