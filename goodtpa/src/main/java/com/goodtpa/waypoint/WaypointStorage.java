package com.goodtpa.waypoint;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class WaypointStorage {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<UUID, Waypoint> publicWaypoints = new HashMap<>();
    private final Map<UUID, Map<UUID, Waypoint>> privateWaypoints = new HashMap<>();

    public WaypointStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "waypoints.yml");
    }

    public void load() {
        publicWaypoints.clear();
        privateWaypoints.clear();
        if (!file.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        loadSection(config.getConfigurationSection("public"), null, publicWaypoints);
        ConfigurationSection privateSection = config.getConfigurationSection("private");
        if (privateSection != null) {
            for (String ownerKey : privateSection.getKeys(false)) {
                UUID ownerId = UUID.fromString(ownerKey);
                Map<UUID, Waypoint> ownerWaypoints = new HashMap<>();
                loadSection(privateSection.getConfigurationSection(ownerKey), ownerId, ownerWaypoints);
                privateWaypoints.put(ownerId, ownerWaypoints);
            }
        }
    }

    private static void loadSection(ConfigurationSection section, UUID ownerId, Map<UUID, Waypoint> target) {
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) {
                continue;
            }
            UUID id = UUID.fromString(key);
            boolean isPublic = ownerId == null;
            UUID actualOwner = null;
            if (entry.contains("owner")) {
                actualOwner = UUID.fromString(entry.getString("owner"));
            } else if (!isPublic) {
                actualOwner = ownerId;
            }
            Material icon = Material.matchMaterial(entry.getString("icon", "LODESTONE"));
            if (icon == null || !icon.isItem()) {
                icon = Material.LODESTONE;
            }
            Waypoint waypoint = new Waypoint(
                    id,
                    actualOwner,
                    isPublic,
                    entry.getString("name", "未命名"),
                    icon,
                    entry.getString("world", "world"),
                    entry.getDouble("x"),
                    entry.getDouble("y"),
                    entry.getDouble("z"),
                    (float) entry.getDouble("yaw"),
                    (float) entry.getDouble("pitch")
            );
            target.put(id, waypoint);
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        saveSection(config.createSection("public"), publicWaypoints.values(), true);
        ConfigurationSection privateSection = config.createSection("private");
        for (Map.Entry<UUID, Map<UUID, Waypoint>> entry : privateWaypoints.entrySet()) {
            saveSection(privateSection.createSection(entry.getKey().toString()), entry.getValue().values(), false);
        }
        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("无法保存 waypoints.yml: " + exception.getMessage());
        }
    }

    private static void saveSection(ConfigurationSection section, Collection<Waypoint> waypoints, boolean isPublic) {
        for (Waypoint waypoint : waypoints) {
            ConfigurationSection entry = section.createSection(waypoint.id().toString());
            entry.set("name", waypoint.name());
            entry.set("icon", waypoint.icon().name());
            entry.set("world", waypoint.worldName());
            entry.set("x", waypoint.x());
            entry.set("y", waypoint.y());
            entry.set("z", waypoint.z());
            entry.set("yaw", waypoint.yaw());
            entry.set("pitch", waypoint.pitch());
            if (waypoint.ownerId() != null) {
                entry.set("owner", waypoint.ownerId().toString());
            }
        }
    }

    public Collection<Waypoint> getPublicWaypoints() {
        return Collections.unmodifiableCollection(publicWaypoints.values());
    }

    public Collection<Waypoint> getPrivateWaypoints(UUID ownerId) {
        Map<UUID, Waypoint> ownerWaypoints = privateWaypoints.get(ownerId);
        if (ownerWaypoints == null) {
            return List.of();
        }
        return Collections.unmodifiableCollection(ownerWaypoints.values());
    }

    public void addWaypoint(Waypoint waypoint) {
        if (waypoint.isPublic()) {
            publicWaypoints.put(waypoint.id(), waypoint);
            return;
        }
        privateWaypoints
                .computeIfAbsent(waypoint.ownerId(), ignored -> new HashMap<>())
                .put(waypoint.id(), waypoint);
    }

    public Waypoint getWaypoint(UUID id) {
        Waypoint waypoint = publicWaypoints.get(id);
        if (waypoint != null) {
            return waypoint;
        }
        for (Map<UUID, Waypoint> ownerWaypoints : privateWaypoints.values()) {
            waypoint = ownerWaypoints.get(id);
            if (waypoint != null) {
                return waypoint;
            }
        }
        return null;
    }

    public void removeWaypoint(Waypoint waypoint) {
        if (waypoint.isPublic()) {
            publicWaypoints.remove(waypoint.id());
            return;
        }
        Map<UUID, Waypoint> ownerWaypoints = privateWaypoints.get(waypoint.ownerId());
        if (ownerWaypoints != null) {
            ownerWaypoints.remove(waypoint.id());
            if (ownerWaypoints.isEmpty()) {
                privateWaypoints.remove(waypoint.ownerId());
            }
        }
    }

    public List<Waypoint> getAccessibleWaypoints(UUID playerId, WaypointTab tab) {
        return switch (tab) {
            case PUBLIC -> new ArrayList<>(getPublicWaypoints());
            case PRIVATE -> new ArrayList<>(getPrivateWaypoints(playerId));
            case CREATE -> List.of();
        };
    }
}
