package com.goodvote.server.handler;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.item.ItemStack;

import java.util.UUID;

/**
 * Server-side screen handler for the vote GUI.
 */
public class VoteScreenHandler extends ScreenHandler {

    public static final PacketCodec<PacketByteBuf, VoteScreenHandlerData> EXTRA_DATA_CODEC =
            new PacketCodec<>() {
                @Override
                public VoteScreenHandlerData decode(PacketByteBuf buf) {
                    return new VoteScreenHandlerData(
                            buf.readUuid(), buf.readVarInt(),
                            buf.readString(32767), buf.readString(32767),
                            buf.readLong()
                    );
                }
                @Override
                public void encode(PacketByteBuf buf, VoteScreenHandlerData data) {
                    buf.writeUuid(data.sessionId);
                    buf.writeVarInt(data.voteType);
                    buf.writeString(data.initiatorName);
                    buf.writeString(data.content);
                    buf.writeLong(data.remainingMs);
                }
            };

    private final VoteScreenHandlerData data;

    // Client constructor
    public VoteScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        super(null, syncId);
        this.data = new VoteScreenHandlerData(
                buf.readUuid(), buf.readVarInt(),
                buf.readString(32767), buf.readString(32767),
                buf.readLong()
        );
    }

    // Server constructor
    public VoteScreenHandler(int syncId, PlayerInventory playerInventory, VoteScreenHandlerData data) {
        super(null, syncId);
        this.data = data;
    }

    public VoteScreenHandlerData getData() { return data; }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    public record VoteScreenHandlerData(
            UUID sessionId,
            int voteType,
            String initiatorName,
            String content,
            long remainingMs
    ) {}
}
