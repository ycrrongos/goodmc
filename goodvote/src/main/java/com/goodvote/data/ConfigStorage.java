package com.goodvote.data;

import com.goodvote.config.FilterMode;
import com.goodvote.config.VoteConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Handles loading and saving VoteConfig as JSON.
 */
public class ConfigStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "goodvote.json";

    private final Path configPath;

    public ConfigStorage() {
        this.configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
    }

    public ConfigStorage(Path configPath) {
        this.configPath = configPath;
    }

    public VoteConfig load() {
        if (!Files.exists(configPath)) {
            VoteConfig defaults = new VoteConfig();
            save(defaults);
            return defaults;
        }
        try {
            String json = Files.readString(configPath, StandardCharsets.UTF_8);
            ConfigData data = GSON.fromJson(json, ConfigData.class);
            return fromData(data);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load GoodVote config", e);
        }
    }

    public void save(VoteConfig config) {
        try {
            Files.createDirectories(configPath.getParent());
            ConfigData data = toData(config);
            String json = GSON.toJson(data);
            Files.writeString(configPath, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save GoodVote config", e);
        }
    }

    private ConfigData toData(VoteConfig config) {
        ConfigData d = new ConfigData();
        d.adminCommandFilterMode = config.getAdminCommandFilterMode().name();
        d.adminCommandList = config.getAdminCommandList();
        d.adminCommandRequiresVote = config.isAdminCommandRequiresVote();
        d.playerRequestFilterMode = config.getPlayerRequestFilterMode().name();
        d.playerRequestCommandList = config.getPlayerRequestCommandList();
        d.playerRequestEnabled = config.isPlayerRequestEnabled();
        d.configChangeRequiresVote = config.isConfigChangeRequiresVote();
        d.voteTimeoutSeconds = config.getVoteTimeoutSeconds();
        d.approvalPercent = config.getApprovalPercent();
        d.afkDefaultAccept = config.isAfkDefaultAccept();
        d.afkThresholdSeconds = config.getAfkThresholdSeconds();
        d.allowTargetSelectors = config.isAllowTargetSelectors();
        return d;
    }

    private VoteConfig fromData(ConfigData data) {
        VoteConfig config = new VoteConfig();
        config.setAdminCommandFilterMode(FilterMode.valueOf(data.adminCommandFilterMode));
        config.setAdminCommandList(data.adminCommandList);
        config.setAdminCommandRequiresVote(data.adminCommandRequiresVote);
        config.setPlayerRequestFilterMode(FilterMode.valueOf(data.playerRequestFilterMode));
        config.setPlayerRequestCommandList(data.playerRequestCommandList);
        config.setPlayerRequestEnabled(data.playerRequestEnabled);
        config.setConfigChangeRequiresVote(data.configChangeRequiresVote);
        config.setVoteTimeoutSeconds(data.voteTimeoutSeconds);
        config.setApprovalPercent(data.approvalPercent);
        config.setAfkDefaultAccept(data.afkDefaultAccept);
        config.setAfkThresholdSeconds(data.afkThresholdSeconds);
        config.setAllowTargetSelectors(data.allowTargetSelectors);
        return config;
    }

    /**
     * Internal JSON data class.
     */
    private static class ConfigData {
        String adminCommandFilterMode = "BLACKLIST";
        java.util.List<String> adminCommandList = new java.util.ArrayList<>();
        boolean adminCommandRequiresVote = true;
        String playerRequestFilterMode = "WHITELIST";
        java.util.List<String> playerRequestCommandList = new java.util.ArrayList<>();
        boolean playerRequestEnabled = true;
        boolean configChangeRequiresVote = true;
        int voteTimeoutSeconds = 60;
        int approvalPercent = 80;
        boolean afkDefaultAccept = true;
        int afkThresholdSeconds = 30;
        boolean allowTargetSelectors = false;
    }
}
