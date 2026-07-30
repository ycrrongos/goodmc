package com.adminvote;

import java.util.*;

public final class AdminCommandVote {

    public enum VoteType { ADMIN_COMMAND, PLAYER_REQUEST }

    private final UUID adminId;
    private final String adminName;
    private final String command;
    private final VoteType type;
    private final long createdAt;
    private final long timeoutMs;
    private final Set<UUID> eligibleVoters;
    private final Map<UUID, VoteRecord> records = new LinkedHashMap<>();
    private boolean concluded = false;
    private boolean passed = false;

    public AdminCommandVote(UUID adminId, String adminName, String command, VoteType type,
                            long timeoutMs, Set<UUID> eligibleVoters) {
        this.adminId = adminId;
        this.adminName = adminName;
        this.command = command;
        this.type = type;
        this.createdAt = System.currentTimeMillis();
        this.timeoutMs = timeoutMs;
        this.eligibleVoters = new HashSet<>(eligibleVoters);
    }

    public UUID adminId() { return adminId; }
    public String adminName() { return adminName; }
    public String command() { return command; }
    public VoteType type() { return type; }
    public long createdAt() { return createdAt; }
    public long timeoutMs() { return timeoutMs; }
    public Set<UUID> eligibleVoters() { return Collections.unmodifiableSet(eligibleVoters); }
    public Map<UUID, VoteRecord> records() { return Collections.unmodifiableMap(records); }
    public boolean isConcluded() { return concluded; }
    public boolean isPassed() { return passed; }

    public boolean isExpired() {
        return System.currentTimeMillis() - createdAt > timeoutMs;
    }

    public long remainingMs() {
        return Math.max(0, timeoutMs - (System.currentTimeMillis() - createdAt));
    }

    public boolean hasVoted(UUID playerId) {
        return records.containsKey(playerId);
    }

    public boolean isEligible(UUID playerId) {
        return eligibleVoters.contains(playerId);
    }

    public boolean recordVote(UUID playerId, String playerName, VoteRecord.VoteChoice choice) {
        if (hasVoted(playerId) || !isEligible(playerId)) return false;
        records.put(playerId, new VoteRecord(playerId, playerName, choice, System.currentTimeMillis()));
        return true;
    }

    public int acceptedCount() {
        return (int) records.values().stream().filter(r -> r.choice() == VoteRecord.VoteChoice.ACCEPT).count();
    }

    public int rejectedCount() {
        return (int) records.values().stream().filter(r -> r.choice() == VoteRecord.VoteChoice.REJECT).count();
    }

    public int abstainedCount() {
        return (int) records.values().stream().filter(r -> r.choice() == VoteRecord.VoteChoice.ABSTAIN).count();
    }

    public int votedCount() { return records.size(); }
    public int eligibleCount() { return eligibleVoters.size(); }

    public Set<UUID> getUnvotedPlayers() {
        Set<UUID> unvoted = new HashSet<>(eligibleVoters);
        unvoted.removeAll(records.keySet());
        return unvoted;
    }

    public boolean isDefinitivelyDecided(int requiredAccepts) {
        if (acceptedCount() >= requiredAccepts) return true;
        int remaining = eligibleCount() - votedCount();
        return acceptedCount() + remaining < requiredAccepts;
    }

    public void conclude(boolean passed) {
        this.concluded = true;
        this.passed = passed;
    }
}
