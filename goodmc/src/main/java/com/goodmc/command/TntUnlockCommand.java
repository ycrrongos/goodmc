package com.goodmc.command;

import com.goodmc.listener.TntThrowListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class TntUnlockCommand implements CommandExecutor {

    private final TntThrowListener listener;

    public TntUnlockCommand(TntThrowListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("只有玩家可以使用此命令。", NamedTextColor.RED));
            return true;
        }
        listener.unlock(player);
        return true;
    }
}
