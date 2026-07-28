package com.servermenu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class MenuItems {

    private MenuItems() {
    }

    public static NamespacedKey key(JavaPlugin plugin) {
        return new NamespacedKey(plugin, "server_menu");
    }

    public static ItemStack createMenuItem(JavaPlugin plugin) {
        Material material = Material.matchMaterial(plugin.getConfig().getString("menu-item.material", "CLOCK"));
        if (material == null) {
            material = Material.CLOCK;
        }
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        String name = plugin.getConfig().getString("menu-item.name", "服务器菜单");
        meta.displayName(Component.text(name, NamedTextColor.AQUA, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(java.util.List.of(
                Component.text("右键打开服务器菜单", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(key(plugin), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isMenuItem(JavaPlugin plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        Byte marker = item.getItemMeta().getPersistentDataContainer().get(key(plugin), PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }
}
