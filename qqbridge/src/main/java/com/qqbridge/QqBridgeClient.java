package com.qqbridge;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

final class QqBridgeClient {

    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    private final QqBridgeConfig config;
    private final HttpClient httpClient;

    QqBridgeClient(QqBridgeConfig config) {
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    QqPollResponse pollMessages(long cursor, int limit) throws IOException, InterruptedException {
        String url = config.qqBridgeApiUrl()
                + "/api/v1/qq/messages?cursor=" + cursor
                + "&limit=" + limit;

        HttpResponse<String> response = httpClient.send(
                buildGet(url),
                HttpResponse.BodyHandlers.ofString()
        );
        return parsePollResponse(response);
    }

    void sendMessage(String message, String group) throws IOException, InterruptedException {
        String json = GSON.toJson(new QqSendRequest(message, group));

        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder()
                        .uri(URI.create(config.qqBridgeApiUrl() + "/api/v1/qq/send"))
                        .timeout(Duration.ofSeconds(15))
                        .header("Authorization", "Bearer " + config.qqBridgeApiKey())
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + ": " + response.body());
        }

        QqSendResponse sendResponse;
        try {
            sendResponse = GSON.fromJson(response.body(), QqSendResponse.class);
        } catch (JsonSyntaxException exception) {
            throw new IOException("Invalid JSON response", exception);
        }
        if (sendResponse == null || !sendResponse.ok()) {
            String error = sendResponse == null ? "null response" : sendResponse.error();
            throw new IOException("Send failed: " + error);
        }
    }

    private HttpRequest buildGet(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + config.qqBridgeApiKey())
                .GET()
                .build();
    }

    private QqPollResponse parsePollResponse(HttpResponse<String> response) throws IOException {
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + ": " + response.body());
        }
        try {
            QqPollResponse pollResponse = GSON.fromJson(response.body(), QqPollResponse.class);
            if (pollResponse == null || !pollResponse.ok()) {
                throw new IOException("API returned ok=false");
            }
            return pollResponse;
        } catch (JsonSyntaxException exception) {
            throw new IOException("Invalid JSON response", exception);
        }
    }
}
