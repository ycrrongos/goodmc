package com.qqbridge;

public final class QqSendResponse {

    private boolean ok;
    private String error;
    private long id;
    private String ts;
    private String group;
    private String status;

    public boolean ok() {
        return ok;
    }

    public String error() {
        return error;
    }
}
