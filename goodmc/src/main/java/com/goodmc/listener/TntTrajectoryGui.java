package com.goodmc.listener;

import com.goodmc.util.DialogApiUtil;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
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
        if (DialogApiUtil.isAvailable()) {
            openTrajectoryDialog(player);
        } else {
            openTrajectoryChestGui(player);
        }
    }

    private void openTrajectoryDialog(Player player) {
        try {
            Dialog dialog = Dialog.create(builder -> builder.empty()
                    .base(io.papermc.paper.registry.data.dialog.DialogBase.builder(
                                    Component.text("TNT 抛物线选择", NamedTextColor.YELLOW))
                            .canCloseWithEscape(true)
                            .body(List.of(
                                    DialogBody.plainMessage(Component.text("选择 TNT 投掷的抛物线类型", NamedTextColor.GRAY))
                            ))
                            .build()
                    )
                    .type(DialogType.multiAction(List.of(
                            ActionButton.builder(Component.text("末影珍珠", TextColor.color(0x55FFFF)))
                                    .tooltip(Component.text("使用末影珍珠抛物线"))
                                    .action(io.papermc.paper.registry.data.dialog.action.DialogAction.staticAction(
                                            ClickEvent.runCommand("tnt_traj ENDER_PEARL")))
                                    .build(),
                            ActionButton.builder(Component.text("弓", TextColor.color(0xFFAA00)))
                                    .tooltip(Component.text("使用弓抛物线"))
                                    .action(io.papermc.paper.registry.data.dialog.action.DialogAction.staticAction(
                                            ClickEvent.runCommand("tnt_traj BOW")))
                                    .build(),
                            ActionButton.builder(Component.text("弩", TextColor.color(0xFFAA00)))
                                    .tooltip(Component.text("使用弩抛物线"))
                                    .action(io.papermc.paper.registry.data.dialog.action.DialogAction.staticAction(
                                            ClickEvent.runCommand("tnt_traj CROSSBOW")))
                                    .build(),
                            ActionButton.builder(Component.text("雪球", TextColor.color(0xFFFFFF)))
                                    .tooltip(Component.text("使用雪球抛物线"))
                                    .action(io.papermc.paper.registry.data.dialog.action.DialogAction.staticAction(
                                            ClickEvent.runCommand("tnt_traj SNOWBALL")))
                                    .build()
                    ), ActionButton.builder(Component.text("切换箱子 UI", TextColor.color(0x55FFFF)))
                            .tooltip(Component.text("点击切换到箱子 GUI 界面"))
                            .action(io.papermc.paper.registry.data.dialog.action.DialogAction.staticAction(
                                    ClickEvent.runCommand("tnt_gui")))
                            .build(), 5))
            );
            player.showDialog(dialog);
            openGuis.put(player.getUniqueId(), true);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to show trajectory dialog for " + player.getName() + ": " + e.getMessage());
            openTrajectoryChestGui(player);
        }
    }

    private void openTrajectoryChestGui(Player player) {
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

        // Switch to Dialog UI button
        inv.setItem(8, createSwitchButton());

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

    private ItemStack createSwitchButton() {
        ItemStack item = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("切换 Dialog UI", NamedTextColor.AQUA).decoration(TextDecoration.BOLD, true));
            meta.lore(java.util.List.of(
                    Component.text("点击切换到 Dialog 对话框界面", NamedTextColor.GRAY)
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
            // Switch to Dialog UI
            player.closeInventory();
            if (DialogApiUtil.isAvailable()) {
                openTrajectoryDialog(player);
            }
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
