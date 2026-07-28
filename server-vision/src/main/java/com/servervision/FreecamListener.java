package com.servervision;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class FreecamListener implements Listener {

    private final FreecamManager freecamManager;

    public FreecamListener(FreecamManager freecamManager) {
        this.freecamManager = freecamManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        if (event.getCause() != PlayerGameModeChangeEvent.Cause.GAMEMODE_SWITCHER) {
            return;
        }
        if (freecamManager.isApplyingChange(event.getPlayer().getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        freecamManager.toggle(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        if (freecamManager.isInFreecam(event.getPlayer().getUniqueId())) {
            freecamManager.exit(event.getPlayer());
        }
    }
}
