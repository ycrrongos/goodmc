package com.qqbridge;

import java.util.Collections;
import java.util.List;

public final class QqPollResponse {

    private boolean ok;
    private long cursor;
    private long nextCursor;
    private int count;
    private List<QqMessage> messages;

    public boolean ok() {
        return ok;
    }

    public long cursor() {
        return cursor;
    }

    public long nextCursor() {
        return nextCursor;
    }

    public int count() {
        return count;
    }

    public List<QqMessage> messages() {
        return messages == null ? Collections.emptyList() : messages;
    }
}
