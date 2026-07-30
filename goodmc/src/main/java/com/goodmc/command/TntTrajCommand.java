package com.goodmc.command;

import com.goodmc.listener.TntThrowListener;
import com.goodmc.listener.TntThrowListener.Trajectory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class TntTrajCommand implements CommandExecutor {

    private final TntThrowListener listener;

    public TntTrajCommand(TntThrowListener listener) {
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
        if (args.length < 1) {
            player.sendMessage(Component.text("用法: /tnt_traj <轨迹类型>", NamedTextColor.RED));
            return true;
        }
        try {
            Trajectory trajectory = Trajectory.valueOf(args[0].toUpperCase());
            listener.setTrajectory(player, trajectory);
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text("无效的轨迹类型: " + args[0], NamedTextColor.RED));
        }
        return true;
    }
}
