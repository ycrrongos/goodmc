package com.servervision;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

public final class FullBrightCommand implements CommandExecutor, Listener {

    private final Set<UUID> activePlayers = new HashSet<>();

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("该指令只能由玩家使用。", NamedTextColor.RED));
            return true;
        }

        if (!player.hasPermission("goodmc.fullbright")) {
            player.sendMessage(Component.text("你没有权限使用该指令。", NamedTextColor.RED));
            return true;
        }

        if (args.length != 0) {
            player.sendMessage(Component.text("用法: /fullbright", NamedTextColor.RED));
            return true;
        }

        UUID playerId = player.getUniqueId();
        if (activePlayers.remove(playerId)) {
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            player.sendMessage(Component.text("已关闭夜视效果。", NamedTextColor.YELLOW));
            return true;
        }

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.NIGHT_VISION,
                PotionEffect.INFINITE_DURATION,
                0,
                false,
                false,
                true
        ));
        activePlayers.add(playerId);
        player.sendMessage(Component.text("已开启无限夜视，再次输入 /fullbright 可关闭。", NamedTextColor.GREEN));
        return true;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        activePlayers.remove(event.getPlayer().getUniqueId());
    }
}
