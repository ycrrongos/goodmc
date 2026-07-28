package com.goodtpa.waypoint;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import com.goodtpa.util.TeleportEffects;

public final class WaypointManager {

    private final JavaPlugin plugin;
    private final WaypointStorage storage;
    private final TeleportEffects teleportEffects;
    private final Map<UUID, WaypointCreateSession> createSessions = new HashMap<>();
    private final Map<UUID, WaypointPlayerState> playerStates = new HashMap<>();

    public WaypointManager(JavaPlugin plugin, TeleportEffects teleportEffects) {
        this.plugin = plugin;
        this.teleportEffects = teleportEffects;
        this.storage = new WaypointStorage(plugin);
        storage.load();
    }

    public WaypointStorage storage() {
        return storage;
    }

    public WaypointPlayerState getOrCreateState(UUID playerId) {
        return playerStates.computeIfAbsent(playerId, ignored -> new WaypointPlayerState());
    }

    public WaypointCreateSession getOrCreateSession(Player player) {
        return createSessions.computeIfAbsent(player.getUniqueId(), ignored -> new WaypointCreateSession());
    }

    public WaypointCreateSession getSession(UUID playerId) {
        return createSessions.get(playerId);
    }

    public void clearSession(UUID playerId) {
        createSessions.remove(playerId);
    }

    public void clearPlayerState(UUID playerId) {
        playerStates.remove(playerId);
    }

    public void saveWaypoint(Waypoint waypoint) {
        storage.addWaypoint(waypoint);
        storage.save();
    }

    public boolean deleteWaypoint(Player player, Waypoint waypoint) {
        if (!canDelete(player, waypoint)) {
            return false;
        }
        storage.removeWaypoint(waypoint);
        storage.save();
        return true;
    }

    public boolean canDelete(Player player, Waypoint waypoint) {
        if (waypoint.ownerId() != null && waypoint.ownerId().equals(player.getUniqueId())) {
            return true;
        }
        if (isWaypointAdmin(player) && waypoint.isPublic()) {
            return true;
        }
        return false;
    }

    public boolean isWaypointAdmin(Player player) {
        return player.isOp() || player.hasPermission("goodmc.waypoint.admin");
    }

    public String creatorName(Waypoint waypoint) {
        if (waypoint.ownerId() == null) {
            return "未知";
        }
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(waypoint.ownerId());
        String name = offlinePlayer.getName();
        return name == null ? "未知" : name;
    }

    public void teleport(Player player, Waypoint waypoint) {
        World world = Bukkit.getWorld(waypoint.worldName());
        if (world == null) {
            player.sendMessage("§c路径点所在世界不存在或尚未加载。");
            return;
        }
        getOrCreateState(player.getUniqueId()).setBackLocation(player.getLocation());
        Location location = waypoint.toLocation(world);
        player.teleportAsync(location).thenAccept(success -> {
            if (success) {
                teleportEffects.spawnTeleportParticles(player);
                player.sendMessage("§a已传送到路径点 §f" + waypoint.name() + "§a。");
            } else {
                player.sendMessage("§c传送失败，请稍后再试。");
            }
        });
    }

    public void teleportBack(Player player) {
        WaypointPlayerState state = getOrCreateState(player.getUniqueId());
        Location back = state.backLocation();
        if (back == null) {
            player.sendMessage("§c没有可返回的传送记录。");
            return;
        }
        state.clearBackLocation();
        player.teleportAsync(back).thenAccept(success -> {
            if (!player.isOnline()) {
                return;
            }
            if (success) {
                teleportEffects.spawnTeleportParticles(player);
                player.sendMessage("§a已返回传送前的位置。");
            } else {
                player.sendMessage("§c返回失败，请稍后再试。");
            }
        });
    }
}
