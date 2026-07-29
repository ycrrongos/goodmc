package com.goodmc.listener;

import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.inventory.GrindstoneInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

public final class GrindstoneExtractorListener implements Listener {

    private static final int RESULT_SLOT = 2;

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (!(event.getInventory() instanceof GrindstoneInventory)) {
            return;
        }
        if (GrindstoneSetup.isExtractor(event.getInventory().getHolder())) {
            event.titleOverride(Component.text("提取物品附魔"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrepareGrindstone(PrepareGrindstoneEvent event) {
        if (!GrindstoneSetup.isExtractor(event.getView().getTopInventory().getHolder())) {
            return;
        }

        ItemStack input = getSingleInputItem(event.getInventory());
        if (input == null) {
            return;
        }

        Map<Enchantment, Integer> enchants = getExtractableEnchants(input);
        if (enchants.isEmpty()) {
            return;
        }

        event.setResult(createEnchantedBook(enchants));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getInventory() instanceof GrindstoneInventory grindstone)) {
            return;
        }
        if (!GrindstoneSetup.isExtractor(event.getView().getTopInventory().getHolder())) {
            return;
        }
        if (event.getClickedInventory() == null
                || event.getClickedInventory().getType() != InventoryType.GRINDSTONE) {
            return;
        }
        if (event.getRawSlot() != RESULT_SLOT && event.getSlot() != RESULT_SLOT) {
            return;
        }

        ItemStack result = grindstone.getResult();
        if (result == null || result.getType() != Material.ENCHANTED_BOOK || result.getType().isAir()) {
            return;
        }

        ItemStack input = getSingleInputItem(grindstone);
        if (input == null) {
            return;
        }

        Map<Enchantment, Integer> enchants = getExtractableEnchants(input);
        if (enchants.isEmpty()) {
            return;
        }

        ClickType click = event.getClick();
        if (click == ClickType.NUMBER_KEY
                || click == ClickType.SWAP_OFFHAND
                || click == ClickType.DROP
                || click == ClickType.CONTROL_DROP
                || click == ClickType.CREATIVE) {
            event.setCancelled(true);
            return;
        }

        ItemStack book = createEnchantedBook(enchants);
        ItemStack stripped = stripEnchants(input.clone());

        if (event.isShiftClick()) {
            event.setCancelled(true);
            clearInputs(grindstone);
            grindstone.setResult(null);
            giveOrDrop(player, book);
            giveOrDrop(player, stripped);
            player.updateInventory();
            return;
        }

        ItemStack cursor = event.getCursor();
        if (cursor != null && !cursor.getType().isAir()) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        clearInputs(grindstone);
        grindstone.setResult(null);
        player.setItemOnCursor(book);
        giveOrDrop(player, stripped);
        player.updateInventory();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory() instanceof GrindstoneInventory)) {
            return;
        }
        if (!GrindstoneSetup.isExtractor(event.getView().getTopInventory().getHolder())) {
            return;
        }
        if (event.getRawSlots().contains(RESULT_SLOT)) {
            event.setCancelled(true);
        }
    }

    private static void clearInputs(GrindstoneInventory grindstone) {
        grindstone.setUpperItem(null);
        grindstone.setLowerItem(null);
    }

    private static void giveOrDrop(Player player, ItemStack item) {
        PlayerInventory inventory = player.getInventory();
        Map<Integer, ItemStack> overflow = inventory.addItem(item);
        for (ItemStack drop : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
    }

    private static ItemStack getSingleInputItem(GrindstoneInventory inventory) {
        ItemStack upper = inventory.getUpperItem();
        ItemStack lower = inventory.getLowerItem();
        boolean hasUpper = upper != null && !upper.getType().isAir();
        boolean hasLower = lower != null && !lower.getType().isAir();
        if (hasUpper && hasLower) {
            return null;
        }
        if (hasUpper) {
            return upper;
        }
        if (hasLower) {
            return lower;
        }
        return null;
    }

    private static Map<Enchantment, Integer> getExtractableEnchants(ItemStack item) {
        if (item.getType() == Material.ENCHANTED_BOOK || item.getType() == Material.BOOK) {
            return Map.of();
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.getEnchants().isEmpty()) {
            return Map.of();
        }
        return Map.copyOf(meta.getEnchants());
    }

    private static ItemStack createEnchantedBook(Map<Enchantment, Integer> enchants) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
            meta.addStoredEnchant(entry.getKey(), entry.getValue(), true);
        }
        book.setItemMeta(meta);
        return book;
    }

    private static ItemStack stripEnchants(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        for (Enchantment enchantment : Map.copyOf(meta.getEnchants()).keySet()) {
            meta.removeEnchant(enchantment);
        }
        item.setItemMeta(meta);
        return item;
    }
}
