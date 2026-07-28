package com.goodtpa.deathback;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public final class DeathListener implements Listener {

    private final DeathBackManager deathBackManager;

    public DeathListener(DeathBackManager deathBackManager) {
        this.deathBackManager = deathBackManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        deathBackManager.setDeathLocation(player.getUniqueId(), player.getLocation());
    }
}
