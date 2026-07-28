package com.qqbridge;

import java.util.Arrays;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class QqCommand implements CommandExecutor {

    private final QqBridgeConfig config;
    private final QqBridgeService bridgeService;

    public QqCommand(QqBridgeConfig config, QqBridgeService bridgeService) {
        this.config = config;
        this.bridgeService = bridgeService;
    }

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

        if (!player.hasPermission("goodmc.qq")) {
            player.sendMessage(Component.text("你没有权限使用该指令。", NamedTextColor.RED));
            return true;
        }

        if (!config.qqBridgeEnabled()) {
            player.sendMessage(Component.text("QQ 群服互联未启用。", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("用法: /qq <消息>", NamedTextColor.RED));
            return true;
        }

        String message = String.join(" ", Arrays.copyOfRange(args, 0, args.length)).trim();
        if (message.isEmpty()) {
            player.sendMessage(Component.text("消息不能为空。", NamedTextColor.RED));
            return true;
        }

        bridgeService.enqueueSend("<" + player.getName() + ">  " + message);
        player.sendMessage(Component.text("已发送到 QQ 群。", NamedTextColor.GREEN));
        return true;
    }
}
