package com.goodmc.listener;

import org.bukkit.Material;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public final class TntThrowListener implements Listener {

    private static final int FUSE_TICKS = 120; // 6 seconds

    @EventHandler(priority = EventPriority.NORMAL)
    public void onTntThrow(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_AIR) {
            return;
        }

        Player player = event.getPlayer();
        EquipmentSlot hand = event.getHand();
        if (hand == null) {
            return;
        }

        ItemStack item = player.getInventory().getItem(hand);
        if (item.getType() != Material.TNT) {
            return;
        }

        event.setCancelled(true);

        Vector throwVelocity = sampleEnderPearlVelocity(player);
        var eye = player.getEyeLocation();

        player.getWorld().spawn(eye, TNTPrimed.class, tnt -> {
            tnt.setFuseTicks(FUSE_TICKS);
            tnt.setSource(player);
            tnt.setVelocity(throwVelocity);
        });

        consumeOne(player, hand, item);
    }

    private static Vector sampleEnderPearlVelocity(Player player) {
        EnderPearl pearl = player.launchProjectile(EnderPearl.class);
        Vector velocity = pearl.getVelocity().clone();
        pearl.remove();
        return velocity;
    }

    private static void consumeOne(Player player, EquipmentSlot hand, ItemStack item) {
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
            return;
        }
        player.getInventory().setItem(hand, null);
    }
}
