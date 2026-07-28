package com.goodtpa.util;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class TeleportEffects {

    private static final int PARTICLE_DURATION_TICKS = 80;
    private static final int PARTICLE_INTERVAL_TICKS = 5;

    private final JavaPlugin plugin;

    public TeleportEffects(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void spawnTeleportParticles(Player player) {
        new BukkitRunnable() {
            private int elapsed = 0;

            @Override
            public void run() {
                if (!player.isOnline() || elapsed >= PARTICLE_DURATION_TICKS) {
                    cancel();
                    return;
                }

                Location center = player.getLocation().add(0, 1.0, 0);
                player.getWorld().spawnParticle(
                        Particle.ENCHANT,
                        center,
                        12,
                        0.6,
                        0.9,
                        0.6,
                        0.5
                );
                player.getWorld().spawnParticle(
                        Particle.PORTAL,
                        center,
                        20,
                        0.6,
                        0.9,
                        0.6,
                        0.3
                );
                elapsed += PARTICLE_INTERVAL_TICKS;
            }
        }.runTaskTimer(plugin, 0L, PARTICLE_INTERVAL_TICKS);
    }
}
