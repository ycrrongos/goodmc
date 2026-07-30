package com.adminvote;

import java.util.UUID;

public record VoteRecord(UUID playerId, String playerName, VoteChoice choice, long timestamp) {

    public enum VoteChoice { ACCEPT, REJECT, ABSTAIN }
}
