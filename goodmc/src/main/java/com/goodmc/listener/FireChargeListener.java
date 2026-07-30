package com.goodmc.listener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public final class FireChargeListener implements Listener {

    private static final double FIREBALL_SPEED = 1.5;

    private final Map<UUID, Boolean> unlocked = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastLockMessage = new ConcurrentHashMap<>();

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

        UUID id = player.getUniqueId();

        if (!unlocked.containsKey(id)) {
            event.setCancelled(true);
            sendLockPrompt(player);
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

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        unlocked.remove(id);
        lastLockMessage.remove(id);
    }

    private void sendLockPrompt(Player player) {
        UUID id = player.getUniqueId();

        // Debounce: only send lock message once per 3 seconds
        Long lastMsg = lastLockMessage.get(id);
        if (lastMsg != null && System.currentTimeMillis() - lastMsg < 3000) {
            return;
        }
        lastLockMessage.put(id, System.currentTimeMillis());

        Component lockLabel = Component.text("🔒 火球丢出保护锁")
                .color(NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, true);

        Component unlockBtn = Component.text(" [点击解锁] ")
                .color(NamedTextColor.GREEN)
                .decoration(TextDecoration.BOLD, true)
                .clickEvent(ClickEvent.runCommand("/fireball_unlock"))
                .hoverEvent(HoverEvent.showText(Component.text("点击解锁以启用右键丢出火球")
                        .color(NamedTextColor.GRAY)));

        Component line1 = lockLabel.append(unlockBtn);

        player.sendMessage(line1);
    }

    public void unlock(Player player) {
        unlocked.put(player.getUniqueId(), true);
        player.sendMessage(Component.text("✅ 火球丢出保护已解锁，右键可丢出火球")
                .color(NamedTextColor.GREEN));
    }

    private static void consumeOne(Player player, EquipmentSlot hand, ItemStack item) {
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
            return;
        }
        player.getInventory().setItem(hand, null);
    }
}
