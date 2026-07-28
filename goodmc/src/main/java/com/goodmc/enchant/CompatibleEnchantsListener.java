package com.goodmc.enchant;

import com.goodmc.GoodMCPluginConfig;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.view.AnvilView;

public final class CompatibleEnchantsListener implements Listener {

    private final GoodMCPluginConfig config;

    public CompatibleEnchantsListener(GoodMCPluginConfig config) {
        this.config = config;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!config.compatibleEnchantsEnabled()) {
            return;
        }

        AnvilInventory inventory = event.getInventory();
        ItemStack left = inventory.getItem(0);
        ItemStack right = inventory.getItem(1);
        if (left == null || left.getType().isAir() || right == null || right.getType().isAir()) {
            return;
        }

        String renameText = event.getView() instanceof AnvilView anvilView
                ? anvilView.getRenameText()
                : null;

        ItemStack merged = AnvilEnchantMerger.merge(left, right, renameText);
        if (merged == null) {
            return;
        }

        ItemStack vanilla = event.getResult();
        if (shouldReplaceResult(vanilla, merged)) {
            event.setResult(merged);
            int repairCost = AnvilEnchantMerger.calculateRepairCost(left, merged);
            if (event.getView() instanceof AnvilView anvilView) {
                anvilView.setRepairCost(repairCost);
            }
        }
    }

    private static boolean shouldReplaceResult(ItemStack vanilla, ItemStack merged) {
        if (vanilla == null || vanilla.getType().isAir()) {
            return true;
        }
        return AnvilEnchantMerger.enchantCount(merged) > AnvilEnchantMerger.enchantCount(vanilla);
    }
}
