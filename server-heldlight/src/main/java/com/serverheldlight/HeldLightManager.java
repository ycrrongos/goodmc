package com.serverheldlight;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Light;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class HeldLightManager implements Listener {

    private static final int MAX_ANCHORS_PER_PLAYER = 2;
    private static final int SEARCH_RADIUS = 1;

    private final JavaPlugin plugin;
    private final Map<UUID, Set<BlockKey>> playerAnchors = new HashMap<>();
    private final Map<BlockKey, SharedLight> sharedLights = new HashMap<>();
    private BukkitTask updateTask;

    public HeldLightManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        updateTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::updateAllPlayers, 1L, 1L);
    }

    public void stop() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        for (UUID playerId : new HashSet<>(playerAnchors.keySet())) {
            clearPlayer(playerId);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clearPlayer(event.getPlayer().getUniqueId());
    }

    private void updateAllPlayers() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            updatePlayer(player);
        }
    }

    private void updatePlayer(Player player) {
        UUID playerId = player.getUniqueId();

        if (!player.hasPermission("goodmc.heldlight") || player.getGameMode() == GameMode.SPECTATOR) {
            clearPlayer(playerId);
            return;
        }

        int lightLevel = getHeldLightLevel(player);
        if (lightLevel <= 0) {
            clearPlayer(playerId);
            return;
        }

        Set<BlockKey> desiredAnchors = findAnchorBlocks(player).stream()
                .map(BlockKey::from)
                .collect(HashSet::new, Set::add, Set::addAll);
        Set<BlockKey> currentAnchors = playerAnchors.getOrDefault(playerId, Set.of());

        Set<BlockKey> toAdd = new HashSet<>(desiredAnchors);
        toAdd.removeAll(currentAnchors);

        Set<BlockKey> toRemove = new HashSet<>(currentAnchors);
        toRemove.removeAll(desiredAnchors);

        for (BlockKey anchor : toAdd) {
            Block block = anchor.toBlock();
            if (block != null) {
                acquireAnchor(playerId, anchor, lightLevel, block);
            }
        }

        for (BlockKey anchor : desiredAnchors) {
            if (!toAdd.contains(anchor)) {
                setPlayerLightLevel(playerId, anchor, lightLevel);
            }
        }

        for (BlockKey anchor : toRemove) {
            releaseAnchor(playerId, anchor);
        }

        if (desiredAnchors.isEmpty()) {
            playerAnchors.remove(playerId);
        } else {
            playerAnchors.put(playerId, desiredAnchors);
        }
    }

    private void clearPlayer(UUID playerId) {
        Set<BlockKey> anchors = playerAnchors.remove(playerId);
        if (anchors == null) {
            return;
        }
        for (BlockKey anchor : anchors) {
            releaseAnchor(playerId, anchor);
        }
    }

    private void acquireAnchor(UUID playerId, BlockKey anchor, int lightLevel, Block block) {
        SharedLight shared = sharedLights.computeIfAbsent(anchor, ignored -> {
            BlockData original = block.getBlockData().clone();
            return new SharedLight(original);
        });

        shared.players.put(playerId, lightLevel);
        applyLightLevel(block, shared.getMaxLightLevel());
    }

    private void setPlayerLightLevel(UUID playerId, BlockKey anchor, int lightLevel) {
        SharedLight shared = sharedLights.get(anchor);
        if (shared == null) {
            return;
        }

        Integer previous = shared.players.put(playerId, lightLevel);
        if (previous != null && previous == lightLevel) {
            return;
        }

        Block block = anchor.toBlock();
        if (block == null) {
            return;
        }
        applyLightLevel(block, shared.getMaxLightLevel());
    }

    private void releaseAnchor(UUID playerId, BlockKey anchor) {
        SharedLight shared = sharedLights.get(anchor);
        if (shared == null) {
            return;
        }

        shared.players.remove(playerId);
        if (!shared.players.isEmpty()) {
            Block block = anchor.toBlock();
            if (block != null) {
                applyLightLevel(block, shared.getMaxLightLevel());
            }
            return;
        }

        Block block = anchor.toBlock();
        if (block != null) {
            block.setBlockData(shared.originalData, true);
        }
        sharedLights.remove(anchor);
    }

    private static void applyLightLevel(Block block, int lightLevel) {
        Light lightData = (Light) Bukkit.createBlockData(Material.LIGHT);
        lightData.setLevel(lightLevel);
        block.setBlockData(lightData, true);
    }

    private static int getHeldLightLevel(Player player) {
        int level = getItemLightLevel(player.getInventory().getItemInMainHand());
        level = Math.max(level, getItemLightLevel(player.getInventory().getItemInOffHand()));
        return level;
    }

    private static int getItemLightLevel(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.getType().isBlock()) {
            return 0;
        }
        return Bukkit.createBlockData(item.getType()).getLightEmission();
    }

    private static List<Block> findAnchorBlocks(Player player) {
        Location center = player.getEyeLocation().subtract(0, 0.2, 0);
        if (!center.getChunk().isLoaded()) {
            return List.of();
        }

        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();
        var world = center.getWorld();
        if (world == null) {
            return List.of();
        }

        List<AnchorCandidate> candidates = new ArrayList<>();
        for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
            for (int dy = -1; dy <= SEARCH_RADIUS + 1; dy++) {
                for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                    Block block = world.getBlockAt(centerX + dx, centerY + dy, centerZ + dz);
                    if (!canReplaceWithLight(block)) {
                        continue;
                    }

                    Location blockCenter = block.getLocation().add(0.5, 0.5, 0.5);
                    double distance = blockCenter.distanceSquared(center);
                    double heightBias = block.getY() >= centerY ? -0.15 : 0.05;
                    candidates.add(new AnchorCandidate(block, distance + heightBias));
                }
            }
        }

        candidates.sort(Comparator.comparingDouble(AnchorCandidate::score));

        Set<BlockKey> seen = new HashSet<>();
        List<Block> anchors = new ArrayList<>();
        for (AnchorCandidate candidate : candidates) {
            BlockKey key = BlockKey.from(candidate.block());
            if (!seen.add(key)) {
                continue;
            }
            anchors.add(candidate.block());
            if (anchors.size() >= MAX_ANCHORS_PER_PLAYER) {
                break;
            }
        }
        return anchors;
    }

    private static boolean canReplaceWithLight(Block block) {
        Material type = block.getType();
        if (type == Material.LIGHT) {
            return true;
        }
        if (!type.isAir()) {
            return false;
        }
        if (block.isLiquid()) {
            return false;
        }
        return true;
    }

    private record AnchorCandidate(Block block, double score) {
    }

    private static final class SharedLight {
        private final BlockData originalData;
        private final Map<UUID, Integer> players = new HashMap<>();

        private SharedLight(BlockData originalData) {
            this.originalData = originalData;
        }

        private int getMaxLightLevel() {
            return players.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        }
    }

    private record BlockKey(String world, int x, int y, int z) {
        private static BlockKey from(Block block) {
            Location location = block.getLocation();
            return new BlockKey(
                    Objects.requireNonNull(location.getWorld()).getUID().toString(),
                    location.getBlockX(),
                    location.getBlockY(),
                    location.getBlockZ()
            );
        }

        private Block toBlock() {
            org.bukkit.World bukkitWorld = Bukkit.getWorld(java.util.UUID.fromString(world));
            if (bukkitWorld == null || !bukkitWorld.isChunkLoaded(x >> 4, z >> 4)) {
                return null;
            }
            return bukkitWorld.getBlockAt(x, y, z);
        }
    }
}
