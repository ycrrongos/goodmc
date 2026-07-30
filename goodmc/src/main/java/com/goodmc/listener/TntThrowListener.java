package com.goodmc.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TntThrowListener implements Listener {

    private static final int FUSE_TICKS = 120; // 6 seconds
    private static final long DROP_DEBOUNCE_MS = 150;

    public enum Trajectory {
        ENDER_PEARL("末影珍珠", org.bukkit.entity.EnderPearl.class),
        BOW("弓", org.bukkit.entity.AbstractArrow.class),
        CROSSBOW("弩", org.bukkit.entity.AbstractArrow.class),
        SNOWBALL("雪球", org.bukkit.entity.Snowball.class);

        private final String displayName;
        private final Class<? extends Projectile> projectileClass;

        Trajectory(String displayName, Class<? extends Projectile> projectileClass) {
            this.displayName = displayName;
            this.projectileClass = projectileClass;
        }

        public String getDisplayName() { return displayName; }
        public Class<? extends Projectile> getProjectileClass() { return projectileClass; }
    }

    private final Plugin plugin;
    private final Map<UUID, Boolean> unlocked = new ConcurrentHashMap<>();
    private final Map<UUID, Trajectory> trajectories = new ConcurrentHashMap<>();
    private final Map<UUID, Long> dropTimes = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastLockMessage = new ConcurrentHashMap<>();
    private TntTrajectoryGui trajectoryGui;

    public TntThrowListener(Plugin plugin) {
        this.plugin = plugin;
    }

    public void setTrajectoryGui(TntTrajectoryGui gui) {
        this.trajectoryGui = gui;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack mainHand = player.getInventory().getItem(EquipmentSlot.HAND);
        if (mainHand != null && mainHand.getType() == Material.TNT) {
            dropTimes.put(player.getUniqueId(), System.currentTimeMillis());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSlotChange(PlayerItemHeldEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        ItemStack newItem = event.getPlayer().getInventory().getItem(event.getNewSlot());
        if (newItem == null || newItem.getType() != Material.TNT) {
            unlocked.remove(id);
            trajectories.remove(id);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onTntInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_AIR) return;

        Player player = event.getPlayer();
        EquipmentSlot hand = event.getHand();
        if (hand == null || hand != EquipmentSlot.HAND) return;

        ItemStack item = player.getInventory().getItem(hand);
        if (item == null || item.getType() != Material.TNT) return;

        UUID id = player.getUniqueId();

        // 检测是否刚按过 Q（150ms 内），防止 Q 键触发抛掷
        Long dropTime = dropTimes.get(id);
        if (dropTime != null && System.currentTimeMillis() - dropTime < DROP_DEBOUNCE_MS) {
            event.setCancelled(true);
            return;
        }

        // 清理切换物品后的状态
        if (item.getAmount() <= 0) {
            unlocked.remove(id);
            trajectories.remove(id);
            return;
        }

        if (!unlocked.containsKey(id)) {
            event.setCancelled(true);
            sendLockPrompt(player);
            return;
        }

        event.setCancelled(true);
        Trajectory traj = trajectories.getOrDefault(id, Trajectory.ENDER_PEARL);
        Vector velocity = sampleVelocity(player, traj);
        var eye = player.getEyeLocation();

        player.getWorld().spawn(eye, TNTPrimed.class, tnt -> {
            tnt.setFuseTicks(FUSE_TICKS);
            tnt.setSource(player);
            tnt.setVelocity(velocity);
        });

        consumeOne(player, hand, item);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onRightClickPrimedTnt(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof TNTPrimed tnt)) return;

        Player player = event.getPlayer();
        event.setCancelled(true);

        tnt.remove();
        tnt.getWorld().dropItemNaturally(tnt.getLocation(), new ItemStack(Material.TNT));

        player.sendActionBar(Component.text("TNT 已回收为掉落物")
                .color(NamedTextColor.GREEN));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        unlocked.remove(id);
        trajectories.remove(id);
        dropTimes.remove(id);
    }

    private void sendLockPrompt(Player player) {
        UUID id = player.getUniqueId();

        // Debounce: only send lock message once per 3 seconds
        Long lastMsg = lastLockMessage.get(id);
        if (lastMsg != null && System.currentTimeMillis() - lastMsg < 3000) {
            return;
        }
        lastLockMessage.put(id, System.currentTimeMillis());

        Component lockLabel = Component.text("🔒 TNT丢出保护锁")
                .color(NamedTextColor.RED)
                .decoration(TextDecoration.BOLD, true);

        Component unlockBtn = Component.text(" [点击解锁] ")
                .color(NamedTextColor.GREEN)
                .decoration(TextDecoration.BOLD, true)
                .clickEvent(ClickEvent.runCommand("/tnt_unlock"))
                .hoverEvent(HoverEvent.showText(Component.text("点击解锁以启用左键丢出TNT")
                        .color(NamedTextColor.GRAY)));

        Component guiBtn = Component.text(" [抛物线选择] ")
                .color(NamedTextColor.AQUA)
                .decoration(TextDecoration.BOLD, true)
                .clickEvent(ClickEvent.runCommand("/tnt_gui"))
                .hoverEvent(HoverEvent.showText(Component.text("点击打开抛物线选择界面")
                        .color(NamedTextColor.GRAY)));

        Component line1 = lockLabel.append(unlockBtn).append(guiBtn);

        player.sendMessage(line1);
    }

    public void unlock(Player player) {
        unlocked.put(player.getUniqueId(), true);
        player.sendMessage(Component.text("✅ TNT丢出保护已解锁，左键可丢出TNT")
                .color(NamedTextColor.GREEN));
    }

    public void setTrajectory(Player player, Trajectory trajectory) {
        trajectories.put(player.getUniqueId(), trajectory);
        player.sendMessage(Component.text("抛物线已切换为: ")
                .color(NamedTextColor.GRAY)
                .append(Component.text(trajectory.getDisplayName())
                        .color(NamedTextColor.YELLOW)));
    }

    public void openTrajectoryGui(Player player) {
        if (trajectoryGui != null) {
            trajectoryGui.openTrajectoryGui(player);
        }
    }

    private static Vector sampleVelocity(Player player, Trajectory trajectory) {
        Projectile proj = player.launchProjectile(trajectory.getProjectileClass());
        Vector velocity = proj.getVelocity().clone();
        proj.remove();
        return velocity;
    }

    private static void consumeOne(Player player, EquipmentSlot hand, ItemStack item) {
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
            return;
        }
        player.getInventory().setItem(hand, null);
    }
}
