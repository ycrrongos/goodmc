package com.goodmc.listener;

import com.goodmc.GoodMCPluginConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class JoinHintListener implements Listener {

    private final GoodMCPluginConfig config;

    public JoinHintListener(GoodMCPluginConfig config) {
        this.config = config;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.getPlayer().sendMessage(
                Component.text(config.joinHintMessage(), NamedTextColor.YELLOW)
        );
    }
}
