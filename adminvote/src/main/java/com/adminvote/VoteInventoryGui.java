package com.adminvote;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Chest GUI for vote dialog - smaller buttons than Dialog API.
 */
public final class VoteInventoryGui implements Listener {

    private final JavaPlugin plugin;
    private final AdminCommandVoteManager voteManager;
    private final VoteDialogManager dialogManager;

    /** Tracks open vote GUIs: player -> admin whose vote they're viewing */
    private final Map<UUID, UUID> openGuis = new HashMap<>();

    public VoteInventoryGui(JavaPlugin plugin, AdminCommandVoteManager voteManager, VoteDialogManager dialogManager) {
        this.plugin = plugin;
        this.voteManager = voteManager;
        this.dialogManager = dialogManager;
    }

    public void showVoteGui(Player player, AdminCommandVote vote, int required) {
        String typeLabel = vote.type() == AdminCommandVote.VoteType.PLAYER_REQUEST ? "玩家请求" : "管理员";
        String rule = voteManager.config().approvalRuleDescription(
                vote.command(), vote.eligibleCount(), Bukkit.getOnlinePlayers().size());
        int timeoutSec = (int) (vote.remainingMs() / 1000);

        Inventory inv = Bukkit.createInventory(null, 18, Component.text("投票 - " + typeLabel, NamedTextColor.YELLOW));

        // Accept button (lime wool)
        inv.setItem(0, createVoteButton(Material.LIME_WOOL, "同意", NamedTextColor.GREEN,
                "点击同意执行该指令", "/adminvote yes " + vote.adminName()));

        // Reject button (red wool)
        inv.setItem(1, createVoteButton(Material.RED_WOOL, "拒绝", NamedTextColor.RED,
                "点击拒绝该指令", "/adminvote no " + vote.adminName()));

        // Abstain button (gray wool)
        inv.setItem(2, createVoteButton(Material.GRAY_WOOL, "弃权", NamedTextColor.GRAY,
                "点击弃权，不计入投票", "/adminvote abstain " + vote.adminName()));

        // Switch to Dialog UI button
        inv.setItem(8, createSwitchButton());

        // Info items
        inv.setItem(9, createInfoItem(Component.text("发起者: ", NamedTextColor.GRAY)
                .append(Component.text(vote.adminName(), NamedTextColor.AQUA))));
        inv.setItem(10, createInfoItem(Component.text("指令: ", NamedTextColor.GRAY)
                .append(Component.text(vote.command(), NamedTextColor.WHITE))));
        inv.setItem(11, createInfoItem(Component.text("规则: ", NamedTextColor.GRAY)
                .append(Component.text(rule, NamedTextColor.GOLD))));
        inv.setItem(12, createInfoItem(Component.text("剩余: ", NamedTextColor.GRAY)
                .append(Component.text(timeoutSec + " 秒", NamedTextColor.GRAY))));
        inv.setItem(13, createInfoItem(Component.text("需要: ", NamedTextColor.GRAY)
                .append(Component.text(required + " 票", NamedTextColor.GOLD))));

        player.openInventory(inv);
        openGuis.put(player.getUniqueId(), vote.adminId());
    }

    private ItemStack createVoteButton(Material material, String name, NamedTextColor color,
                                        String hover, String command) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, color).decoration(TextDecoration.BOLD, true));
            meta.lore(java.util.List.of(
                    Component.text(hover, NamedTextColor.GRAY),
                    Component.text("点击执行: " + command, NamedTextColor.DARK_GRAY)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSwitchButton() {
        ItemStack item = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("切换Dialog UI", NamedTextColor.AQUA).decoration(TextDecoration.BOLD, true));
            meta.lore(java.util.List.of(
                    Component.text("点击切换到Dialog对话框界面", NamedTextColor.GRAY)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createInfoItem(Component text) {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(text);
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

        UUID adminId = openGuis.get(player.getUniqueId());
        VoteRecord.VoteChoice choice = switch (slot) {
            case 0 -> VoteRecord.VoteChoice.ACCEPT;
            case 1 -> VoteRecord.VoteChoice.REJECT;
            case 2 -> VoteRecord.VoteChoice.ABSTAIN;
            default -> null;
        };

        if (choice != null) {
            player.closeInventory();
            voteManager.recordVote(player, adminId, choice);
        } else if (slot == 8) {
            // Switch to Dialog UI
            player.closeInventory();
            AdminCommandVote vote = voteManager.getVote(adminId).orElse(null);
            if (vote != null && dialogManager != null) {
                dialogManager.showVoteDialog(player, vote, 0);
                dialogManager.showVoteProgressBar(player, vote);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        openGuis.remove(player.getUniqueId());
    }

    public void closeGuiFor(Player player) {
        if (openGuis.containsKey(player.getUniqueId())) {
            player.closeInventory();
        }
    }

    public void closeAllForVote(AdminCommandVote vote) {
        for (UUID id : vote.eligibleVoters()) {
            if (openGuis.containsKey(id)) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) {
                    p.closeInventory();
                }
            }
        }
    }

    public void onPlayerQuit(UUID playerId) {
        openGuis.remove(playerId);
    }
}
