package com.goodtpa.menu;

import com.goodtpa.deathback.DeathBackManager;
import com.goodtpa.tpa.TpaActions;
import com.goodtpa.tpa.TpaSettingsManager;
import com.goodtpa.tpa.TpaType;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import io.papermc.paper.event.player.AsyncChatEvent;

public final class TpaMenuListener implements Listener {

    private final JavaPlugin plugin;
    private final TpaMenuGuiService guiService;
    private final TpaActions tpaActions;
    private final TpaSettingsManager tpaSettingsManager;
    private final DeathBackManager deathBackManager;
    private final Map<UUID, Boolean> pendingTimeoutInput = new HashMap<>();

    public TpaMenuListener(
            JavaPlugin plugin,
            TpaMenuGuiService guiService,
            TpaActions tpaActions,
            TpaSettingsManager tpaSettingsManager,
            DeathBackManager deathBackManager
    ) {
        this.plugin = plugin;
        this.guiService = guiService;
        this.tpaActions = tpaActions;
        this.tpaSettingsManager = tpaSettingsManager;
        this.deathBackManager = deathBackManager;
    }

    public void openMain(Player player) {
        guiService.openMain(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof TpaMenuGuiHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }

        int slot = event.getRawSlot();
        if (slot == TpaMenuGuiHolder.SWITCH_UI_SLOT) {
            // Switch to Dialog UI directly
            player.closeInventory();
            guiService.openMainDialog(player);
            return;
        }
        if (slot == TpaMenuGuiHolder.BACK_SLOT) {
            if (holder.screen() == TpaMenuScreen.MAIN) {
                player.closeInventory();
            } else {
                guiService.openMain(player);
            }
            return;
        }

        if (slot == 45 && holder.page() > 0) {
            guiService.openPlayerPicker(player, holder.screen(), holder.page() - 1);
            return;
        }
        if (slot == 53) {
            guiService.openPlayerPicker(player, holder.screen(), holder.page() + 1);
            return;
        }

        UUID targetId = holder.playerAt(slot);
        if (targetId != null) {
            handlePlayerPick(player, holder.screen(), targetId);
            return;
        }

        if (holder.screen() != TpaMenuScreen.MAIN) {
            return;
        }

        switch (slot) {
            case 10 -> guiService.openPlayerPicker(player, TpaMenuScreen.PLAYER_PICK_TPA);
            case 11 -> guiService.openPlayerPicker(player, TpaMenuScreen.PLAYER_PICK_TPAHERE);
            case 12 -> guiService.openPlayerPicker(player, TpaMenuScreen.PLAYER_PICK_TPACCEPT);
            case 13 -> guiService.openPlayerPicker(player, TpaMenuScreen.PLAYER_PICK_TPADENY);
            case 14 -> openTpaTimeoutChat(player);
            case 19 -> runAndClose(player, () -> tpaActions.teleportBack(player));
            case 20 -> runAndClose(player, () -> teleportDeathBack(player));
            default -> {
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncChat(AsyncChatEvent event) {
        if (!pendingTimeoutInput.containsKey(event.getPlayer().getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> handleTimeoutInput(player, message));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pendingTimeoutInput.remove(event.getPlayer().getUniqueId());
    }

    private void handlePlayerPick(Player player, TpaMenuScreen screen, UUID targetId) {
        Player target = Bukkit.getPlayer(targetId);
        if (target == null) {
            player.sendMessage(Component.text("玩家不在线或不存在。", NamedTextColor.RED));
            guiService.openPlayerPicker(player, screen, 0);
            return;
        }

        player.closeInventory();
        switch (screen) {
            case PLAYER_PICK_TPA -> tpaActions.requestTeleport(player, target, TpaType.TO_TARGET);
            case PLAYER_PICK_TPAHERE -> tpaActions.requestTeleport(player, target, TpaType.TO_REQUESTER);
            case PLAYER_PICK_TPACCEPT -> tpaActions.acceptRequest(player, target);
            case PLAYER_PICK_TPADENY -> tpaActions.denyRequest(player, target);
            default -> {
            }
        }
    }

    private void openTpaTimeoutChat(Player player) {
        player.closeInventory();
        int seconds = tpaSettingsManager.getTimeoutSeconds(player.getUniqueId());
        int min = tpaSettingsManager.minTimeoutSeconds();
        int max = tpaSettingsManager.maxTimeoutSeconds();
        pendingTimeoutInput.put(player.getUniqueId(), true);
        player.sendMessage(Component.text("请在聊天栏输入传送失效时间（秒）。当前: ", NamedTextColor.YELLOW)
                .append(Component.text(seconds + " 秒", NamedTextColor.AQUA))
                .append(Component.text("，范围 ", NamedTextColor.YELLOW))
                .append(Component.text(min + "-" + max, NamedTextColor.GOLD))
                .append(Component.text("。输入 ", NamedTextColor.YELLOW))
                .append(Component.text("取消", NamedTextColor.RED))
                .append(Component.text(" 可放弃。", NamedTextColor.YELLOW)));
    }

    private void handleTimeoutInput(Player player, String text) {
        pendingTimeoutInput.remove(player.getUniqueId());
        if (text.equalsIgnoreCase("取消")) {
            player.sendMessage(Component.text("已取消输入。", NamedTextColor.YELLOW));
            return;
        }
        if (!player.hasPermission("goodmc.tpa")) {
            player.sendMessage(Component.text("你没有权限使用传送指令。", NamedTextColor.RED));
            return;
        }
        if (text.isBlank()) {
            int seconds = tpaSettingsManager.getTimeoutSeconds(player.getUniqueId());
            player.sendMessage(Component.text("你的传送请求失效时间为 " + seconds + " 秒。", NamedTextColor.AQUA));
            return;
        }
        int seconds;
        try {
            seconds = Integer.parseInt(text.trim());
        } catch (NumberFormatException exception) {
            player.sendMessage(Component.text("请输入有效的秒数。", NamedTextColor.RED));
            return;
        }
        if (seconds < tpaSettingsManager.minTimeoutSeconds() || seconds > tpaSettingsManager.maxTimeoutSeconds()) {
            player.sendMessage(Component.text(
                    "失效时间必须在 "
                            + tpaSettingsManager.minTimeoutSeconds()
                            + " 到 "
                            + tpaSettingsManager.maxTimeoutSeconds()
                            + " 秒之间。",
                    NamedTextColor.RED
            ));
            return;
        }
        int saved = tpaSettingsManager.setTimeoutSeconds(player, seconds);
        player.sendMessage(Component.text("已设置你的传送请求失效时间为 " + saved + " 秒。", NamedTextColor.GREEN));
    }

    private void teleportDeathBack(Player player) {
        if (!player.hasPermission("goodmc.back")) {
            player.sendMessage(Component.text("你没有权限使用该指令。", NamedTextColor.RED));
            return;
        }
        var deathLocation = deathBackManager.getDeathLocation(player.getUniqueId());
        if (deathLocation.isEmpty()) {
            player.sendMessage(Component.text("没有可返回的死亡点记录。", NamedTextColor.RED));
            return;
        }
        deathBackManager.clearDeathLocation(player.getUniqueId());
        player.teleportAsync(deathLocation.get()).thenAccept(success -> {
            if (success) {
                player.sendMessage(Component.text("已返回死亡点。", NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("传送失败，请稍后再试。", NamedTextColor.RED));
            }
        });
    }

    private void runAndClose(Player player, Runnable action) {
        player.closeInventory();
        plugin.getServer().getScheduler().runTask(plugin, action);
    }
}
