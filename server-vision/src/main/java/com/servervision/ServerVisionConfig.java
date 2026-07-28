package com.servervision;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class ServerVisionConfig {

    private static final List<String> DEFAULT_BLOCKED = List.of(
            "tpa",
            "tpahere",
            "back",
            "tpaccept",
            "tpaback",
            "kill"
    );

    private final JavaPlugin plugin;
    private boolean freecamCommandEnabled = true;
    private boolean fullbrightCommandEnabled = true;
    private Set<String> blockedCommands = new HashSet<>(DEFAULT_BLOCKED);

    public ServerVisionConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        freecamCommandEnabled = config.getBoolean("commands.freecam", true);
        fullbrightCommandEnabled = config.getBoolean("commands.fullbright", true);

        List<String> configured = config.getStringList("blocked-commands");
        Set<String> parsed = new HashSet<>();
        List<String> source = configured.isEmpty() ? DEFAULT_BLOCKED : configured;
        for (String entry : source) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            String command = entry.trim().toLowerCase(Locale.ROOT);
            if (command.startsWith("/")) {
                command = command.substring(1);
            }
            if (command.contains(":")) {
                command = command.substring(command.indexOf(':') + 1);
            }
            if (!command.isEmpty()) {
                parsed.add(command);
            }
        }
        blockedCommands = Set.copyOf(parsed);
    }

    public boolean freecamCommandEnabled() {
        return freecamCommandEnabled;
    }

    public boolean fullbrightCommandEnabled() {
        return fullbrightCommandEnabled;
    }

    public Set<String> blockedCommands() {
        return blockedCommands;
    }
}
