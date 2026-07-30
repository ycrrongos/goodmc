package com.adminvote;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public final class AdminCommandVoteListener implements Listener {

    private final AdminCommandVoteManager voteManager;

    public AdminCommandVoteListener(AdminCommandVoteManager voteManager) {
        this.voteManager = voteManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!voteManager.shouldRequireVote(player)) return;
        if (voteManager.isExecutingApproved(player.getUniqueId())) return;

        String message = event.getMessage();
        if (!voteManager.config().requiresVoteForCommand(message)) return;
        if (VanillaAdminCommands.isSelfKillCommand(message, player)) return;

        // target selector check
        if (!voteManager.config().canEnterVoting(message)) {
            player.sendMessage(net.kyori.adventure.text.Component
                    .text("该指令包含目标选择器，已被禁止。", net.kyori.adventure.text.format.NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        voteManager.beginVote(player, message, AdminCommandVote.VoteType.ADMIN_COMMAND);
    }

    // --- AFK tracking ---
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        voteManager.updateActivity(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        voteManager.updateActivity(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(PlayerCommandPreprocessEvent event) {
        voteManager.updateActivity(event.getPlayer().getUniqueId());
    }
}
