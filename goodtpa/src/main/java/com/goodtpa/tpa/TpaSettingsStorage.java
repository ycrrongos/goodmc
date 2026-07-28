package com.goodtpa.tpa;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

final class TpaSettingsStorage {

    private final File file;
    private final Map<UUID, Integer> timeoutSecondsByPlayer = new HashMap<>();

    TpaSettingsStorage(JavaPlugin plugin) {
        this.file = new File(plugin.getDataFolder(), "tpa-settings.yml");
    }

    void load() {
        timeoutSecondsByPlayer.clear();
        if (!file.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        var section = config.getConfigurationSection("players");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(key);
                timeoutSecondsByPlayer.put(playerId, section.getInt(key + ".timeout-seconds"));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, Integer> entry : timeoutSecondsByPlayer.entrySet()) {
            config.set("players." + entry.getKey() + ".timeout-seconds", entry.getValue());
        }

        try {
            file.getParentFile().mkdirs();
            config.save(file);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save tpa-settings.yml", exception);
        }
    }

    Integer get(UUID playerId) {
        return timeoutSecondsByPlayer.get(playerId);
    }

    void set(UUID playerId, int timeoutSeconds) {
        timeoutSecondsByPlayer.put(playerId, timeoutSeconds);
    }
}
