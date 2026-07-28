package com.adminvote;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class AdminVoteConfig {

    private final JavaPlugin plugin;
    private boolean adminvoteCommandEnabled = true;
    private boolean tphCommandEnabled = true;
    private int approvalPercent = 80;
    private int gameruleApprovalPercent = 100;
    private int gameruleSpecialMinOnline = 2;

    public AdminVoteConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        adminvoteCommandEnabled = config.getBoolean("commands.adminvote", true);
        tphCommandEnabled = config.getBoolean("commands.tph", true);
        approvalPercent = clampPercent(config.getInt("admin-vote.approval-percent", 80));
        gameruleApprovalPercent = clampPercent(config.getInt("admin-vote.gamerule-approval-percent", 100));
        gameruleSpecialMinOnline = Math.max(1, config.getInt("admin-vote.gamerule-special-min-online", 2));
    }

    public boolean adminvoteCommandEnabled() {
        return adminvoteCommandEnabled;
    }

    public boolean tphCommandEnabled() {
        return tphCommandEnabled;
    }

    public int approvalPercent() {
        return approvalPercent;
    }

    public int gameruleApprovalPercent() {
        return gameruleApprovalPercent;
    }

    public int gameruleSpecialMinOnline() {
        return gameruleSpecialMinOnline;
    }

    public int requiredYesVotes(String commandLine, int eligibleVoters, int totalOnlinePlayers) {
        if (eligibleVoters <= 0) {
            return 0;
        }
        int percent = approvalPercent;
        if (VanillaAdminCommands.isGameruleCommand(commandLine)
                && totalOnlinePlayers >= gameruleSpecialMinOnline) {
            percent = gameruleApprovalPercent;
        }
        return Math.min(eligibleVoters, Math.max(1, (int) Math.ceil(eligibleVoters * (percent / 100.0))));
    }

    public String approvalRuleDescription(String commandLine, int eligibleVoters, int totalOnlinePlayers) {
        int percent = approvalPercent;
        if (VanillaAdminCommands.isGameruleCommand(commandLine)
                && totalOnlinePlayers >= gameruleSpecialMinOnline) {
            percent = gameruleApprovalPercent;
        }
        int required = requiredYesVotes(commandLine, eligibleVoters, totalOnlinePlayers);
        if (percent >= 100) {
            return "需要全体同意 " + required + " 人";
        }
        return "需要 " + percent + "% 同意（" + required + "/" + eligibleVoters + " 人）";
    }

    private static int clampPercent(int value) {
        return Math.max(1, Math.min(100, value));
    }
}
