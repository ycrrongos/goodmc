package com.goodvote.server.handler;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;

/**
 * Server-side screen handler for the config GUI.
 */
public class ConfigScreenHandler extends ScreenHandler {

    // Client constructor
    public ConfigScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        super(null, syncId);
    }

    // Server constructor
    public ConfigScreenHandler(int syncId, PlayerInventory playerInventory) {
        super(null, syncId);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return player.hasPermissionLevel(2);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }
}
