package com.goodmc.listener;

import org.bukkit.Material;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public final class FireChargeListener implements Listener {

    private static final double FIREBALL_SPEED = 1.5;

    @EventHandler(priority = EventPriority.NORMAL)
    public void onFireChargeUse(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        EquipmentSlot hand = event.getHand();
        if (hand == null) {
            return;
        }

        ItemStack item = player.getInventory().getItem(hand);
        if (item.getType() != Material.FIRE_CHARGE) {
            return;
        }

        event.setCancelled(true);

        var eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();

        LargeFireball fireball = player.getWorld().spawn(eye, LargeFireball.class, spawned -> {
            spawned.setShooter(player);
            spawned.setDirection(direction);
            spawned.setVelocity(direction.multiply(FIREBALL_SPEED));
            spawned.setYield(1.0f);
            spawned.setIsIncendiary(true);
        });

        fireball.setShooter(player);

        consumeOne(player, hand, item);
    }

    private static void consumeOne(Player player, EquipmentSlot hand, ItemStack item) {
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
            return;
        }
        player.getInventory().setItem(hand, null);
    }
}
