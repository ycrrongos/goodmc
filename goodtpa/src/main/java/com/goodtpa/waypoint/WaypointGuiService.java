package com.goodtpa.waypoint;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class WaypointGuiService {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private static final Material[] ICON_OPTIONS = {
            Material.LODESTONE,
            Material.COMPASS,
            Material.RECOVERY_COMPASS,
            Material.ENDER_PEARL,
            Material.BEACON,
            Material.NETHER_STAR,
            Material.EMERALD,
            Material.DIAMOND,
            Material.GRASS_BLOCK,
            Material.MAP
    };

    private final WaypointManager waypointManager;

    public WaypointGuiService(WaypointManager waypointManager) {
        this.waypointManager = waypointManager;
    }

    public void open(Player player, WaypointTab tab) {
        int page = waypointManager.getOrCreateState(player.getUniqueId()).page(tab);
        open(player, tab, page);
    }

    public void open(Player player, WaypointTab tab, int page) {
        WaypointPlayerState state = waypointManager.getOrCreateState(player.getUniqueId());
        state.setPage(tab, page);

        WaypointGuiHolder holder = new WaypointGuiHolder(tab, page);
        Component title = state.deleteMode()
                ? Component.text("路径点 - 删除模式", NamedTextColor.RED)
                : Component.text("路径点", NamedTextColor.DARK_AQUA);
        Inventory inventory = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inventory);

        fillBackground(inventory);
        renderTabs(inventory, tab, state);
        renderBackButton(inventory);

        switch (tab) {
            case PUBLIC -> renderWaypointList(player, holder, inventory, WaypointTab.PUBLIC, page);
            case PRIVATE -> renderWaypointList(player, holder, inventory, WaypointTab.PRIVATE, page);
            case CREATE -> renderCreatePage(player, inventory);
        }

        player.openInventory(inventory);
    }

    private void renderBackButton(Inventory inventory) {
        inventory.setItem(
                WaypointGuiHolder.BACK_BUTTON,
                createGlowingItem(
                        Material.ENDER_PEARL,
                        "§e返回传送前位置",
                        "§7返回上次路径点传送前的位置",
                        "§7等同 /waypoint back"
                )
        );
    }

    private void renderWaypointList(Player player, WaypointGuiHolder holder, Inventory inventory, WaypointTab tab, int page) {
        List<Waypoint> waypoints = new ArrayList<>(waypointManager.storage().getAccessibleWaypoints(player.getUniqueId(), tab));
        waypoints.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));

        int totalPages = Math.max(1, (int) Math.ceil(waypoints.size() / (double) WaypointGuiHolder.PAGE_SIZE));
        int safePage = Math.min(page, totalPages - 1);
        if (safePage != page) {
            waypointManager.getOrCreateState(player.getUniqueId()).setPage(tab, safePage);
        }

        int start = safePage * WaypointGuiHolder.PAGE_SIZE;
        int end = Math.min(start + WaypointGuiHolder.PAGE_SIZE, waypoints.size());

        WaypointPlayerState state = waypointManager.getOrCreateState(player.getUniqueId());
        boolean deleteMode = state.deleteMode();

        for (int i = start; i < end; i++) {
            Waypoint waypoint = waypoints.get(i);
            int slot = WaypointGuiHolder.WAYPOINT_SLOTS[i - start];
            inventory.setItem(slot, createWaypointItem(waypoint, deleteMode, waypointManager.creatorName(waypoint)));
            holder.mapSlot(slot, waypoint.id());
        }

        if (safePage > 0) {
            inventory.setItem(
                    WaypointGuiHolder.PAGE_PREVIOUS,
                    createActionItem(Material.SPECTRAL_ARROW, "§e上一页", "§7第 " + safePage + " 页")
            );
        }
        if (end < waypoints.size()) {
            inventory.setItem(
                    WaypointGuiHolder.PAGE_NEXT,
                    createActionItem(Material.SPECTRAL_ARROW, "§e下一页", "§7第 " + (safePage + 2) + " 页")
            );
        }
    }

    private void renderCreatePage(Player player, Inventory inventory) {
        WaypointCreateSession session = waypointManager.getOrCreateSession(player);
        session.setLocation(player.getLocation());

        inventory.setItem(
                WaypointGuiHolder.CREATE_NAME,
                createActionItem(
                        Material.NAME_TAG,
                        "§e设置名称",
                        session.name() == null ? "§7点击后在聊天栏输入路径点名称" : "§a当前名称: §f" + session.name()
                )
        );
        inventory.setItem(
                WaypointGuiHolder.CREATE_ICON,
                createGlowingItem(
                        session.icon(),
                        "§e选择图标",
                        "§7点击切换图标",
                        "§7当前: §f" + formatMaterial(session.icon()),
                        "§7也可在切换时读取主手物品"
                )
        );
        inventory.setItem(
                WaypointGuiHolder.CREATE_VISIBILITY,
                createActionItem(
                        session.isPublic() ? Material.LIME_DYE : Material.GRAY_DYE,
                        session.isPublic() ? "§a公开路径点" : "§7私有路径点",
                        "§7点击切换公开/私有"
                )
        );
        inventory.setItem(
                WaypointGuiHolder.CREATE_CONFIRM,
                createActionItem(
                        Material.EMERALD,
                        "§a确认创建",
                        session.isReady() ? "§7点击保存当前路径点" : "§c请先设置名称"
                )
        );
        inventory.setItem(4, createActionItem(
                Material.PAPER,
                "§b创建路径点",
                "§7已记录当前坐标",
                "§7世界: §f" + player.getWorld().getName(),
                "§7X: §f" + formatCoord(player.getLocation().getX()),
                "§7Y: §f" + formatCoord(player.getLocation().getY()),
                "§7Z: §f" + formatCoord(player.getLocation().getZ())
        ));
    }

    private void renderTabs(Inventory inventory, WaypointTab activeTab, WaypointPlayerState state) {
        inventory.setItem(
                WaypointGuiHolder.TAB_PUBLIC,
                createTabItem(Material.COMPASS, "§a公开路径点", activeTab == WaypointTab.PUBLIC)
        );
        inventory.setItem(
                WaypointGuiHolder.TAB_CREATE,
                createTabItem(Material.WRITABLE_BOOK, "§e创建路径点", activeTab == WaypointTab.CREATE)
        );
        inventory.setItem(
                WaypointGuiHolder.TAB_DELETE_MODE,
                createActionItem(
                        state.deleteMode() ? Material.BARRIER : Material.IRON_AXE,
                        state.deleteMode() ? "§c删除模式：开" : "§c删除模式：关",
                        "§7点击切换删除/传送模式",
                        "§7按 ESC 关闭界面可退出删除模式"
                )
        );
        inventory.setItem(
                WaypointGuiHolder.TAB_PRIVATE,
                createTabItem(Material.ENDER_EYE, "§d私有路径点", activeTab == WaypointTab.PRIVATE)
        );
    }

    private static void fillBackground(Inventory inventory) {
        ItemStack filler = createFiller();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
    }

    private static ItemStack createFiller() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(" "));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createTabItem(Material material, String name, boolean active) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LEGACY.deserialize(name).decoration(TextDecoration.ITALIC, false));
        if (active) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createWaypointItem(Waypoint waypoint, boolean deleteMode, String creatorName) {
        ItemStack item = new ItemStack(waypoint.icon());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(waypoint.name(), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("创建者: " + creatorName, NamedTextColor.AQUA));
        lore.add(Component.text("世界: " + waypoint.worldName(), NamedTextColor.GRAY));
        lore.add(Component.text(
                "X: " + formatCoord(waypoint.x()) + " Y: " + formatCoord(waypoint.y()) + " Z: " + formatCoord(waypoint.z()),
                NamedTextColor.GRAY
        ));
        lore.add(Component.text(deleteMode ? "点击删除" : "点击传送", deleteMode ? NamedTextColor.RED : NamedTextColor.GREEN));
        meta.lore(lore);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createGlowingItem(Material material, String name, String... loreLines) {
        ItemStack item = createActionItem(material, name, loreLines);
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createActionItem(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LEGACY.deserialize(name).decoration(TextDecoration.ITALIC, false));
        if (material == Material.LODESTONE || material == Material.COMPASS || material == Material.RECOVERY_COMPASS) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        }
        if (loreLines.length > 0) {
            List<Component> lore = new ArrayList<>();
            for (String line : loreLines) {
                lore.add(LEGACY.deserialize(line).decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    public Material nextIcon(Material current) {
        if (current == null) {
            return ICON_OPTIONS[0];
        }
        for (int i = 0; i < ICON_OPTIONS.length; i++) {
            if (ICON_OPTIONS[i] == current) {
                return ICON_OPTIONS[(i + 1) % ICON_OPTIONS.length];
            }
        }
        return ICON_OPTIONS[0];
    }

    private static String formatCoord(double value) {
        return String.format("%.1f", value);
    }

    private static String formatMaterial(Material material) {
        return material.name();
    }
}
