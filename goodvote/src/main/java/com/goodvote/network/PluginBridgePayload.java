package com.goodvote.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Payload for plugin bridge communication between Paper plugin and Fabric client.
 * The Paper plugin sends raw UTF-8 JSON on channel "goodvote:main" via Bukkit plugin messaging.
 * This payload type allows the Fabric client to receive those messages.
 */
public record PluginBridgePayload(String data) implements CustomPayload {

    public static final Id<PluginBridgePayload> ID = new Id<>(Identifier.of("goodvote", "main"));

    public static final PacketCodec<PacketByteBuf, PluginBridgePayload> CODEC = new PacketCodec<>() {
        @Override
        public PluginBridgePayload decode(PacketByteBuf buf) {
            // Read all remaining bytes as UTF-8 string (the raw JSON from the plugin)
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            return new PluginBridgePayload(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
        }

        @Override
        public void encode(PacketByteBuf buf, PluginBridgePayload payload) {
            buf.writeBytes(payload.data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
