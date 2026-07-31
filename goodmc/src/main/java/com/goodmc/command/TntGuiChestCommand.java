package com.goodmc.command;

import com.goodmc.listener.TntThrowListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class TntGuiChestCommand implements CommandExecutor {

    private final TntThrowListener tntListener;

    public TntGuiChestCommand(TntThrowListener tntListener) {
        this.tntListener = tntListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("此命令仅限玩家使用。");
            return true;
        }
        tntListener.openTrajectoryChest(player);
        return true;
    }
}
