package com.adminvote;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Uses Paper's Dialog API (MC 1.21.6+) to show in-game vote GUIs.
 * No mods required — works on vanilla clients.
 * Also provides BossBar progress bars for vote timeouts.
 */
public final class VoteDialogManager {

    private final JavaPlugin plugin;
    private final AdminCommandVoteManager voteManager;
    private VoteInventoryGui voteGui;

    /** Tracks which players currently have a vote dialog open: player -> admin whose vote they're viewing */
    private final Map<UUID, UUID> openDialogs = new ConcurrentHashMap<>();

    /** BossBar progress bars for vote timeouts: player -> BossBar */
    private final Map<UUID, BossBar> voteProgressBars = new ConcurrentHashMap<>();

    /** Player UI preferences: true = Dialog, false = Chest */
    private final Map<UUID, Boolean> playerUiPreference = new ConcurrentHashMap<>();

    public VoteDialogManager(JavaPlugin plugin, AdminCommandVoteManager voteManager) {
        this.plugin = plugin;
        this.voteManager = voteManager;
    }

    public void setVoteGui(VoteInventoryGui voteGui) {
        this.voteGui = voteGui;
    }

    /**
     * Show vote UI to a player based on their preference.
     */
    public void showVoteUi(Player player, AdminCommandVote vote, int required) {
        Boolean preferDialog = playerUiPreference.get(player.getUniqueId());
        if (preferDialog == null || preferDialog) {
            // Default to Dialog UI
            if (!showVoteDialog(player, vote, required)) {
                // Fallback to chest GUI if dialog fails
                playerUiPreference.put(player.getUniqueId(), false);
                if (voteGui != null) {
                    voteGui.showVoteGui(player, vote, required);
                }
            }
        } else {
            // Use chest GUI
            if (voteGui != null) {
                voteGui.showVoteGui(player, vote, required);
            }
        }
    }

    /**
     * Set player's UI preference for vote UI.
     */
    public void setPlayerPreference(Player player, boolean preferDialog) {
        playerUiPreference.put(player.getUniqueId(), preferDialog);
    }

