package com.goodvote.config;

/**
 * Filter mode for command lists.
 */
public enum FilterMode {
    /** Only listed commands require voting / are allowed */
    WHITELIST,
    /** All commands except listed ones require voting / are allowed */
    BLACKLIST;

    public static FilterMode fromId(int id) {
        return id == 0 ? WHITELIST : BLACKLIST;
    }

    public int toId() {
        return ordinal();
    }

    public FilterMode toggle() {
        return this == WHITELIST ? BLACKLIST : WHITELIST;
    }
}
