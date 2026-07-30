package com.goodvote.vote;

import java.util.UUID;

/**
 * A single player's vote record.
 */
public record VoteRecord(UUID playerId, String playerName, VoteChoice choice, long timestamp) {
}
