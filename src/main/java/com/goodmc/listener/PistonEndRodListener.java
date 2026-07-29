package com.goodmc.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.util.Vector;

public final class PistonEndRodListener implements Listener {

    private static final int TRIGGER_COUNT = 10;
    private static final int TNT_FUSE_TICKS = 80;

    private final Map<String, Integer> moveCounts = new HashMap<>();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (event.getBlock().getType() != Material.STICKY_PISTON) {
            return;
        }
        Block endRod = findEndRodOnPistonHead(event.getBlock(), event.getBlocks());
        trackMove(event.getBlock(), endRod);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!event.isSticky() || event.getBlock().getType() != Material.STICKY_PISTON) {
            return;
        }
        Block endRod = findEndRodOnPistonHead(event.getBlock(), event.getBlocks());
        trackMove(event.getBlock(), endRod);
    }

    private Block findEndRodOnPistonHead(Block pistonBlock, java.util.List<Block> movedBlocks) {
        BlockFace facing = ((Directional) pistonBlock.getBlockData()).getFacing();
        Block head = pistonBlock.getRelative(facing);
        Block aboveHead = head.getRelative(BlockFace.UP);
        if (aboveHead.getType() == Material.END_ROD) {
            return aboveHead;
        }

        for (Block moved : movedBlocks) {
            if (moved.getType() == Material.END_ROD) {
                return moved;
            }
            if (moved.getRelative(BlockFace.UP).getType() == Material.END_ROD) {
                return moved.getRelative(BlockFace.UP);
            }
        }
        return null;
    }

    private void trackMove(Block pistonBlock, Block endRodBlock) {
        if (endRodBlock == null) {
            return;
        }

        String key = blockKey(pistonBlock);
        int count = moveCounts.getOrDefault(key, 0) + 1;
        if (count < TRIGGER_COUNT) {
            moveCounts.put(key, count);
            return;
        }

        moveCounts.remove(key);
        punish(endRodBlock.getLocation().add(0.5, 0.5, 0.5));
    }

    private static void punish(Location center) {
        var world = center.getWorld();
        if (world == null) {
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 10; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 1.5;
            double offsetZ = (random.nextDouble() - 0.5) * 1.5;
            Location spawn = center.clone().add(offsetX, 0.2, offsetZ);
            world.spawn(spawn, ExplosiveMinecart.class, cart -> cart.setVelocity(new Vector(0, 0.1, 0)));
        }

        world.spawn(center, TNTPrimed.class, tnt -> tnt.setFuseTicks(TNT_FUSE_TICKS));
    }

    private static String blockKey(Block block) {
        Location location = block.getLocation();
        return location.getWorld().getUID() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }
}
