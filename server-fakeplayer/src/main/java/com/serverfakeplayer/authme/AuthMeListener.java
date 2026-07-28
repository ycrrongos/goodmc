package com.serverfakeplayer.authme;

import com.serverfakeplayer.nms.FakePlayerManager;
import com.serverfakeplayer.nms.FakeServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class AuthMeListener implements Listener {

    private final JavaPlugin plugin;
    private final AuthMeHook hook;
    private final FakePlayerManager manager;

    public AuthMeListener(JavaPlugin plugin, AuthMeHook hook, FakePlayerManager manager) {
        this.plugin = plugin;
        this.hook = hook;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("authme.auto-login", true)) {
            return;
        }
        Player player = event.getPlayer();
        FakeServerPlayer fake = manager.get(player.getName());
        if (fake == null) {
            return;
        }
        // AuthMe often processes join after other plugins; delay a tick.
        Bukkit.getScheduler().runTask(plugin, () -> hook.ensureLoggedIn(player));
        Bukkit.getScheduler().runTaskLater(plugin, () -> hook.ensureLoggedIn(player), 5L);
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if (event.getPlugin().getName().equalsIgnoreCase("AuthMe")) {
            hook.tryHook();
        }
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin().getName().equalsIgnoreCase("AuthMe")) {
            hook.unhook();
        }
    }
}
