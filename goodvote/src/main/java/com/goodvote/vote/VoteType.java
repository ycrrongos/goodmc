package com.goodvote.vote;

/**
 * The type of vote being conducted.
 */
public enum VoteType {
    /** Admin executing a command that requires voting */
    COMMAND,
    /** Player requesting to execute an admin command */
    PLAYER_REQUEST,
    /** Server config change requiring voting */
    CONFIG_CHANGE;

    public static VoteType fromId(int id) {
        return switch (id) {
            case 0 -> COMMAND;
            case 1 -> PLAYER_REQUEST;
            case 2 -> CONFIG_CHANGE;
            default -> throw new IllegalArgumentException("Unknown VoteType id: " + id);
        };
    }

    public int toId() {
        return ordinal();
    }
}
