package com.adminvote;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class VoteSwitchGuiCommand implements CommandExecutor {

    private final AdminCommandVoteManager voteManager;
    private final VoteDialogManager dialogManager;
    private final VoteInventoryGui voteGui;
    private final boolean forceChest;

    public VoteSwitchGuiCommand(AdminCommandVoteManager voteManager, VoteDialogManager dialogManager, VoteInventoryGui voteGui) {
        this(voteManager, dialogManager, voteGui, false);
    }

    public VoteSwitchGuiCommand(AdminCommandVoteManager voteManager, VoteDialogManager dialogManager, VoteInventoryGui voteGui, boolean forceChest) {
        this.voteManager = voteManager;
        this.dialogManager = dialogManager;
        this.voteGui = voteGui;
        this.forceChest = forceChest;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("此命令仅限玩家使用。");
            return true;
        }

        // Find the active vote for this player
        for (AdminCommandVote vote : voteManager.getActiveVotes()) {
            if (vote.isEligible(player.getUniqueId())) {
                // Close dialog if open
                if (dialogManager != null) {
                    dialogManager.closeDialogFor(player);
                }
                // Open chest GUI
                if (voteGui != null) {
                    int required = voteManager.config().requiredYesVotes(vote.command(), vote.eligibleCount(), org.bukkit.Bukkit.getOnlinePlayers().size());
                    voteGui.showVoteGui(player, vote, required);
                    if (dialogManager != null) {
                        dialogManager.showVoteProgressBar(player, vote);
                    }
                }
                return true;
            }
        }

        player.sendMessage("当前没有进行中的投票。");
        return true;
    }
}
