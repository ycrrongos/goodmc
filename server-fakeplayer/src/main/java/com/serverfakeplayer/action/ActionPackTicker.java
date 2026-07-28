package com.serverfakeplayer.action;

import com.serverfakeplayer.nms.FakePlayerManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;

public final class ActionPackTicker implements Listener {

    private final FakePlayerManager manager;

    public ActionPackTicker(FakePlayerManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.tickActions();
    }

    @EventHandler
    public void onDisable(PluginDisableEvent event) {
        // no-op; plugin onDisable handles cleanup
    }
}
