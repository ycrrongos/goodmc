package com.goodvote.server.listener;

import com.goodvote.server.GoodVoteServer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Server-side event listener for player join/leave, AFK detection, and server lifecycle.
 */
public class ServerEventListener {

    public static void register(GoodVoteServer server) {
        // Server started
        ServerLifecycleEvents.SERVER_STARTED.register(mcServer -> {
            server.setServer(mcServer);
        });

        // Player join - update activity and notify
        ServerPlayConnectionEvents.JOIN.register((handler, sender, mcServer) -> {
            ServerPlayerEntity player = handler.getPlayer();
            server.getVoteManager().updatePlayerActivity(player.getUuid());
        });

        // Player disconnect - cleanup
        ServerPlayConnectionEvents.DISCONNECT.register((handler, mcServer) -> {
            server.getVoteManager().onPlayerDisconnect(handler.getPlayer().getUuid());
        });
    }
}
