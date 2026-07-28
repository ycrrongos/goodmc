package com.servervision;

import java.util.Locale;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public final class FreecamCommandBlockListener implements Listener {

    private final FreecamManager freecamManager;
    private final Set<String> blockedCommands;

    public FreecamCommandBlockListener(FreecamManager freecamManager, Set<String> blockedCommands) {
        this.freecamManager = freecamManager;
        this.blockedCommands = Set.copyOf(blockedCommands);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!freecamManager.isInFreecam(event.getPlayer().getUniqueId())) {
            return;
        }

        String command = parseCommand(event.getMessage());
        if (!blockedCommands.contains(command)) {
            return;
        }

        event.setCancelled(true);
        event.getPlayer().sendMessage(Component.text(
                "自由视角下不能使用 /" + command + "，请先输入 /freecam 退出。",
                NamedTextColor.RED
        ));
    }

    private static String parseCommand(String message) {
        if (!message.startsWith("/")) {
            return "";
        }
        String withoutSlash = message.substring(1);
        int spaceIndex = withoutSlash.indexOf(' ');
        String command = (spaceIndex == -1 ? withoutSlash : withoutSlash.substring(0, spaceIndex))
                .toLowerCase(Locale.ROOT);
        if (command.contains(":")) {
            command = command.substring(command.indexOf(':') + 1);
        }
        return command;
    }
}
