package com.goodmc.listener;

import net.kyori.adventure.text.Component;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.inventory.GrindstoneInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

public final class GrindstoneCurseListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (!(event.getInventory() instanceof GrindstoneInventory)) {
            return;
        }
        if (GrindstoneSetup.isCurseRemover(event.getInventory().getHolder())) {
            event.titleOverride(Component.text("去除物品负魔"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrepareGrindstone(PrepareGrindstoneEvent event) {
        if (!GrindstoneSetup.isCurseRemover(event.getView().getTopInventory().getHolder())) {
            return;
        }

        ItemStack input = getInputItem(event.getInventory());
        if (input == null) {
            return;
        }

        if (!hasAnyEnchant(input)) {
            return;
        }

        if (!hasCurse(input)) {
            event.setResult(input.clone());
            return;
        }

        ItemStack result = input.clone();
        removeCursesOnly(result);
        event.setResult(result);
    }

    private static ItemStack getInputItem(GrindstoneInventory inventory) {
        ItemStack upper = inventory.getUpperItem();
        if (upper != null && !upper.getType().isAir()) {
            return upper;
        }
        ItemStack lower = inventory.getLowerItem();
        if (lower != null && !lower.getType().isAir()) {
            return lower;
        }
        return null;
    }

    private static boolean hasAnyEnchant(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof EnchantmentStorageMeta bookMeta) {
            return !bookMeta.getStoredEnchants().isEmpty();
        }
        return meta != null && !meta.getEnchants().isEmpty();
    }

    private static boolean hasCurse(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof EnchantmentStorageMeta bookMeta) {
            return bookMeta.hasStoredEnchant(Enchantment.BINDING_CURSE)
                    || bookMeta.hasStoredEnchant(Enchantment.VANISHING_CURSE);
        }
        return meta != null && (meta.hasEnchant(Enchantment.BINDING_CURSE) || meta.hasEnchant(Enchantment.VANISHING_CURSE));
    }

    private static void removeCursesOnly(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof EnchantmentStorageMeta bookMeta) {
            bookMeta.removeStoredEnchant(Enchantment.BINDING_CURSE);
            bookMeta.removeStoredEnchant(Enchantment.VANISHING_CURSE);
            item.setItemMeta(bookMeta);
            return;
        }
        if (meta != null) {
            meta.removeEnchant(Enchantment.BINDING_CURSE);
            meta.removeEnchant(Enchantment.VANISHING_CURSE);
            item.setItemMeta(meta);
        }
    }
}
