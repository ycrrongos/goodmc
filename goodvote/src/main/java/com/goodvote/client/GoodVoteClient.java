package com.goodvote.client;

import com.goodvote.GoodVoteMod;
import com.goodvote.client.network.ClientPacketHandler;
import net.fabricmc.api.ClientModInitializer;

public class GoodVoteClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Register packets
        ClientPacketHandler.register();

        GoodVoteMod.LOGGER.info("GoodVote client initialized");
    }
}