    /**
     * Show a vote dialog to a player.
     * @return true if dialog was shown successfully, false if fallback to chat buttons is needed
     */
    public boolean showVoteDialog(Player player, AdminCommandVote vote, int required) {
        try {
            String typeLabel = vote.type() == AdminCommandVote.VoteType.PLAYER_REQUEST ? "玩家请求" : "管理员";
            String rule = voteManager.config().approvalRuleDescription(
                    vote.command(), vote.eligibleCount(), Bukkit.getOnlinePlayers().size());
            int timeoutSec = (int) (vote.remainingMs() / 1000);

            String adminName = vote.adminName();

            // Buttons use commandTemplate to run vote commands directly
            Dialog dialog = Dialog.create(builder -> builder.empty()
                    .base(io.papermc.paper.registry.data.dialog.DialogBase.builder(
                                    Component.text("投票 - " + typeLabel, NamedTextColor.YELLOW))
                            .canCloseWithEscape(true)
                            .body(List.of(
                                    DialogBody.plainMessage(Component.text("发起者: ", NamedTextColor.GRAY)
                                            .append(Component.text(vote.adminName(), NamedTextColor.AQUA))),
                                    DialogBody.plainMessage(Component.text("指令: ", NamedTextColor.GRAY)
                                            .append(Component.text(vote.command(), NamedTextColor.WHITE))),
                                    DialogBody.plainMessage(Component.text("规则: ", NamedTextColor.GRAY)
                                            .append(Component.text(rule, NamedTextColor.GOLD))),
                                    DialogBody.plainMessage(Component.text("剩余时间: ", NamedTextColor.GRAY)
                                            .append(Component.text(timeoutSec + " 秒", NamedTextColor.GRAY)))
                            ))
                            .build()
                    )
                    .type(DialogType.multiAction(List.of(
                            ActionButton.builder(Component.text("同意", TextColor.color(0x55FF55)))
                                    .tooltip(Component.text("点击同意执行该指令"))
                                    .action(io.papermc.paper.registry.data.dialog.action.DialogAction.staticAction(
                                            net.kyori.adventure.text.event.ClickEvent.runCommand("/adminvote yes " + adminName)))
                                    .build(),
                            ActionButton.builder(Component.text("拒绝", TextColor.color(0xFF5555)))
                                    .tooltip(Component.text("点击拒绝该指令"))
                                    .action(io.papermc.paper.registry.data.dialog.action.DialogAction.staticAction(
                                            net.kyori.adventure.text.event.ClickEvent.runCommand("/adminvote no " + adminName)))
                                    .build(),
                            ActionButton.builder(Component.text("弃权", TextColor.color(0xAAAAAA)))
                                    .tooltip(Component.text("点击弃权，不计入投票"))
                                    .action(io.papermc.paper.registry.data.dialog.action.DialogAction.staticAction(
                                            net.kyori.adventure.text.event.ClickEvent.runCommand("/adminvote abstain " + adminName)))
                                    .build()
                    ), ActionButton.builder(Component.text("切换箱子 UI", TextColor.color(0x55FFFF)))
                            .tooltip(Component.text("点击切换到箱子 GUI 界面"))
                            .action(io.papermc.paper.registry.data.dialog.action.DialogAction.staticAction(
                                    net.kyori.adventure.text.event.ClickEvent.runCommand("/vote_switch_gui_chest")))
                            .build(), 5))  // 5 columns to make buttons smaller
            );

            player.showDialog(dialog);
            openDialogs.put(player.getUniqueId(), vote.adminId());
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to show vote dialog for " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Show a BossBar progress bar for vote timeout.
     */
    public void showVoteProgressBar(Player player, AdminCommandVote vote) {
        hideVoteProgressBar(player);

        long timeoutMs = voteManager.config().voteTimeoutSeconds() * 1000L;
        BossBar bar = BossBar.bossBar(
                Component.empty(),
                1.0f,
                BossBar.Color.GREEN,
                BossBar.Overlay.PROGRESS
        );

        voteProgressBars.put(player.getUniqueId(), bar);
        player.showBossBar(bar);
        updateVoteProgressBar(player, vote, timeoutMs);
    }

    /**
     * Update the BossBar progress bar for a player.
     */
    public void updateVoteProgressBar(Player player, AdminCommandVote vote, long timeoutMs) {
        BossBar bar = voteProgressBars.get(player.getUniqueId());
        if (bar == null) return;

        long remainingMs = vote.remainingMs();
        float progress = timeoutMs <= 0 ? 0.0f : Math.max(0.0f, (float) remainingMs / timeoutMs);
        int remainingSeconds = (int) Math.ceil(remainingMs / 1000.0);

        BossBar.Color color = progress > 0.5f
                ? BossBar.Color.GREEN
                : progress > 0.2f ? BossBar.Color.YELLOW : BossBar.Color.RED;

        String title = "投票剩余 " + remainingSeconds + " 秒 · " + vote.adminName() + ": " + vote.command();
        bar.name(Component.text(title, NamedTextColor.WHITE));
        bar.progress(progress);
        bar.color(color);
    }

    /**
     * Hide the BossBar progress bar for a player.
     */
    public void hideVoteProgressBar(Player player) {
        BossBar bar = voteProgressBars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    /**
     * Hide all vote progress bars for a vote.
     */
    public void hideAllVoteProgressBars(AdminCommandVote vote) {
        for (UUID id : vote.eligibleVoters()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                hideVoteProgressBar(p);
            }
        }
    }

    /**
     * Close the vote dialog for a player (e.g. when vote ends).
     */
    public void closeDialogFor(Player player) {
        if (openDialogs.containsKey(player.getUniqueId())) {
            player.closeDialog();
            openDialogs.remove(player.getUniqueId());
        }
        hideVoteProgressBar(player);
    }

    /**
     * Close dialogs for all eligible voters when a vote concludes.
     */
    public void closeAllForVote(AdminCommandVote vote) {
        for (UUID id : vote.eligibleVoters()) {
            if (openDialogs.containsKey(id)) {
                Player p = Bukkit.getPlayer(id);
                if (p != null) {
                    p.closeDialog();
                }
                openDialogs.remove(id);
            }
            hideVoteProgressBar(Bukkit.getPlayer(id));
        }
    }

    public void onPlayerQuit(UUID playerId) {
        openDialogs.remove(playerId);
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            hideVoteProgressBar(player);
        }
    }
}
