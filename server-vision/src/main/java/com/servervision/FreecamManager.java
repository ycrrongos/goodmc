package com.servervision;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class FreecamManager {

    private final Map<UUID, Location> returnLocations = new HashMap<>();
    private final Set<UUID> applyingChange = new HashSet<>();

    public boolean isInFreecam(UUID playerId) {
        return returnLocations.containsKey(playerId);
    }

    public boolean isApplyingChange(UUID playerId) {
        return applyingChange.contains(playerId);
    }

    public void enter(Player player) {
        applyingChange.add(player.getUniqueId());
        try {
            returnLocations.put(player.getUniqueId(), player.getLocation().clone());
            player.setGameMode(GameMode.SPECTATOR);
        } finally {
            applyingChange.remove(player.getUniqueId());
        }
    }

    public void exit(Player player) {
        applyingChange.add(player.getUniqueId());
        try {
            Location returnLocation = returnLocations.remove(player.getUniqueId());
            player.setGameMode(GameMode.SURVIVAL);
            if (returnLocation != null) {
                player.teleportAsync(returnLocation);
            }
        } finally {
            applyingChange.remove(player.getUniqueId());
        }
    }

    public void toggle(Player player) {
        if (!player.hasPermission("goodmc.freecam")) {
            player.sendMessage(Component.text("你没有权限使用自由视角。", NamedTextColor.RED));
            return;
        }

        if (isInFreecam(player.getUniqueId())) {
            exit(player);
            player.sendMessage(Component.text("已退出自由视角并返回原位置。", NamedTextColor.GREEN));
            return;
        }

        if (player.getGameMode() != GameMode.SURVIVAL) {
            player.sendMessage(Component.text("只能在生存模式下进入自由视角。", NamedTextColor.RED));
            return;
        }

        enter(player);
        player.sendMessage(Component.text("已进入自由视角，再次输入 /freecam 或按 F3+F4 返回。", NamedTextColor.GREEN));
    }

    public void restoreAllOnlinePlayers(Iterable<? extends Player> players) {
        for (Player player : players) {
            if (isInFreecam(player.getUniqueId())) {
                exit(player);
            }
        }
    }
}
