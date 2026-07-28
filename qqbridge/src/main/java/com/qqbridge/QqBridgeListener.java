package com.qqbridge;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class QqBridgeListener implements Listener {

    private final QqBridgeConfig config;
    private final QqBridgeService bridgeService;

    public QqBridgeListener(QqBridgeConfig config, QqBridgeService bridgeService) {
        this.config = config;
        this.bridgeService = bridgeService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!config.qqBridgeEnabled() || !config.qqBridgeForwardJoinQuit()) {
            return;
        }

        Player player = event.getPlayer();
        bridgeService.enqueueSend(formatJoinMessage(player.getName()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        if (!config.qqBridgeEnabled() || !config.qqBridgeForwardJoinQuit()) {
            return;
        }

        Player player = event.getPlayer();
        bridgeService.enqueueSend(formatQuitMessage(player));
    }

    private static String formatJoinMessage(String playerName) {
        List<String> names = sortedOnlineNames();
        return formatPresenceMessage(playerName + "加入了游戏", names);
    }

    private static String formatQuitMessage(Player player) {
        List<String> names = sortedOnlineNamesExcluding(player);
        return formatPresenceMessage(player.getName() + "退出了游戏", names);
    }

    private static String formatPresenceMessage(String headline, List<String> names) {
        StringBuilder builder = new StringBuilder(headline);
        builder.append('\n').append("当前有").append(names.size()).append("名玩家在服务器内");
        for (String name : names) {
            builder.append('\n').append(name);
        }
        return builder.toString();
    }

    private static List<String> sortedOnlineNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static List<String> sortedOnlineNamesExcluding(Player excluded) {
        return Bukkit.getOnlinePlayers().stream()
                .filter(online -> !online.getUniqueId().equals(excluded.getUniqueId()))
                .map(Player::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
