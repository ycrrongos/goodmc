package com.goodtpa.tpa;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class TpaActions {

    private final TpaManager tpaManager;

    public TpaActions(TpaManager tpaManager) {
        this.tpaManager = tpaManager;
    }

    public boolean requestTeleport(Player requester, Player target, TpaType type) {
        if (!requester.hasPermission("goodmc.tpa")) {
            requester.sendMessage(Component.text("你没有权限使用传送指令。", NamedTextColor.RED));
            return false;
        }
        if (target.equals(requester)) {
            requester.sendMessage(Component.text("不能对自己使用传送请求。", NamedTextColor.RED));
            return false;
        }

        tpaManager.sendRequest(requester, target, type);
        sendRequestMessages(requester, target, type);
        return true;
    }

    public boolean acceptRequest(Player accepter, Player requester) {
        if (!accepter.hasPermission("goodmc.tpa")) {
            accepter.sendMessage(Component.text("你没有权限使用传送指令。", NamedTextColor.RED));
            return false;
        }

        var requestOpt = tpaManager.getRequest(accepter.getUniqueId(), requester.getUniqueId());
        if (requestOpt.isEmpty()) {
            accepter.sendMessage(Component.text("没有来自该玩家的传送请求。", NamedTextColor.RED));
            return false;
        }

        tpaManager.accept(accepter, requester, requestOpt.get());

        String teleportedName = requestOpt.get().type() == TpaType.TO_TARGET
                ? requester.getName()
                : accepter.getName();
        accepter.sendMessage(
                Component.text("已接受 ", NamedTextColor.GREEN)
                        .append(Component.text(requester.getName(), NamedTextColor.AQUA))
                        .append(Component.text(" 的传送请求。", NamedTextColor.GREEN))
        );
        requester.sendMessage(
                Component.text(teleportedName, NamedTextColor.AQUA)
                        .append(Component.text(" 已传送。", NamedTextColor.GREEN))
        );
        return true;
    }

    public boolean denyRequest(Player denier, Player requester) {
        if (!denier.hasPermission("goodmc.tpa")) {
            denier.sendMessage(Component.text("你没有权限使用传送指令。", NamedTextColor.RED));
            return false;
        }

        var requestOpt = tpaManager.getRequest(denier.getUniqueId(), requester.getUniqueId());
        if (requestOpt.isEmpty()) {
            denier.sendMessage(Component.text("没有来自该玩家的传送请求。", NamedTextColor.RED));
            return false;
        }

        tpaManager.removeRequest(denier.getUniqueId(), requester.getUniqueId());
        denier.sendMessage(
                Component.text("已拒绝 ", NamedTextColor.YELLOW)
                        .append(Component.text(requester.getName(), NamedTextColor.AQUA))
                        .append(Component.text(" 的传送请求。", NamedTextColor.YELLOW))
        );
        requester.sendMessage(
                Component.text(denier.getName(), NamedTextColor.AQUA)
                        .append(Component.text(" 拒绝了你的传送请求。", NamedTextColor.RED))
        );
        return true;
    }

    public boolean teleportBack(Player player) {
        if (!player.hasPermission("goodmc.tpa")) {
            player.sendMessage(Component.text("你没有权限使用传送指令。", NamedTextColor.RED));
            return false;
        }

        var backOpt = tpaManager.getBackLocation(player.getUniqueId());
        if (backOpt.isEmpty()) {
            player.sendMessage(Component.text("没有可返回的传送记录。", NamedTextColor.RED));
            return false;
        }

        var back = backOpt.get();
        tpaManager.clearBackLocation(player.getUniqueId());
        player.teleportAsync(back).thenAccept(success -> {
            if (success) {
                tpaManager.spawnTeleportParticles(player);
                player.sendMessage(Component.text("已返回传送前的位置。", NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("返回失败，请稍后再试。", NamedTextColor.RED));
            }
        });
        return true;
    }

    private static void sendRequestMessages(Player requester, Player target, TpaType type) {
        String requesterName = requester.getName();
        String actionText = type == TpaType.TO_TARGET
                ? requesterName + " 请求传送到你这里"
                : requesterName + " 请求你传送到他那里";

        Component acceptButton = Component.text("[接受]")
                .color(NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/tpaccept " + requesterName))
                .hoverEvent(HoverEvent.showText(Component.text("点击接受传送请求", NamedTextColor.GREEN)));

        Component denyButton = Component.text("[拒绝]")
                .color(NamedTextColor.RED)
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/tpadeny " + requesterName))
                .hoverEvent(HoverEvent.showText(Component.text("点击拒绝传送请求", NamedTextColor.RED)));

        target.sendMessage(
                Component.text(actionText + "。", NamedTextColor.YELLOW)
                        .append(Component.text(" "))
                        .append(acceptButton)
                        .append(Component.text(" "))
                        .append(denyButton)
        );

        requester.sendMessage(
                Component.text("已向 ", NamedTextColor.GREEN)
                        .append(Component.text(target.getName(), NamedTextColor.AQUA))
                        .append(Component.text(" 发送传送请求，等待对方回应...", NamedTextColor.GREEN))
        );
    }
}
