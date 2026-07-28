package com.qqbridge;

import java.io.File;
import java.io.IOException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

final class QqCursorStorage {

    private final File file;
    private long cursor;

    QqCursorStorage(JavaPlugin plugin) {
        this.file = new File(plugin.getDataFolder(), "qq-bridge-cursor.yml");
    }

    void load() {
        if (!file.exists()) {
            cursor = 0;
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        cursor = config.getLong("cursor", 0L);
    }

    long cursor() {
        return cursor;
    }

    void save(long nextCursor) {
        cursor = nextCursor;
        YamlConfiguration config = new YamlConfiguration();
        config.set("cursor", cursor);
        try {
            file.getParentFile().mkdirs();
            config.save(file);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to save qq-bridge-cursor.yml", exception);
        }
    }
}
