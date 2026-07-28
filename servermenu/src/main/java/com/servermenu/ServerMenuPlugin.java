package com.servermenu;

import org.bukkit.plugin.java.JavaPlugin;

public final class ServerMenuPlugin extends JavaPlugin {

    private MenuButtonRegistry registry;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        registry = new MenuButtonRegistry(this);
        // Delay discovery until other plugins finish enabling
        getServer().getScheduler().runTaskLater(this, () -> {
            registry.reload();
            getLogger().info("Discovered " + registry.buttons().size() + " menu button(s)");
        }, 2L);

        MenuGuiService guiService = new MenuGuiService(registry);
        getServer().getPluginManager().registerEvents(new MenuListener(this, guiService, registry), this);

        var command = getCommand("servermenu");
        if (command != null) {
            command.setExecutor(new ServerMenuCommand(this, guiService, registry));
        }

        getLogger().info("ServerMenu enabled");
    }

    public MenuButtonRegistry registry() {
        return registry;
    }
}
