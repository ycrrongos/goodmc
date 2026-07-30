package com.goodvote.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Main configuration for the GoodVote mod.
 */
public class VoteConfig {
    // --- Admin command voting ---
    private FilterMode adminCommandFilterMode = FilterMode.BLACKLIST;
    private List<String> adminCommandList = new ArrayList<>();
    private boolean adminCommandRequiresVote = true;

    // --- Player request voting ---
    private FilterMode playerRequestFilterMode = FilterMode.WHITELIST;
    private List<String> playerRequestCommandList = new ArrayList<>();
    private boolean playerRequestEnabled = true;

    // --- Config change voting ---
    private boolean configChangeRequiresVote = true;

    // --- Vote settings ---
    private int voteTimeoutSeconds = 60;
    private int approvalPercent = 80;
    private boolean afkDefaultAccept = true;
    private int afkThresholdSeconds = 30;

    // --- Target selector control ---
    private boolean allowTargetSelectors = false;

    // --- Getters and Setters ---

    public FilterMode getAdminCommandFilterMode() { return adminCommandFilterMode; }
    public void setAdminCommandFilterMode(FilterMode mode) { this.adminCommandFilterMode = mode; }

    public List<String> getAdminCommandList() { return adminCommandList; }
    public void setAdminCommandList(List<String> list) { this.adminCommandList = new ArrayList<>(list); }

    public boolean isAdminCommandRequiresVote() { return adminCommandRequiresVote; }
    public void setAdminCommandRequiresVote(boolean v) { this.adminCommandRequiresVote = v; }

    public FilterMode getPlayerRequestFilterMode() { return playerRequestFilterMode; }
    public void setPlayerRequestFilterMode(FilterMode mode) { this.playerRequestFilterMode = mode; }

    public List<String> getPlayerRequestCommandList() { return playerRequestCommandList; }
    public void setPlayerRequestCommandList(List<String> list) { this.playerRequestCommandList = new ArrayList<>(list); }

    public boolean isPlayerRequestEnabled() { return playerRequestEnabled; }
    public void setPlayerRequestEnabled(boolean v) { this.playerRequestEnabled = v; }

    public boolean isConfigChangeRequiresVote() { return configChangeRequiresVote; }
    public void setConfigChangeRequiresVote(boolean v) { this.configChangeRequiresVote = v; }

    public int getVoteTimeoutSeconds() { return voteTimeoutSeconds; }
    public void setVoteTimeoutSeconds(int v) { this.voteTimeoutSeconds = Math.max(10, v); }

    public int getApprovalPercent() { return approvalPercent; }
    public void setApprovalPercent(int v) { this.approvalPercent = Math.max(1, Math.min(100, v)); }

    public boolean isAfkDefaultAccept() { return afkDefaultAccept; }
    public void setAfkDefaultAccept(boolean v) { this.afkDefaultAccept = v; }

    public int getAfkThresholdSeconds() { return afkThresholdSeconds; }
    public void setAfkThresholdSeconds(int v) { this.afkThresholdSeconds = Math.max(5, v); }

    public boolean isAllowTargetSelectors() { return allowTargetSelectors; }
    public void setAllowTargetSelectors(boolean v) { this.allowTargetSelectors = v; }

    // --- Filter logic ---

    /**
     * Check if a command requires voting when executed by an admin.
     */
    public boolean requiresVoteForAdminCommand(String command) {
        if (!adminCommandRequiresVote) return false;
        String normalized = normalizeCommand(command);
        boolean inList = adminCommandList.stream()
                .anyMatch(c -> normalizeCommand(c).equalsIgnoreCase(normalized));
        return adminCommandFilterMode == FilterMode.WHITELIST ? inList : !inList;
    }

    /**
     * Check if a player is allowed to request execution of a command.
     */
    public boolean isPlayerRequestAllowed(String command) {
        if (!playerRequestEnabled) return false;
        String normalized = normalizeCommand(command);
        boolean inList = playerRequestCommandList.stream()
                .anyMatch(c -> normalizeCommand(c).equalsIgnoreCase(normalized));
        return playerRequestFilterMode == FilterMode.WHITELIST ? inList : !inList;
    }

    /**
     * Check if a command contains target selectors (@a, @e, @r).
     */
    public boolean containsTargetSelectors(String command) {
        return command.contains("@a") || command.contains("@e") || command.contains("@r");
    }

    /**
     * Validate whether a command can enter the voting flow.
     */
    public boolean canEnterVoting(String command) {
        if (!allowTargetSelectors && containsTargetSelectors(command)) {
            return false;
        }
        return true;
    }

    /**
     * Calculate the number of accepts required to pass.
     */
    public int requiredAccepts(int eligibleCount) {
        if (eligibleCount <= 0) return 0;
        return Math.min(eligibleCount, Math.max(1,
                (int) Math.ceil(eligibleCount * (approvalPercent / 100.0))));
    }

    private static String normalizeCommand(String cmd) {
        String trimmed = cmd.trim();
        if (trimmed.startsWith("/")) trimmed = trimmed.substring(1);
        return trimmed;
    }

    /**
     * Create a deep copy of this config.
     */
    public VoteConfig copy() {
        VoteConfig c = new VoteConfig();
        c.adminCommandFilterMode = this.adminCommandFilterMode;
        c.adminCommandList = new ArrayList<>(this.adminCommandList);
        c.adminCommandRequiresVote = this.adminCommandRequiresVote;
        c.playerRequestFilterMode = this.playerRequestFilterMode;
        c.playerRequestCommandList = new ArrayList<>(this.playerRequestCommandList);
        c.playerRequestEnabled = this.playerRequestEnabled;
        c.configChangeRequiresVote = this.configChangeRequiresVote;
        c.voteTimeoutSeconds = this.voteTimeoutSeconds;
        c.approvalPercent = this.approvalPercent;
        c.afkDefaultAccept = this.afkDefaultAccept;
        c.afkThresholdSeconds = this.afkThresholdSeconds;
        c.allowTargetSelectors = this.allowTargetSelectors;
        return c;
    }
}
