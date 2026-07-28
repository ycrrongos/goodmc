package com.servermenu;

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
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;
import io.papermc.paper.event.player.AsyncChatEvent;

public final class MenuListener implements Listener {

    private final JavaPlugin plugin;
    private final MenuGuiService guiService;
    private final MenuButtonRegistry registry;
    private final Map<UUID, PendingChat> pendingChat = new HashMap<>();

    public MenuListener(JavaPlugin plugin, MenuGuiService guiService, MenuButtonRegistry registry) {
        this.plugin = plugin;
        this.guiService = guiService;
        this.registry = registry;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!MenuItems.isMenuItem(plugin, event.getItem())) {
            return;
        }
        event.setCancelled(true);
        guiService.openMain(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuGuiHolder holder)) {
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
        if (slot == MenuGuiHolder.BACK_SLOT) {
            if (holder.screen() == MenuGuiHolder.Screen.MAIN) {
                player.closeInventory();
            } else {
                guiService.openMain(player);
            }
            return;
        }

        if (holder.screen() == MenuGuiHolder.Screen.PLAYER_PICK) {
            if (slot == 45 && holder.page() > 0) {
                guiService.openPlayerPicker(player, holder.pendingButton(), holder.page() - 1);
                return;
            }
            if (slot == 53) {
                guiService.openPlayerPicker(player, holder.pendingButton(), holder.page() + 1);
                return;
            }
            UUID targetId = holder.playerAt(slot);
            if (targetId != null) {
                Player target = Bukkit.getPlayer(targetId);
                if (target == null) {
                    player.sendMessage(Component.text("玩家不在线或不存在。", NamedTextColor.RED));
                    guiService.openPlayerPicker(player, holder.pendingButton(), 0);
                    return;
                }
                continueAfterPlayerPick(player, holder.pendingButton(), target.getName());
            }
            return;
        }

        MenuButton button = findButton(slot);
        if (button == null) {
            return;
        }

        if (button.pickPlayer()) {
            guiService.openPlayerPicker(player, button, 0);
            return;
        }
        if (button.chatInput()) {
            beginChatInput(player, button, button.command());
            return;
        }
        runCommand(player, button.command());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncChat(AsyncChatEvent event) {
        PendingChat pending = pendingChat.get(event.getPlayer().getUniqueId());
        if (pending == null) {
            return;
        }
        event.setCancelled(true);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> handleChatInput(player, pending, message));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pendingChat.remove(event.getPlayer().getUniqueId());
    }

    private MenuButton findButton(int slot) {
        for (MenuButton button : registry.buttons()) {
            if (button.slot() == slot) {
                return button;
            }
        }
        return null;
    }

    private void continueAfterPlayerPick(Player player, MenuButton button, String playerName) {
        String command = button.command().replace("{player}", playerName);
        if (button.chatInput()) {
            beginChatInput(player, button, command);
            return;
        }
        runCommand(player, command);
    }

    private void beginChatInput(Player player, MenuButton button, String commandTemplate) {
        player.closeInventory();
        pendingChat.put(player.getUniqueId(), new PendingChat(commandTemplate));
        player.sendMessage(Component.text(button.prompt(), NamedTextColor.YELLOW));
    }

    private void handleChatInput(Player player, PendingChat pending, String message) {
        pendingChat.remove(player.getUniqueId());
        if (message.equalsIgnoreCase("取消")) {
            player.sendMessage(Component.text("已取消输入。", NamedTextColor.YELLOW));
            return;
        }
        if (message.isBlank()) {
            player.sendMessage(Component.text("输入不能为空。", NamedTextColor.RED));
            return;
        }
        runCommand(player, pending.commandTemplate().replace("{input}", message));
    }

    private void runCommand(Player player, String commandLine) {
        player.closeInventory();
        String command = commandLine.startsWith("/") ? commandLine.substring(1) : commandLine;
        plugin.getServer().getScheduler().runTask(plugin, () -> player.performCommand(command));
    }

    private record PendingChat(String commandTemplate) {
    }
}
