package com.qqbridge;

public final class QqMessage {

    private long id;
    private String ts;
    private String group;
    private String sender;
    private String message;
    private String key;

    public long id() {
        return id;
    }

    public String ts() {
        return ts;
    }

    public String group() {
        return group;
    }

    public String sender() {
        return sender;
    }

    public String message() {
        return message;
    }

    public String key() {
        return key;
    }
}
