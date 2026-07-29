package com.goodmc.listener;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Creeper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

public final class CreeperExplosionListener implements Listener {

    private static final Set<Material> DESTROYABLE_GRASS = EnumSet.of(
            Material.SHORT_GRASS,
            Material.TALL_GRASS,
            Material.FERN,
            Material.LARGE_FERN
    );

    private static final int GRASS_BLOCK_RADIUS = 4;

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreeperExplode(EntityExplodeEvent event) {
        if (!(event.getEntity() instanceof Creeper)) {
            return;
        }

        Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (!DESTROYABLE_GRASS.contains(block.getType())) {
                iterator.remove();
            }
        }

        convertGrassBlocksToDirt(event);
    }

    private static void convertGrassBlocksToDirt(EntityExplodeEvent event) {
        var center = event.getLocation();
        var world = center.getWorld();
        if (world == null) {
            return;
        }

        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();
        int radiusSq = GRASS_BLOCK_RADIUS * GRASS_BLOCK_RADIUS;

        for (int x = -GRASS_BLOCK_RADIUS; x <= GRASS_BLOCK_RADIUS; x++) {
            for (int y = -GRASS_BLOCK_RADIUS; y <= GRASS_BLOCK_RADIUS; y++) {
                for (int z = -GRASS_BLOCK_RADIUS; z <= GRASS_BLOCK_RADIUS; z++) {
                    if (x * x + y * y + z * z > radiusSq) {
                        continue;
                    }
                    Block block = world.getBlockAt(cx + x, cy + y, cz + z);
                    if (block.getType() == Material.GRASS_BLOCK) {
                        block.setType(Material.DIRT);
                    }
                }
            }
        }
    }
}
