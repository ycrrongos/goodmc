package com.adminvote;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class AdminCommandVote {

    private static final long TIMEOUT_MS = 60_000L;

    private final UUID adminId;
    private final String adminName;
    private final String command;
    private final long createdAt;
    private final Set<UUID> accepted = new HashSet<>();
    private final Set<UUID> rejected = new HashSet<>();

    public AdminCommandVote(UUID adminId, String adminName, String command) {
        this.adminId = adminId;
        this.adminName = adminName;
        this.command = command;
        this.createdAt = System.currentTimeMillis();
    }

    public UUID adminId() {
        return adminId;
    }

    public String adminName() {
        return adminName;
    }

    public String command() {
        return command;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - createdAt > TIMEOUT_MS;
    }

    public boolean hasVoted(UUID playerId) {
        return accepted.contains(playerId) || rejected.contains(playerId);
    }

    public boolean recordYes(UUID playerId) {
        if (hasVoted(playerId)) {
            return false;
        }
        accepted.add(playerId);
        return true;
    }

    public boolean recordNo(UUID playerId) {
        if (hasVoted(playerId)) {
            return false;
        }
        rejected.add(playerId);
        return true;
    }

    public int acceptedCount() {
        return accepted.size();
    }

    public int rejectedCount() {
        return rejected.size();
    }
}
