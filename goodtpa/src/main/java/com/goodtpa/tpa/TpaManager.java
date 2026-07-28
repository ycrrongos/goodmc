package com.goodtpa.tpa;

import com.goodtpa.util.TeleportEffects;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public final class TpaManager {

    private final JavaPlugin plugin;
    private final TpaSettingsManager settingsManager;
    private final TeleportEffects teleportEffects;
    private final TpaBossBarManager bossBarManager;
    private final Map<UUID, Map<UUID, TpaRequest>> pendingByTarget = new HashMap<>();
    private final Map<UUID, Location> backLocations = new HashMap<>();

    public TpaManager(JavaPlugin plugin, TpaSettingsManager settingsManager, TeleportEffects teleportEffects) {
        this.plugin = plugin;
        this.settingsManager = settingsManager;
        this.teleportEffects = teleportEffects;
        this.bossBarManager = new TpaBossBarManager();
        startTicker();
    }

    public void sendRequest(Player requester, Player target, TpaType type) {
        removeRequestsFrom(requester.getUniqueId());

        long timeoutMillis = settingsManager.getTimeoutMillis(target.getUniqueId());
        TpaRequest request = new TpaRequest(requester.getUniqueId(), target.getUniqueId(), type, timeoutMillis);
        pendingByTarget
                .computeIfAbsent(target.getUniqueId(), ignored -> new HashMap<>())
                .put(requester.getUniqueId(), request);
        bossBarManager.startTracking(request, requester, target);
    }

    public List<UUID> getIncomingRequesterIds(UUID targetId) {
        Map<UUID, TpaRequest> requests = pendingByTarget.get(targetId);
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }

        List<UUID> requesterIds = new ArrayList<>();
        for (Map.Entry<UUID, TpaRequest> entry : requests.entrySet()) {
            TpaRequest request = entry.getValue();
            if (request.isExpired()) {
                expireRequest(targetId, entry.getKey(), request);
                continue;
            }
            requesterIds.add(entry.getKey());
        }
        return requesterIds;
    }

    public Optional<TpaRequest> getRequest(UUID targetId, UUID requesterId) {
        Map<UUID, TpaRequest> requests = pendingByTarget.get(targetId);
        if (requests == null) {
            return Optional.empty();
        }

        TpaRequest request = requests.get(requesterId);
        if (request == null) {
            return Optional.empty();
        }
        if (request.isExpired()) {
            expireRequest(targetId, requesterId, request);
            return Optional.empty();
        }
        return Optional.of(request);
    }

    public void removeRequest(UUID targetId, UUID requesterId) {
        Map<UUID, TpaRequest> requests = pendingByTarget.get(targetId);
        if (requests == null) {
            return;
        }
        requests.remove(requesterId);
        if (requests.isEmpty()) {
            pendingByTarget.remove(targetId);
        }
        bossBarManager.stopTracking(requesterId, targetId);
    }

    public void removeRequestsFrom(UUID requesterId) {
        for (Map.Entry<UUID, Map<UUID, TpaRequest>> entry : pendingByTarget.entrySet()) {
            UUID targetId = entry.getKey();
            if (entry.getValue().remove(requesterId) != null) {
                bossBarManager.stopTracking(requesterId, targetId);
            }
        }
        pendingByTarget.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public void clearPlayer(UUID playerId) {
        removeRequestsFrom(playerId);
        for (UUID targetId : new ArrayList<>(pendingByTarget.keySet())) {
            removeRequest(targetId, playerId);
        }
        bossBarManager.stopAllForPlayer(playerId);
    }

    public boolean accept(Player accepter, Player requester, TpaRequest request) {
        removeRequest(accepter.getUniqueId(), requester.getUniqueId());

        Player teleported = switch (request.type()) {
            case TO_TARGET -> requester;
            case TO_REQUESTER -> accepter;
        };
        Player destination = switch (request.type()) {
            case TO_TARGET -> accepter;
            case TO_REQUESTER -> requester;
        };

        backLocations.put(teleported.getUniqueId(), teleported.getLocation().clone());
        teleported.teleportAsync(destination.getLocation()).thenAccept(success -> {
            if (success) {
                spawnTeleportParticles(teleported);
            }
        });
        return true;
    }

    public Optional<Location> getBackLocation(UUID playerId) {
        return Optional.ofNullable(backLocations.get(playerId));
    }

    public void clearBackLocation(UUID playerId) {
        backLocations.remove(playerId);
    }

    public void spawnTeleportParticles(Player player) {
        teleportEffects.spawnTeleportParticles(player);
    }

    private void startTicker() {
        new BukkitRunnable() {
            @Override
            public void run() {
                tickPendingRequests();
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private void tickPendingRequests() {
        List<Map.Entry<UUID, UUID>> expiring = new ArrayList<>();
        for (Map.Entry<UUID, Map<UUID, TpaRequest>> targetEntry : pendingByTarget.entrySet()) {
            UUID targetId = targetEntry.getKey();
            for (Map.Entry<UUID, TpaRequest> requestEntry : targetEntry.getValue().entrySet()) {
                TpaRequest request = requestEntry.getValue();
                if (request.isExpired()) {
                    expiring.add(Map.entry(targetId, requestEntry.getKey()));
                }
            }
        }

        for (Map.Entry<UUID, UUID> entry : expiring) {
            Map<UUID, TpaRequest> requests = pendingByTarget.get(entry.getKey());
            if (requests == null) {
                continue;
            }
            TpaRequest request = requests.get(entry.getValue());
            if (request != null) {
                expireRequest(entry.getKey(), entry.getValue(), request);
            }
        }

        bossBarManager.refreshAll();
    }

    private void expireRequest(UUID targetId, UUID requesterId, TpaRequest request) {
        removeRequest(targetId, requesterId);

        Player requester = Bukkit.getPlayer(requesterId);
        Player target = Bukkit.getPlayer(targetId);
        if (requester != null) {
            String targetName = target != null ? target.getName() : "对方";
            requester.sendMessage(Component.text(
                    "向 " + targetName + " 发起的传送请求已超时失效。",
                    NamedTextColor.RED
            ));
        }
        if (target != null) {
            String requesterName = requester != null ? requester.getName() : "某玩家";
            target.sendMessage(Component.text(
                    requesterName + " 的传送请求已超时失效。",
                    NamedTextColor.YELLOW
            ));
        }
    }
}
