package com.serverfakeplayer.permission;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Persisted allowlist for fake-player usage. Ops always have access.
 */
public final class FakePlayerPermissionStore {

    private final JavaPlugin plugin;
    private final File file;
    private final Set<UUID> allowed = new LinkedHashSet<>();

    public FakePlayerPermissionStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "allowed-players.yml");
        load();
    }

    public void load() {
        allowed.clear();
        if (!file.exists()) {
            save();
            return;
        }
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        List<String> raw = yaml.getStringList("allowed");
        for (String entry : raw) {
            try {
                allowed.add(UUID.fromString(entry));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("忽略无效 UUID: " + entry);
            }
        }
    }

    public void save() {
        FileConfiguration yaml = new YamlConfiguration();
        List<String> list = allowed.stream().map(UUID::toString).toList();
        yaml.set("allowed", list);
        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "无法保存 allowed-players.yml", exception);
        }
    }

    public boolean canUse(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return true;
        }
        if (player.isOp()) {
            return true;
        }
        if (allowed.contains(player.getUniqueId())) {
            return true;
        }
        return player.hasPermission("serverfakeplayer.use");
    }

    public boolean canSpawn(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return false;
        }
        if (player.isOp()) {
            return true;
        }
        if (allowed.contains(player.getUniqueId())) {
            return true;
        }
        return player.hasPermission("serverfakeplayer.spawn");
    }

    public boolean grant(OfflinePlayer target) {
        UUID id = target.getUniqueId();
        if (allowed.contains(id)) {
            return false;
        }
        allowed.add(id);
        save();
        return true;
    }

    public boolean revoke(OfflinePlayer target) {
        UUID id = target.getUniqueId();
        if (!allowed.contains(id)) {
            return false;
        }
        allowed.remove(id);
        save();
        return true;
    }

    public boolean isAllowed(UUID uuid) {
        return allowed.contains(uuid);
    }

    public Set<UUID> allowed() {
        return Collections.unmodifiableSet(allowed);
    }

    public static OfflinePlayer resolvePlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }
        for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
            String n = offline.getName();
            if (n != null && n.equalsIgnoreCase(name)) {
                return offline;
            }
        }
        return Bukkit.getOfflinePlayer(name);
    }

    public String displayName(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        String name = player.getName();
        return name != null ? name : uuid.toString();
    }

    public List<String> tabCompleteNames(String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(p))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }
}
