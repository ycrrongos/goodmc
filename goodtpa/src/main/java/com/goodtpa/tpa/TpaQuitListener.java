package com.goodtpa.tpa;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class TpaQuitListener implements Listener {

    private final TpaManager tpaManager;

    public TpaQuitListener(TpaManager tpaManager) {
        this.tpaManager = tpaManager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        tpaManager.clearPlayer(event.getPlayer().getUniqueId());
    }
}
