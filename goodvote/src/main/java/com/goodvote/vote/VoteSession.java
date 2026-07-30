package com.goodvote.vote;

import java.util.*;

/**
 * Represents an active voting session.
 */
public class VoteSession {
    private final UUID id;
    private final VoteType type;
    private final UUID initiatorId;
    private final String initiatorName;
    private final String content; // command line or config change description
    private final long createdAt;
    private final long timeoutMs;
    private final Set<UUID> eligibleVoters;
    private final Map<UUID, VoteRecord> votes = new LinkedHashMap<>();
    private boolean concluded = false;
    private boolean passed = false;

    public VoteSession(UUID id, VoteType type, UUID initiatorId, String initiatorName,
                       String content, long timeoutMs, Set<UUID> eligibleVoters) {
        this.id = id;
        this.type = type;
        this.initiatorId = initiatorId;
        this.initiatorName = initiatorName;
        this.content = content;
        this.createdAt = System.currentTimeMillis();
        this.timeoutMs = timeoutMs;
        this.eligibleVoters = new HashSet<>(eligibleVoters);
    }

    public UUID getId() { return id; }
    public VoteType getType() { return type; }
    public UUID getInitiatorId() { return initiatorId; }
    public String getInitiatorName() { return initiatorName; }
    public String getContent() { return content; }
    public long getCreatedAt() { return createdAt; }
    public long getTimeoutMs() { return timeoutMs; }
    public Set<UUID> getEligibleVoters() { return Collections.unmodifiableSet(eligibleVoters); }
    public Map<UUID, VoteRecord> getVotes() { return Collections.unmodifiableMap(votes); }
    public boolean isConcluded() { return concluded; }
    public boolean isPassed() { return passed; }

    public boolean isExpired() {
        return System.currentTimeMillis() - createdAt > timeoutMs;
    }

    public long getRemainingMs() {
        return Math.max(0, timeoutMs - (System.currentTimeMillis() - createdAt));
    }

    public boolean hasVoted(UUID playerId) {
        return votes.containsKey(playerId);
    }

    public boolean isEligible(UUID playerId) {
        return eligibleVoters.contains(playerId);
    }

    /**
     * Record a player's vote. Returns true if the vote was recorded, false if already voted or not eligible.
     */
    public boolean castVote(UUID playerId, String playerName, VoteChoice choice) {
        if (concluded || hasVoted(playerId) || !isEligible(playerId)) {
            return false;
        }
        votes.put(playerId, new VoteRecord(playerId, playerName, choice, System.currentTimeMillis()));
        return true;
    }

    public int getAcceptCount() {
        return (int) votes.values().stream().filter(r -> r.choice() == VoteChoice.ACCEPT).count();
    }

    public int getRejectCount() {
        return (int) votes.values().stream().filter(r -> r.choice() == VoteChoice.REJECT).count();
    }

    public int getAbstainCount() {
        return (int) votes.values().stream().filter(r -> r.choice() == VoteChoice.ABSTAIN).count();
    }

    public int getVotedCount() {
        return votes.size();
    }

    public int getEligibleCount() {
        return eligibleVoters.size();
    }

    /**
     * Get players who haven't voted yet.
     */
    public Set<UUID> getUnvotedPlayers() {
        Set<UUID> unvoted = new HashSet<>(eligibleVoters);
        unvoted.removeAll(votes.keySet());
        return unvoted;
    }

    /**
     * Conclude the vote with the given result.
     */
    public void conclude(boolean passed) {
        this.concluded = true;
        this.passed = passed;
    }

    /**
     * Check if the vote has reached a definitive result (all eligible have voted or mathematically decided).
     */
    public boolean isDefinitivelyDecided(int requiredAccepts) {
        if (getAcceptCount() >= requiredAccepts) return true;
        int remaining = getEligibleCount() - getVotedCount();
        if (getAcceptCount() + remaining < requiredAccepts) return true;
        return false;
    }
}
