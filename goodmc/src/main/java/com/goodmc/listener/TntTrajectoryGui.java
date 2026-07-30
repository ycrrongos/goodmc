package com.goodmc.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

/**
 * Chest GUI for TNT trajectory selection.
 */
public final class TntTrajectoryGui implements Listener {

    private final Plugin plugin;
    private final TntThrowListener tntListener;
    private final Map<UUID, Boolean> openGuis = new HashMap<>();

    public TntTrajectoryGui(Plugin plugin, TntThrowListener tntListener) {
        this.plugin = plugin;
        this.tntListener = tntListener;
    }

    public void openTrajectoryGui(Player player) {
        Inventory inv = plugin.getServer().createInventory(null, 9,
                Component.text("TNT 抛物线选择", NamedTextColor.YELLOW));

        // Ender Pearl
        inv.setItem(0, createTrajectoryItem(Material.ENDER_PEARL, "末影珍珠", NamedTextColor.AQUA,
                TntThrowListener.Trajectory.ENDER_PEARL));

        // Bow
        inv.setItem(2, createTrajectoryItem(Material.BOW, "弓", NamedTextColor.GOLD,
                TntThrowListener.Trajectory.BOW));

        // Crossbow
        inv.setItem(4, createTrajectoryItem(Material.CROSSBOW, "弩", NamedTextColor.GOLD,
                TntThrowListener.Trajectory.CROSSBOW));

        // Snowball
        inv.setItem(6, createTrajectoryItem(Material.SNOWBALL, "雪球", NamedTextColor.WHITE,
                TntThrowListener.Trajectory.SNOWBALL));

        // Close button
        inv.setItem(8, createCloseButton());

        player.openInventory(inv);
        openGuis.put(player.getUniqueId(), true);
    }

    private ItemStack createTrajectoryItem(Material material, String name, NamedTextColor color,
                                            TntThrowListener.Trajectory trajectory) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, color).decoration(TextDecoration.BOLD, true));
            meta.lore(java.util.List.of(
                    Component.text("点击选择此抛物线", NamedTextColor.GRAY)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createCloseButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("关闭", NamedTextColor.RED).decoration(TextDecoration.BOLD, true));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!openGuis.containsKey(player.getUniqueId())) return;

        event.setCancelled(true);
        int slot = event.getSlot();

        TntThrowListener.Trajectory trajectory = switch (slot) {
            case 0 -> TntThrowListener.Trajectory.ENDER_PEARL;
            case 2 -> TntThrowListener.Trajectory.BOW;
            case 4 -> TntThrowListener.Trajectory.CROSSBOW;
            case 6 -> TntThrowListener.Trajectory.SNOWBALL;
            default -> null;
        };

        if (trajectory != null) {
            tntListener.setTrajectory(player, trajectory);
            player.closeInventory();
        } else if (slot == 8) {
            player.closeInventory();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        openGuis.remove(player.getUniqueId());
    }

    public void onPlayerQuit(UUID playerId) {
        openGuis.remove(playerId);
    }
}
