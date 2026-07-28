package com.goodmc.command;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class ScCommand implements CommandExecutor {

    private final JavaPlugin plugin;

    public ScCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!sender.hasPermission("goodmc.sc")) {
            sender.sendMessage(Component.text("你没有权限查看功能说明。", NamedTextColor.RED));
            return true;
        }

        sender.sendMessage(Component.text("══════ GoodMC 功能说明 ══════", NamedTextColor.GOLD));
        sender.sendMessage(line("/seed", "查看当前世界种子"));
        sender.sendMessage(line("/kill", "自杀（管理员击杀他人需投票）"));
        sender.sendMessage(line("/sc", "显示本帮助"));
        sender.sendMessage(Component.text("玩法增强：", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text(" • 雪球造成 1 点伤害", NamedTextColor.GRAY));
        sender.sendMessage(Component.text(" • 手持 TNT 左键丢出（末影珍珠抛物线）", NamedTextColor.GRAY));
        sender.sendMessage(Component.text(" • 手持火焰弹右键发射恶魂火球", NamedTextColor.GRAY));
        sender.sendMessage(Component.text(" • 雪傀儡免疫弹射物伤害", NamedTextColor.GRAY));
        sender.sendMessage(Component.text(" • 双砂轮相邻可只去除绑定/消失诅咒", NamedTextColor.GRAY));
        sender.sendMessage(Component.text(" • 铁砧上放砂轮可提取附魔为附魔书", NamedTextColor.GRAY));
        sender.sendMessage(Component.text(" • 苦力怕爆炸仅破坏草类植物", NamedTextColor.GRAY));
        sender.sendMessage(Component.text(" • 铁砧可合并所有互斥附魔（如无限+经验修补）", NamedTextColor.GRAY));

        FileConfiguration config = plugin.getConfig();
        List<String> extra = config.getStringList("help.extra-lines");
        for (String line : extra) {
            sender.sendMessage(Component.text(line, NamedTextColor.GRAY));
        }

        sender.sendMessage(Component.text("══════════════════════════", NamedTextColor.GOLD));
        sender.sendMessage(
                Component.text("作者：", NamedTextColor.GRAY)
                        .append(Component.text("B站 爱玩电脑的小融融", NamedTextColor.LIGHT_PURPLE))
                        .append(Component.text(" | GitHub: ", NamedTextColor.GRAY))
                        .append(Component.text("ycrrongos", NamedTextColor.AQUA))
        );
        return true;
    }

    private static Component line(String command, String description) {
        return Component.text(" ", NamedTextColor.GRAY)
                .append(Component.text(command, NamedTextColor.AQUA))
                .append(Component.text(" — " + description, NamedTextColor.WHITE));
    }
}
