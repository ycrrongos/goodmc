package com.goodtpa.command;

import com.goodtpa.menu.TpaMenuGuiService;
import com.goodtpa.menu.TpaMenuScreen;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to open TPA menu player pickers from Dialog UI.
 * Usage: /tpa_menu_open <tpa|tpahere|tpaccept|tpdeny>
 */
public final class TpaMenuOpenCommand implements CommandExecutor {

    private final TpaMenuGuiService guiService;

    public TpaMenuOpenCommand(TpaMenuGuiService guiService) {
        this.guiService = guiService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("此命令仅限玩家使用。");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage("用法: /tpa_menu_open <tpa|tpahere|tpaccept|tpdeny>");
            return true;
        }

        TpaMenuScreen screen = switch (args[0].toLowerCase()) {
            case "tpa" -> TpaMenuScreen.PLAYER_PICK_TPA;
            case "tpahere" -> TpaMenuScreen.PLAYER_PICK_TPAHERE;
            case "tpaccept" -> TpaMenuScreen.PLAYER_PICK_TPACCEPT;
            case "tpdeny" -> TpaMenuScreen.PLAYER_PICK_TPADENY;
            default -> null;
        };

        if (screen == null) {
            player.sendMessage("未知操作: " + args[0]);
            player.sendMessage("可用操作: tpa, tpahere, tpaccept, tpdeny");
            return true;
        }

        guiService.openPlayerPicker(player, screen);
        return true;
    }
}
