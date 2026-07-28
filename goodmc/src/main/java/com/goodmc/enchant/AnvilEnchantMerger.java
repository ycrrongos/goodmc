package com.goodmc.enchant;

import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

final class AnvilEnchantMerger {

    private AnvilEnchantMerger() {
    }

    static ItemStack merge(ItemStack left, ItemStack right, String renameText) {
        if (left == null || left.getType().isAir()) {
            return null;
        }
        if (right == null || right.getType().isAir()) {
            return null;
        }

        ItemStack result;
        if (left.getType() == right.getType()) {
            result = left.clone();
            mergeDurability(result, left, right);
        } else if (right.getType() == Material.ENCHANTED_BOOK) {
            result = left.clone();
        } else if (left.getType() == Material.ENCHANTED_BOOK && right.getType() != Material.ENCHANTED_BOOK) {
            result = right.clone();
        } else {
            return null;
        }

        Map<Enchantment, Integer> enchants = new HashMap<>();
        collectEnchantsFromItem(enchants, left);
        collectEnchantsFromItem(enchants, right);
        applyEnchants(result, enchants, renameText);
        return result;
    }

    static int enchantCount(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return 0;
        }
        return collectEnchantsFromItem(new HashMap<>(), itemStack).size();
    }

    static int calculateRepairCost(ItemStack left, ItemStack merged) {
        Map<Enchantment, Integer> before = new HashMap<>();
        collectEnchantsFromItem(before, left);
        Map<Enchantment, Integer> after = new HashMap<>();
        collectEnchantsFromItem(after, merged);
        int cost = 1;
        for (Map.Entry<Enchantment, Integer> entry : after.entrySet()) {
            int previous = before.getOrDefault(entry.getKey(), 0);
            if (entry.getValue() > previous) {
                cost += entry.getValue() - previous;
            }
        }
        return Math.min(39, Math.max(1, cost));
    }

    private static Map<Enchantment, Integer> collectEnchantsFromItem(
            Map<Enchantment, Integer> target,
            ItemStack itemStack
    ) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return target;
        }
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return target;
        }

        if (meta instanceof EnchantmentStorageMeta bookMeta) {
            for (Map.Entry<Enchantment, Integer> entry : bookMeta.getStoredEnchants().entrySet()) {
                mergeLevel(target, entry.getKey(), entry.getValue());
            }
            return target;
        }

        for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
            mergeLevel(target, entry.getKey(), entry.getValue());
        }
        return target;
    }

    private static void mergeLevel(Map<Enchantment, Integer> target, Enchantment enchantment, int level) {
        int incoming = clampLevel(enchantment, level);
        Integer existing = target.get(enchantment);
        if (existing == null) {
            target.put(enchantment, incoming);
            return;
        }
        int combined = existing.equals(incoming)
                ? Math.min(enchantment.getMaxLevel(), existing + 1)
                : Math.max(existing, incoming);
        target.put(enchantment, clampLevel(enchantment, combined));
    }

    private static int clampLevel(Enchantment enchantment, int level) {
        return Math.max(1, Math.min(level, enchantment.getMaxLevel()));
    }

    private static void applyEnchants(ItemStack itemStack, Map<Enchantment, Integer> enchants, String renameText) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }

        if (renameText != null && !renameText.isBlank()) {
            meta.displayName(Component.text(renameText));
        }

        for (Enchantment enchantment : Map.copyOf(meta.getEnchants()).keySet()) {
            meta.removeEnchant(enchantment);
        }

        for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
            if (!entry.getKey().canEnchantItem(itemStack)) {
                continue;
            }
            int level = clampLevel(entry.getKey(), entry.getValue());
            meta.addEnchant(entry.getKey(), level, true);
        }

        itemStack.setItemMeta(meta);
    }

    private static void mergeDurability(ItemStack result, ItemStack left, ItemStack right) {
        ItemMeta leftMeta = left.getItemMeta();
        ItemMeta rightMeta = right.getItemMeta();
        ItemMeta resultMeta = result.getItemMeta();
        if (!(leftMeta instanceof Damageable leftDamage)
                || !(rightMeta instanceof Damageable rightDamage)
                || !(resultMeta instanceof Damageable resultDamage)) {
            return;
        }

        int max = result.getType().getMaxDurability();
        if (max <= 0) {
            return;
        }

        int leftRemaining = max - leftDamage.getDamage();
        int rightRemaining = max - rightDamage.getDamage();
        int combined = Math.min(max, leftRemaining + rightRemaining);
        resultDamage.setDamage(max - combined);
        result.setItemMeta(resultDamage);
    }
}
