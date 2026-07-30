package com.goodvote.client.network;

import com.goodvote.client.screen.ConfigScreen;
import com.goodvote.client.screen.VoteScreen;
import com.goodvote.network.GoodVotePackets;
import com.goodvote.network.PluginBridgePayload;
import com.goodvote.client.screen.PluginBridgeHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Handles client-side packet registration and processing.
 */
public class ClientPacketHandler {

    private static GoodVotePackets.ConfigData cachedConfigData;

    public static void register() {
        // Register S2C payload types on client (Fabric server communication)
        PayloadTypeRegistry.playS2C().register(GoodVotePackets.OpenVotePayload.ID, GoodVotePackets.OpenVotePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(GoodVotePackets.VoteUpdatePayload.ID, GoodVotePackets.VoteUpdatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(GoodVotePackets.VoteResultPayload.ID, GoodVotePackets.VoteResultPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(GoodVotePackets.OpenConfigPayload.ID, GoodVotePackets.OpenConfigPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(GoodVotePackets.ConfigSyncS2CPayload.ID, GoodVotePackets.ConfigSyncS2CPayload.CODEC);

        // Register plugin bridge payload (Paper plugin communication)
        PayloadTypeRegistry.playS2C().register(PluginBridgePayload.ID, PluginBridgePayload.CODEC);

        // Register C2S payload types on client
        PayloadTypeRegistry.playC2S().register(GoodVotePackets.CastVotePayload.ID, GoodVotePackets.CastVotePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(GoodVotePackets.ConfigSyncC2SPayload.ID, GoodVotePackets.ConfigSyncC2SPayload.CODEC);

        // Handle plugin bridge messages from Paper plugin
        ClientPlayNetworking.registerGlobalReceiver(PluginBridgePayload.ID, (payload, context) -> {
            context.client().execute(() -> PluginBridgeHandler.handle(payload.data()));
        });

        // Handle open vote screen (Fabric server)
        ClientPlayNetworking.registerGlobalReceiver(GoodVotePackets.OpenVotePayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                MinecraftClient client = MinecraftClient.getInstance();
                client.setScreen(new VoteScreen(payload));
            });
        });

        // Handle vote update (Fabric server)
        ClientPlayNetworking.registerGlobalReceiver(GoodVotePackets.VoteUpdatePayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.currentScreen instanceof VoteScreen voteScreen) {
                    voteScreen.updateVote(payload);
                }
            });
        });

        // Handle vote result (Fabric server)
        ClientPlayNetworking.registerGlobalReceiver(GoodVotePackets.VoteResultPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.currentScreen instanceof VoteScreen voteScreen) {
                    voteScreen.showResult(payload);
                }
            });
        });

        // Handle config sync (receive config data before opening config screen)
        ClientPlayNetworking.registerGlobalReceiver(GoodVotePackets.ConfigSyncS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> cachedConfigData = payload.data());
        });

        // Handle open config screen
        ClientPlayNetworking.registerGlobalReceiver(GoodVotePackets.OpenConfigPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                MinecraftClient client = MinecraftClient.getInstance();
                if (cachedConfigData != null) {
                    client.setScreen(new ConfigScreen(cachedConfigData));
                    cachedConfigData = null;
                }
            });
        });

        // Send "hello" to Paper plugin on join so it knows we have the mod
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            // Send hello via plugin bridge channel
            byte[] helloBytes = "hello".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            var buf = io.netty.buffer.Unpooled.wrappedBuffer(helloBytes);
            var payload = new PluginBridgePayload("hello");
            ClientPlayNetworking.send(payload);
        });
    }
}
