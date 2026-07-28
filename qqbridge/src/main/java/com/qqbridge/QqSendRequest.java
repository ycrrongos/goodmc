package com.qqbridge;

final class QqSendRequest {

    private final String message;
    private final String group;

    QqSendRequest(String message, String group) {
        this.message = message;
        this.group = group;
    }
}
