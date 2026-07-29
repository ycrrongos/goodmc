package com.goodmc.listener;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Snowman;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.projectiles.ProjectileSource;

public final class SnowballListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSnowballHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball snowball)) {
            return;
        }
        if (!(event.getHitEntity() instanceof LivingEntity victim)) {
            return;
        }
        if (victim instanceof Snowman) {
            return;
        }

        ProjectileSource shooter = snowball.getShooter();
        LivingEntity damager = shooter instanceof LivingEntity living ? living : null;
        victim.damage(1.0, damager != null ? damager : snowball);
    }
}
