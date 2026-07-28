package com.goodmc.command;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class KillCommand implements CommandExecutor, Listener {

    private static final String DEATH_MESSAGE = "被[刻意的服务器设计]杀死了";
    private static final String BYPASS_PERMISSION = "goodmc.bypass";

    private final Set<UUID> pendingSelfKill = ConcurrentHashMap.newKeySet();

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

        if (isInFreecam(player)) {
            player.sendMessage(Component.text(
                    "自由视角下不能使用 /kill，请先输入 /freecam 退出。",
                    NamedTextColor.RED
            ));
            return true;
        }

        if (isSelfKill(player.getName(), args)) {
            performSelfKill(player);
            return true;
        }

        if (player.hasPermission(BYPASS_PERMISSION)) {
            dispatchVanillaKill(player, args);
            return true;
        }

        if (player.isOp()) {
            Plugin adminVote = Bukkit.getPluginManager().getPlugin("AdminVote");
            if (adminVote == null || !adminVote.isEnabled()) {
                dispatchVanillaKill(player, args);
                return true;
            }
            if (isVoteExecutingApproved(player.getUniqueId())) {
                dispatchVanillaKill(player, args);
                return true;
            }
            player.sendMessage(Component.text("击杀其他玩家需要全体玩家投票同意。", NamedTextColor.RED));
            return true;
        }

        player.sendMessage(Component.text("你只能自杀，不能击杀其他玩家。", NamedTextColor.RED));
        return true;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent event) {
        if (!pendingSelfKill.remove(event.getEntity().getUniqueId())) {
            return;
        }
        Player player = event.getEntity();
        event.deathMessage(Component.text(player.getName() + " " + DEATH_MESSAGE));
    }

    private void performSelfKill(Player player) {
        pendingSelfKill.add(player.getUniqueId());
        player.setHealth(0.0);
    }

    private static void dispatchVanillaKill(Player player, String[] args) {
        StringBuilder commandLine = new StringBuilder("minecraft:kill");
        for (String arg : args) {
            commandLine.append(' ').append(arg);
        }
        Bukkit.dispatchCommand(player, commandLine.toString());
    }

    private static boolean isSelfKill(String playerName, String[] args) {
        if (args.length == 0) {
            return true;
        }
        if (args.length == 1) {
            String target = args[0].toLowerCase(Locale.ROOT);
            return target.equals("@s")
                    || target.equalsIgnoreCase(playerName)
                    || target.equals("@p");
        }
        return false;
    }

    private static boolean isInFreecam(Player player) {
        Plugin vision = Bukkit.getPluginManager().getPlugin("Server-Vision");
        if (vision == null || !vision.isEnabled()) {
            return false;
        }
        try {
            Method managerMethod = vision.getClass().getMethod("freecamManager");
            Object manager = managerMethod.invoke(vision);
            if (manager == null) {
                return false;
            }
            Method check = manager.getClass().getMethod("isInFreecam", UUID.class);
            Object result = check.invoke(manager, player.getUniqueId());
            return result instanceof Boolean bool && bool;
        } catch (ReflectiveOperationException exception) {
            Bukkit.getLogger().log(Level.FINE, "Server-Vision freecam check failed", exception);
            return false;
        }
    }

    private static boolean isVoteExecutingApproved(UUID playerId) {
        Plugin adminVote = Bukkit.getPluginManager().getPlugin("AdminVote");
        if (adminVote == null || !adminVote.isEnabled()) {
            return false;
        }
        try {
            Method managerMethod = adminVote.getClass().getMethod("voteManager");
            Object manager = managerMethod.invoke(adminVote);
            if (manager == null) {
                return false;
            }
            Method check = manager.getClass().getMethod("isExecutingApproved", UUID.class);
            Object result = check.invoke(manager, playerId);
            return result instanceof Boolean bool && bool;
        } catch (ReflectiveOperationException exception) {
            Bukkit.getLogger().log(Level.FINE, "AdminVote check failed", exception);
            return false;
        }
    }
}
