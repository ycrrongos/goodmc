package com.goodvote.client.screen;

import com.goodvote.network.GoodVotePackets;
import com.goodvote.network.PluginBridgePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import java.util.UUID;

/**
 * Handles JSON messages from the Paper AdminVote plugin via the goodvote:main channel.
 * Parses vote start/update/conclude messages and shows/updates the VoteScreen.
 */
public final class PluginBridgeHandler {

    // Shared state for plugin bridge votes
    private static UUID bridgeSessionId = null;

    /** Check if a session ID was created by the plugin bridge */
    public static boolean isBridgeSession(UUID sessionId) {
        return sessionId != null && sessionId.equals(bridgeSessionId);
    }

    public static void handle(String json) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        String type = extractString(json, "type");
        if (type == null) return;

        switch (type) {
            case "start" -> handleStart(json, client);
            case "update" -> handleUpdate(json, client);
            case "conclude" -> handleConclude(json, client);
        }
    }

    private static void handleStart(String json, MinecraftClient client) {
        String admin = extractString(json, "admin");
        String cmd = extractString(json, "cmd");
        int required = extractInt(json, "required");
        int eligible = extractInt(json, "eligible");
        int timeout = extractInt(json, "timeout");
        String vtype = extractString(json, "vtype");

        int voteType = "PLAYER_REQUEST".equals(vtype) ? 1 : 0;
        bridgeSessionId = UUID.randomUUID();

        // Create OpenVotePayload-compatible data
        var payload = new GoodVotePackets.OpenVotePayload(
                bridgeSessionId, voteType, admin, cmd,
                timeout * 1000L, eligible, 0, 0, 0, false, -1
        );

        client.execute(() -> client.setScreen(new VoteScreen(payload)));
    }

    private static void handleUpdate(String json, MinecraftClient client) {
        if (bridgeSessionId == null) return;
        int accept = extractInt(json, "accept");
        int reject = extractInt(json, "reject");
        int abstain = extractInt(json, "abstain");
        int eligible = extractInt(json, "eligible");
        int remaining = extractInt(json, "remaining");

        var update = new GoodVotePackets.VoteUpdatePayload(
                bridgeSessionId, accept, reject, abstain, eligible,
                remaining * 1000L, false, -1
        );

        client.execute(() -> {
            Screen screen = client.currentScreen;
            if (screen instanceof VoteScreen voteScreen) {
                voteScreen.updateVote(update);
            }
        });
    }

    private static void handleConclude(String json, MinecraftClient client) {
        if (bridgeSessionId == null) return;
        boolean passed = extractBoolean(json, "passed");
        String cmd = extractString(json, "cmd");
        String admin = extractString(json, "admin");

        var result = new GoodVotePackets.VoteResultPayload(
                bridgeSessionId, passed, cmd, admin, 0, 0, 0, 0, 0
        );

        client.execute(() -> {
            Screen screen = client.currentScreen;
            if (screen instanceof VoteScreen voteScreen) {
                voteScreen.showResult(result);
            }
        });
    }

    // --- Simple JSON parsing (no library needed for flat JSON) ---

    private static String extractString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) return null;
        start += search.length();
        int end = json.indexOf('"', start);
        if (end < 0) return null;
        return json.substring(start, end).replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static int extractInt(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start < 0) return 0;
        start += search.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        try {
            return Integer.parseInt(json.substring(start, end));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static boolean extractBoolean(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start < 0) return false;
        start += search.length();
        return json.substring(start).trim().startsWith("true");
    }

    private PluginBridgeHandler() {}
}
