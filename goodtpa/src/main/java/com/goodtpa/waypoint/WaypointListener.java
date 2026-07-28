package com.goodtpa.waypoint;

import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.UUID;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public final class WaypointListener implements Listener {

    private final JavaPlugin plugin;
    private final WaypointManager waypointManager;
    private final WaypointGuiService guiService;

    public WaypointListener(
            JavaPlugin plugin,
            WaypointManager waypointManager,
            WaypointGuiService guiService
    ) {
        this.plugin = plugin;
        this.waypointManager = waypointManager;
        this.guiService = guiService;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof WaypointGuiHolder holder)) {
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
        if (slot == WaypointGuiHolder.TAB_PUBLIC) {
            guiService.open(player, WaypointTab.PUBLIC);
            return;
        }
        if (slot == WaypointGuiHolder.TAB_CREATE) {
            guiService.open(player, WaypointTab.CREATE);
            return;
        }
        if (slot == WaypointGuiHolder.TAB_PRIVATE) {
            guiService.open(player, WaypointTab.PRIVATE);
            return;
        }
        if (slot == WaypointGuiHolder.TAB_DELETE_MODE) {
            WaypointPlayerState state = waypointManager.getOrCreateState(player.getUniqueId());
            state.setDeleteMode(!state.deleteMode());
            if (state.deleteMode()) {
                player.sendMessage("§c已进入删除模式，点击路径点可删除。按 ESC 退出界面可关闭删除模式。");
            } else {
                player.sendMessage("§a已退出删除模式。");
            }
            guiService.open(player, holder.tab(), holder.page());
            return;
        }

        if (slot == WaypointGuiHolder.BACK_BUTTON) {
            player.closeInventory();
            waypointManager.teleportBack(player);
            return;
        }

        if (holder.tab() == WaypointTab.CREATE) {
            handleCreateClick(player, slot);
            return;
        }

        if (slot == WaypointGuiHolder.PAGE_PREVIOUS) {
            guiService.open(player, holder.tab(), holder.page() - 1);
            return;
        }
        if (slot == WaypointGuiHolder.PAGE_NEXT) {
            guiService.open(player, holder.tab(), holder.page() + 1);
            return;
        }

        UUID waypointId = holder.waypointAt(slot);
        if (waypointId == null) {
            return;
        }
        Waypoint waypoint = waypointManager.storage().getWaypoint(waypointId);
        if (waypoint == null) {
            player.sendMessage("§c该路径点已不存在。");
            guiService.open(player, holder.tab(), holder.page());
            return;
        }
        if (!waypoint.isPublic() && (waypoint.ownerId() == null || !waypoint.ownerId().equals(player.getUniqueId()))) {
            player.sendMessage("§c你没有权限使用该路径点。");
            return;
        }

        WaypointPlayerState state = waypointManager.getOrCreateState(player.getUniqueId());
        if (state.deleteMode()) {
            if (!waypointManager.deleteWaypoint(player, waypoint)) {
                player.sendMessage("§c你没有权限删除该路径点。");
                return;
            }
            player.sendMessage("§a已删除路径点 §f" + waypoint.name() + "§a。");
            guiService.open(player, holder.tab(), holder.page());
            return;
        }

        player.closeInventory();
        waypointManager.teleport(player, waypoint);
    }

    private void handleCreateClick(Player player, int slot) {
        WaypointCreateSession session = waypointManager.getOrCreateSession(player);
        session.setLocation(player.getLocation());

        if (slot == WaypointGuiHolder.CREATE_NAME) {
            session.beginNameInput();
            player.closeInventory();
            player.sendMessage("§e请在聊天栏输入路径点名称。输入 §c取消 §e可放弃。");
            return;
        }
        if (slot == WaypointGuiHolder.CREATE_ICON) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand.getType() != Material.AIR && hand.getType().isItem()) {
                session.setIcon(hand.getType());
                player.sendMessage("§a已使用主手物品作为路径点图标。");
            } else {
                session.setIcon(guiService.nextIcon(session.icon()));
                player.sendMessage("§a已切换路径点图标。");
            }
            guiService.open(player, WaypointTab.CREATE);
            return;
        }
        if (slot == WaypointGuiHolder.CREATE_VISIBILITY) {
            session.setPublic(!session.isPublic());
            guiService.open(player, WaypointTab.CREATE);
            return;
        }
        if (slot == WaypointGuiHolder.CREATE_CONFIRM) {
            if (!session.isReady()) {
                player.sendMessage("§c请先设置路径点名称。");
                return;
            }
            Waypoint waypoint = session.toWaypoint(player.getUniqueId());
            waypointManager.saveWaypoint(waypoint);
            waypointManager.clearSession(player.getUniqueId());
            player.sendMessage("§a路径点 §f" + waypoint.name() + " §a创建成功！");
            guiService.open(player, waypoint.isPublic() ? WaypointTab.PUBLIC : WaypointTab.PRIVATE);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof WaypointGuiHolder)) {
            return;
        }
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof WaypointGuiHolder) {
                return;
            }
            WaypointPlayerState state = waypointManager.getOrCreateState(player.getUniqueId());
            if (state.deleteMode()) {
                state.setDeleteMode(false);
                player.sendMessage("§a已退出删除模式。");
            }
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        WaypointCreateSession session = waypointManager.getSession(player.getUniqueId());
        if (session == null || !session.isAwaitingName()) {
            return;
        }

        event.setCancelled(true);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        plugin.getServer().getScheduler().runTask(plugin, () -> handleNameInput(player, session, message));
    }

    private void handleNameInput(Player player, WaypointCreateSession session, String message) {
        if (message.equalsIgnoreCase("取消")) {
            session.cancelNameInput();
            player.sendMessage("§e已取消命名。");
            guiService.open(player, WaypointTab.CREATE);
            return;
        }
        if (message.isBlank() || message.length() > 32) {
            player.sendMessage("§c名称长度需在 1 到 32 个字符之间。");
            return;
        }
        session.setName(message);
        player.sendMessage("§a路径点名称已设置为 §f" + message + "§a。");
        guiService.open(player, WaypointTab.CREATE);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        waypointManager.clearSession(playerId);
        waypointManager.clearPlayerState(playerId);
    }
}
