package com.goodmc.command;

import com.goodmc.listener.FireChargeListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class FireballUnlockCommand implements CommandExecutor {

    private final FireChargeListener fireChargeListener;

    public FireballUnlockCommand(FireChargeListener fireChargeListener) {
        this.fireChargeListener = fireChargeListener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("此命令仅限玩家使用。");
            return true;
        }
        fireChargeListener.unlock(player);
        return true;
    }
}
