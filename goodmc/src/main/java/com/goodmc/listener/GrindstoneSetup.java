package com.goodmc.listener;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.BlockInventoryHolder;

final class GrindstoneSetup {

    private static final BlockFace[] ADJACENT_FACES = {
            BlockFace.NORTH,
            BlockFace.SOUTH,
            BlockFace.EAST,
            BlockFace.WEST,
            BlockFace.UP,
            BlockFace.DOWN
    };

    private GrindstoneSetup() {
    }

    static Block grindstoneBlock(Object holder) {
        if (!(holder instanceof BlockInventoryHolder blockHolder)) {
            return null;
        }
        Block block = blockHolder.getBlock();
        if (block.getType() != Material.GRINDSTONE) {
            return null;
        }
        return block;
    }

    /** 砂轮放在铁砧正上方：附魔提取 */
    static boolean isExtractor(Object holder) {
        Block block = grindstoneBlock(holder);
        if (block == null) {
            return false;
        }
        return isAnvil(block.getRelative(0, -1, 0).getType());
    }

    /** 两个砂轮紧挨：去除负魔（诅咒） */
    static boolean isCurseRemover(Object holder) {
        Block block = grindstoneBlock(holder);
        if (block == null || isExtractor(holder)) {
            return false;
        }
        for (BlockFace face : ADJACENT_FACES) {
            if (block.getRelative(face).getType() == Material.GRINDSTONE) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAnvil(Material material) {
        return material == Material.ANVIL
                || material == Material.CHIPPED_ANVIL
                || material == Material.DAMAGED_ANVIL;
    }
}
