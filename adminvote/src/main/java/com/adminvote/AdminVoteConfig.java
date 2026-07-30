package com.adminvote;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class AdminVoteConfig {

    public enum FilterMode { WHITELIST, BLACKLIST }

    private final JavaPlugin plugin;

    // command toggles
    private boolean adminvoteCommandEnabled = true;
    private boolean tphCommandEnabled = true;
    private boolean votecmdCommandEnabled = true;
    private boolean voteCommandEnabled = true;

    // admin command filter
    private FilterMode adminFilterMode = FilterMode.BLACKLIST;
    private List<String> adminCommandList = new ArrayList<>();
    private boolean useHardcodedList = true;

    // player request
    private boolean playerRequestEnabled = true;
    private FilterMode playerRequestFilterMode = FilterMode.WHITELIST;
    private List<String> playerRequestCommandList = new ArrayList<>();

    // vote settings
    private int approvalPercent = 80;
    private int gameruleApprovalPercent = 100;
    private int gameruleSpecialMinOnline = 2;
    private int voteTimeoutSeconds = 60;

    // AFK
    private boolean afkDefaultAccept = true;
    private int afkThresholdSeconds = 30;

    // target selector
    private boolean allowTargetSelectors = false;

    public AdminVoteConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();

        adminvoteCommandEnabled = c.getBoolean("commands.adminvote", true);
        tphCommandEnabled = c.getBoolean("commands.tph", true);
        votecmdCommandEnabled = c.getBoolean("commands.votecmd", true);
        voteCommandEnabled = c.getBoolean("commands.vote", true);

        String mode = c.getString("admin-command-filter.mode", "BLACKLIST").toUpperCase(Locale.ROOT);
        adminFilterMode = "WHITELIST".equals(mode) ? FilterMode.WHITELIST : FilterMode.BLACKLIST;
        adminCommandList = c.getStringList("admin-command-filter.list");
        useHardcodedList = c.getBoolean("admin-command-filter.use-hardcoded-list", true);

        playerRequestEnabled = c.getBoolean("player-request.enabled", true);
        String prMode = c.getString("player-request.filter-mode", "WHITELIST").toUpperCase(Locale.ROOT);
        playerRequestFilterMode = "BLACKLIST".equals(prMode) ? FilterMode.BLACKLIST : FilterMode.WHITELIST;
        playerRequestCommandList = c.getStringList("player-request.command-list");

        approvalPercent = clamp(c.getInt("vote-settings.approval-percent", 80));
        gameruleApprovalPercent = clamp(c.getInt("vote-settings.gamerule-approval-percent", 100));
        gameruleSpecialMinOnline = Math.max(1, c.getInt("vote-settings.gamerule-special-min-online", 2));
        voteTimeoutSeconds = Math.max(10, c.getInt("vote-settings.timeout-seconds", 60));

        afkDefaultAccept = c.getBoolean("vote-settings.afk-default-accept", true);
        afkThresholdSeconds = Math.max(5, c.getInt("vote-settings.afk-threshold-seconds", 30));

        allowTargetSelectors = c.getBoolean("vote-settings.allow-target-selectors", false);
    }

    // --- command toggles ---
    public boolean adminvoteCommandEnabled() { return adminvoteCommandEnabled; }
    public boolean tphCommandEnabled() { return tphCommandEnabled; }
    public boolean votecmdCommandEnabled() { return votecmdCommandEnabled; }
    public boolean voteCommandEnabled() { return voteCommandEnabled; }

    // --- admin command filter ---
    public FilterMode adminFilterMode() { return adminFilterMode; }
    public List<String> adminCommandList() { return adminCommandList; }
    public boolean useHardcodedList() { return useHardcodedList; }

    public boolean requiresVoteForCommand(String commandLine) {
        String cmdName = VanillaAdminCommands.parseCommandName(commandLine);
        if (cmdName.isEmpty()) return false;

        if (useHardcodedList) {
            return VanillaAdminCommands.isVanillaAdminCommand(commandLine);
        }
        boolean inList = adminCommandList.stream()
                .anyMatch(c -> c.equalsIgnoreCase(cmdName));
        return adminFilterMode == FilterMode.WHITELIST ? inList : !inList;
    }

    // --- player request ---
    public boolean playerRequestEnabled() { return playerRequestEnabled; }

    public boolean isPlayerRequestAllowed(String commandLine) {
        if (!playerRequestEnabled) return false;
        String cmdName = VanillaAdminCommands.parseCommandName(commandLine);
        boolean inList = playerRequestCommandList.stream()
                .anyMatch(c -> c.equalsIgnoreCase(cmdName));
        return playerRequestFilterMode == FilterMode.WHITELIST ? inList : !inList;
    }

    // --- target selector ---
    public boolean allowTargetSelectors() { return allowTargetSelectors; }

    public boolean containsTargetSelectors(String commandLine) {
        return commandLine.contains("@a") || commandLine.contains("@e") || commandLine.contains("@r");
    }

    public boolean canEnterVoting(String commandLine) {
        return allowTargetSelectors || !containsTargetSelectors(commandLine);
    }

    // --- vote settings ---
    public int approvalPercent() { return approvalPercent; }
    public int gameruleApprovalPercent() { return gameruleApprovalPercent; }
    public int gameruleSpecialMinOnline() { return gameruleSpecialMinOnline; }
    public int voteTimeoutSeconds() { return voteTimeoutSeconds; }
    public boolean afkDefaultAccept() { return afkDefaultAccept; }
    public int afkThresholdSeconds() { return afkThresholdSeconds; }

    public int requiredYesVotes(String commandLine, int eligibleVoters, int totalOnlinePlayers) {
        if (eligibleVoters <= 0) return 0;
        int percent = approvalPercent(commandLine, totalOnlinePlayers);
        return Math.min(eligibleVoters, Math.max(1, (int) Math.ceil(eligibleVoters * (percent / 100.0))));
    }

    public int approvalPercent(String commandLine, int totalOnlinePlayers) {
        if (VanillaAdminCommands.isGameruleCommand(commandLine)
                && totalOnlinePlayers >= gameruleSpecialMinOnline) {
            return gameruleApprovalPercent;
        }
        return approvalPercent;
    }

    public String approvalRuleDescription(String commandLine, int eligibleVoters, int totalOnlinePlayers) {
        int percent = approvalPercent(commandLine, totalOnlinePlayers);
        int required = requiredYesVotes(commandLine, eligibleVoters, totalOnlinePlayers);
        if (percent >= 100) return "需要全体同意 " + required + " 人";
        return "需要 " + percent + "% 同意（" + required + "/" + eligibleVoters + " 人）";
    }

    private static int clamp(int value) { return Math.max(1, Math.min(100, value)); }
}
