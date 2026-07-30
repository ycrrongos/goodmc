package com.goodvote.vote;

import com.goodvote.config.VoteConfig;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core voting engine that manages active vote sessions.
 */
public class VoteManager {
    private final VoteConfig config;
    private final Map<UUID, VoteSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastActiveTime = new ConcurrentHashMap<>(); // player UUID -> last active timestamp
    private final VoteCallback callback;

    /**
     * Callback interface for vote events.
     */
    public interface VoteCallback {
        void onVoteStarted(VoteSession session);
        void onVoteUpdated(VoteSession session);
        void onVoteConcluded(VoteSession session, boolean passed);
        void openVoteScreen(ServerPlayerEntity player, VoteSession session);
        void broadcastMessage(String message);
        void sendActionBar(ServerPlayerEntity player, String message);
    }

    public VoteManager(VoteConfig config, VoteCallback callback) {
        this.config = config;
        this.callback = callback;
    }

    public VoteConfig getConfig() { return config; }

    public Optional<VoteSession> getActiveSession(UUID sessionId) {
        VoteSession session = activeSessions.get(sessionId);
        if (session != null && session.isExpired() && !session.isConcluded()) {
            concludeSession(session);
        }
        return Optional.ofNullable(activeSessions.get(sessionId));
    }

    public Collection<VoteSession> getActiveSessions() {
        return Collections.unmodifiableCollection(activeSessions.values());
    }

    /**
     * Start a new vote session.
     */
    public VoteSession startVote(VoteType type, UUID initiatorId, String initiatorName,
                                  String content, Set<UUID> eligibleVoters) {
        UUID sessionId = UUID.randomUUID();
        long timeoutMs = config.getVoteTimeoutSeconds() * 1000L;
        VoteSession session = new VoteSession(sessionId, type, initiatorId, initiatorName,
                content, timeoutMs, eligibleVoters);
        activeSessions.put(sessionId, session);
        callback.onVoteStarted(session);
        return session;
    }

    /**
     * Record a player's vote.
     */
    public VoteResult castVote(UUID sessionId, UUID playerId, String playerName, VoteChoice choice) {
        VoteSession session = activeSessions.get(sessionId);
        if (session == null) return VoteResult.NOT_FOUND;
        if (session.isConcluded()) return VoteResult.ALREADY_CONCLUDED;
        if (session.isExpired()) {
            concludeSession(session);
            return VoteResult.EXPIRED;
        }
        if (!session.isEligible(playerId)) return VoteResult.NOT_ELIGIBLE;
        if (session.hasVoted(playerId)) return VoteResult.ALREADY_VOTED;

        boolean recorded = session.castVote(playerId, playerName, choice);
        if (!recorded) return VoteResult.ERROR;

        // Update last active time
        lastActiveTime.put(playerId, System.currentTimeMillis());

        // Check if vote is decided
        int required = config.requiredAccepts(session.getEligibleCount());
        if (session.getAcceptCount() >= required) {
            concludeSession(session);
            return VoteResult.PASSED;
        }
        if (session.isDefinitivelyDecided(required)) {
            concludeSession(session);
            return VoteResult.FAILED;
        }

        callback.onVoteUpdated(session);
        return VoteResult.RECORDED;
    }

    /**
     * Tick called every server tick to check for expired sessions and AFK players.
     */
    public void tick(ServerWorld world) {
        // Check for expired sessions
        for (VoteSession session : activeSessions.values()) {
            if (!session.isConcluded() && session.isExpired()) {
                // AFK handling: auto-vote for unvoted eligible players
                handleAfkPlayers(session);
                concludeSession(session);
            }
        }
    }

    /**
     * Handle AFK players at vote conclusion - they default to ACCEPT.
     */
    private void handleAfkPlayers(VoteSession session) {
        if (!config.isAfkDefaultAccept()) return;
        Set<UUID> unvoted = session.getUnvotedPlayers();
        for (UUID playerId : unvoted) {
            // Record as ACCEPT for AFK players
            session.castVote(playerId, "AFK", VoteChoice.ACCEPT);
        }
    }

    /**
     * Conclude a vote session and determine the result.
     */
    private void concludeSession(VoteSession session) {
        if (session.isConcluded()) return;
        int required = config.requiredAccepts(session.getEligibleCount());
        boolean passed = session.getAcceptCount() >= required;
        session.conclude(passed);
        activeSessions.remove(session.getId());
        callback.onVoteConcluded(session, passed);
    }

    /**
     * Update player's last active time (called on movement, interaction, etc).
     */
    public void updatePlayerActivity(UUID playerId) {
        lastActiveTime.put(playerId, System.currentTimeMillis());
    }

    /**
     * Check if a player is considered AFK.
     */
    public boolean isPlayerAfk(UUID playerId) {
        Long lastActive = lastActiveTime.get(playerId);
        if (lastActive == null) return false;
        long thresholdMs = config.getAfkThresholdSeconds() * 1000L;
        return System.currentTimeMillis() - lastActive > thresholdMs;
    }

    /**
     * Remove player data on disconnect.
     */
    public void onPlayerDisconnect(UUID playerId) {
        lastActiveTime.remove(playerId);
    }

    /**
     * Open vote screen for a player.
     */
    public void openVoteScreenForPlayer(ServerPlayerEntity player, VoteSession session) {
        callback.openVoteScreen(player, session);
    }

    /**
     * Broadcast vote start to all eligible voters.
     */
    public void broadcastVoteStart(VoteSession session, List<ServerPlayerEntity> eligiblePlayers) {
        for (ServerPlayerEntity player : eligiblePlayers) {
            callback.openVoteScreen(player, session);
        }
    }

    public enum VoteResult {
        RECORDED,
        PASSED,
        FAILED,
        NOT_FOUND,
        NOT_ELIGIBLE,
        ALREADY_VOTED,
        ALREADY_CONCLUDED,
        EXPIRED,
        ERROR
    }
}
