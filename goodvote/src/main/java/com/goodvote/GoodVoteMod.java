package com.goodvote;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoodVoteMod implements ModInitializer {
    public static final String MOD_ID = "goodvote";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("GoodVote mod loading...");
    }
}
