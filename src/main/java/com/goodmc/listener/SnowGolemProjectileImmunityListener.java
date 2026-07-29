package com.goodmc.listener;

import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowman;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public final class SnowGolemProjectileImmunityListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamageByProjectile(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Snowman)) {
            return;
        }
        if (event.getDamager() instanceof Projectile) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onProjectileDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Snowman)) {
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {
            event.setCancelled(true);
        }
    }
}
