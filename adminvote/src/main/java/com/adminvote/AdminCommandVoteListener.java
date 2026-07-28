package com.adminvote;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public final class AdminCommandVoteListener implements Listener {

    private final AdminCommandVoteManager voteManager;

    public AdminCommandVoteListener(AdminCommandVoteManager voteManager) {
        this.voteManager = voteManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!voteManager.shouldRequireVote(player)) {
            return;
        }
        if (voteManager.isExecutingApproved(player.getUniqueId())) {
            return;
        }

        String message = event.getMessage();
        if (!VanillaAdminCommands.isVanillaAdminCommand(message)) {
            return;
        }
        if (VanillaAdminCommands.isSelfKillCommand(message, player)) {
            return;
        }

        event.setCancelled(true);
        voteManager.beginVote(player, message);
    }
}
