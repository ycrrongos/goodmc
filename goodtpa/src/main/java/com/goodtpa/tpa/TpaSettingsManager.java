package com.goodtpa.tpa;

import com.goodtpa.GoodTpaConfig;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class TpaSettingsManager {

    private final TpaSettingsStorage storage;
    private final GoodTpaConfig config;

    public TpaSettingsManager(JavaPlugin plugin, GoodTpaConfig config) {
        this.config = config;
        this.storage = new TpaSettingsStorage(plugin);
    }

    public void load() {
        storage.load();
    }

    void save() {
        storage.save();
    }

    public int getTimeoutSeconds(UUID playerId) {
        Integer custom = storage.get(playerId);
        if (custom != null) {
            return clamp(custom);
        }
        return config.tpaDefaultTimeoutSeconds();
    }

    public long getTimeoutMillis(UUID playerId) {
        return getTimeoutSeconds(playerId) * 1000L;
    }

    public int setTimeoutSeconds(Player player, int timeoutSeconds) {
        int clamped = clamp(timeoutSeconds);
        storage.set(player.getUniqueId(), clamped);
        storage.save();
        return clamped;
    }

    public int minTimeoutSeconds() {
        return config.tpaMinTimeoutSeconds();
    }

    public int maxTimeoutSeconds() {
        return config.tpaMaxTimeoutSeconds();
    }

    private int clamp(int timeoutSeconds) {
        return Math.max(minTimeoutSeconds(), Math.min(maxTimeoutSeconds(), timeoutSeconds));
    }
}
