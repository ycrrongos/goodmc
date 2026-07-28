package com.goodtpa.tpa;

import java.util.UUID;

public final class TpaRequest {

    private final UUID requesterId;
    private final UUID targetId;
    private final TpaType type;
    private final long createdAt;
    private final long timeoutMillis;

    public TpaRequest(UUID requesterId, UUID targetId, TpaType type, long timeoutMillis) {
        this.requesterId = requesterId;
        this.targetId = targetId;
        this.type = type;
        this.createdAt = System.currentTimeMillis();
        this.timeoutMillis = timeoutMillis;
    }

    public UUID requesterId() {
        return requesterId;
    }

    public UUID targetId() {
        return targetId;
    }

    public TpaType type() {
        return type;
    }

    public long createdAt() {
        return createdAt;
    }

    public long timeoutMillis() {
        return timeoutMillis;
    }

    public boolean isExpired() {
        return remainingMillis() <= 0;
    }

    public long remainingMillis() {
        return Math.max(0, timeoutMillis - (System.currentTimeMillis() - createdAt));
    }
}
