package com.goodvote.vote;

/**
 * The choice a player can make during a vote.
 */
public enum VoteChoice {
    ACCEPT,
    REJECT,
    ABSTAIN;

    public static VoteChoice fromId(int id) {
        return switch (id) {
            case 0 -> ACCEPT;
            case 1 -> REJECT;
            case 2 -> ABSTAIN;
            default -> throw new IllegalArgumentException("Unknown VoteChoice id: " + id);
        };
    }

    public int toId() {
        return ordinal();
    }
}
