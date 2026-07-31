package com.goodtpa.menu;

import com.goodtpa.tpa.TpaManager;
import com.goodtpa.util.DialogApiUtil;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public final class TpaMenuGuiService {

    private static final int PLAYER_PAGE_SIZE = 45;

    private final TpaManager tpaManager;

    public TpaMenuGuiService(TpaManager tpaManager) {
        this.tpaManager = tpaManager;
    }

    public void openMain(Player player) {
        if (DialogApiUtil.isAvailable()) {
            openMainDialog(player);
        } else {
            openMainChestGui(player);
        }
    }

    private void openMainDialog(Player player) {
        try {
            Dialog dialog = Dialog.create(builder -> builder.empty()
                    .base(io.papermc.paper.registry.data.dialog.DialogBase.builder(
                                    Component.text("传送菜单", NamedTextColor.DARK_AQUA))
                            .canCloseWithEscape(true)
                            .body(List.of(
                                    DialogBody.plainMessage(Component.text("选择传送操作", NamedTextColor.GRAY))
                            ))
                            .build()
                    )
                    .type(DialogType.multiAction(List.of(
                            ActionButton.builder(Component.text("传送到玩家", TextColor.color(0x55FF55)))
                                    .tooltip(Component.text("打开玩家选择器"))
                                    .action(io.papermc.paper.registry.data.dialog.action.DialogAction.staticAction(
                                            ClickEvent.runCommand("tpa_menu_open tpa")))
                                    .build(),
                            ActionButton.builder(Component.text("召唤玩家", TextColor.color(0x55FF55)))
                                    .tooltip(Component.text("打开玩家选择器"))
                                    .action(io.papermc.paper.registry.data.dialog.action.DialogAction.staticAction(
                                            ClickEvent.runCommand("tpa_menu_open tpahere")))
                                    .build(),
                            ActionButton.builder(Component.text("接受传送", TextColor.color(0x55FF55)))
                                    .tooltip(Component.text("打开请求者列表"))
                                    .action(io.papermc.paper.registry.data.dialog.action.DialogAction.staticAction(
                                            ClickEvent.runCommand("tpa_menu_open tpaccept")))
                                    .build(),
                            ActionButton.builder(Component.text("拒绝传送", TextColor.color(0xFF5555)))
                                    .tooltip(Component.text("打开请求者列表"))
                                    .action(io.papermc.paper.registry.data.dialog.action.DialogAction.staticAction(
                                            ClickEvent.runCommand("tpa_menu_open tpdeny")))
                                    .build(),
                            ActionButton.builder(Component.text("返回传送点", TextColor.color(0xFFFF55)))
                                    .tooltip(Component.text("等同 /tpaback"))
                                    .action(io.papermc.paper.registry.data.dialog.action.DialogAction.staticAction(
                                            ClickEvent.runCommand("tpaback")))
                                    .build(),
                            ActionButton.builder(Component.text("返回死亡点", TextColor.color(0xFFFF55)))
                                    .tooltip(Component.text("等同 /back"))
                                    .action(io.papermc.paper.registry.data.dialog.action.DialogAction.staticAction(
                                            ClickEvent.runCommand("back")))
                                    .build()
                    ), ActionButton.builder(Component.text("切换箱子 UI", TextColor.color(0x55FFFF)))
                            .tooltip(Component.text("点击切换到箱子 GUI 界面"))
                            .action(io.papermc.paper.registry.data.dialog.action.DialogAction.staticAction(
                                    ClickEvent.runCommand("tpamenu")))
                            .build(), 3))
            );
            player.showDialog(dialog);
        } catch (Exception e) {
            player.sendMessage(Component.text("Dialog UI 不可用，使用箱子 GUI", NamedTextColor.YELLOW));
            openMainChestGui(player);
        }
    }

    private void openMainChestGui(Player player) {
        TpaMenuGuiHolder holder = new TpaMenuGuiHolder(TpaMenuScreen.MAIN);
        Inventory inventory = Bukkit.createInventory(holder, 54, Component.text("传送菜单", NamedTextColor.DARK_AQUA));
        holder.setInventory(inventory);
        fillBackground(inventory);

        setAction(inventory, 10, Material.ENDER_PEARL, NamedTextColor.GREEN, "传送到玩家", "等同 /tpa", "点击选择在线玩家");
        setAction(inventory, 11, Material.ENDER_EYE, NamedTextColor.GREEN, "召唤玩家", "等同 /tpahere", "点击选择在线玩家");
        setAction(inventory, 12, Material.LIME_DYE, NamedTextColor.GREEN, "接受传送", "等同 /tpaccept", "点击选择请求者");
        setAction(inventory, 13, Material.RED_DYE, NamedTextColor.RED, "拒绝传送", "等同 /tpadeny", "点击选择请求者");
        setAction(inventory, 14, Material.CLOCK, NamedTextColor.YELLOW, "传送失效时间", "等同 /tpatimeout", "点击后在聊天栏输入秒数");
        setAction(inventory, 19, Material.COMPASS, NamedTextColor.YELLOW, "返回传送点", "等同 /tpaback");
        setAction(inventory, 20, Material.RECOVERY_COMPASS, NamedTextColor.YELLOW, "返回死亡点", "等同 /back");

        // Switch to Dialog UI button
        inventory.setItem(TpaMenuGuiHolder.SWITCH_UI_SLOT, createSwitchButtonItem());

        inventory.setItem(TpaMenuGuiHolder.BACK_SLOT, createItem(
                Material.BARRIER,
                Component.text("关闭", NamedTextColor.RED),
                List.of(Component.text("关闭菜单", NamedTextColor.GRAY))
        ));

        player.openInventory(inventory);
    }

    public void openPlayerPicker(Player viewer, TpaMenuScreen screen) {
        openPlayerPicker(viewer, screen, 0);
    }

    public void openPlayerPicker(Player viewer, TpaMenuScreen screen, int page) {
        List<Player> candidates = collectCandidates(viewer, screen);
        int totalPages = Math.max(1, (int) Math.ceil(candidates.size() / (double) PLAYER_PAGE_SIZE));
        int safePage = Math.min(Math.max(page, 0), totalPages - 1);

        TpaMenuGuiHolder holder = new TpaMenuGuiHolder(screen, safePage);
        Inventory inventory = Bukkit.createInventory(holder, 54, pickerTitle(screen));
        holder.setInventory(inventory);
        fillBackground(inventory);

        int start = safePage * PLAYER_PAGE_SIZE;
        int end = Math.min(start + PLAYER_PAGE_SIZE, candidates.size());
        for (int i = start; i < end; i++) {
            Player target = candidates.get(i);
            int slot = i - start;
            inventory.setItem(slot, createPlayerHead(target, screen));
            holder.mapSlot(slot, target.getUniqueId());
        }

        if (safePage > 0) {
            inventory.setItem(45, createItem(Material.ARROW, Component.text("上一页", NamedTextColor.YELLOW), List.of()));
        }
        if (end < candidates.size()) {
            inventory.setItem(53, createItem(Material.ARROW, Component.text("下一页", NamedTextColor.YELLOW), List.of()));
        }

        inventory.setItem(TpaMenuGuiHolder.BACK_SLOT, createItem(
                Material.ARROW,
                Component.text("返回主菜单", NamedTextColor.YELLOW),
                List.of(Component.text("返回传送菜单", NamedTextColor.GRAY))
        ));

        if (candidates.isEmpty()) {
            inventory.setItem(22, createItem(
                    Material.BARRIER,
                    Component.text("没有可选玩家", NamedTextColor.RED),
                    List.of(Component.text("当前没有符合条件的在线玩家", NamedTextColor.GRAY))
            ));
        }

        viewer.openInventory(inventory);
    }

    private List<Player> collectCandidates(Player viewer, TpaMenuScreen screen) {
        List<Player> candidates = new ArrayList<>();
        switch (screen) {
            case PLAYER_PICK_TPACCEPT, PLAYER_PICK_TPADENY -> {
                for (UUID requesterId : tpaManager.getIncomingRequesterIds(viewer.getUniqueId())) {
                    Player requester = Bukkit.getPlayer(requesterId);
                    if (requester != null) {
                        candidates.add(requester);
                    }
                }
            }
            case PLAYER_PICK_TPA, PLAYER_PICK_TPAHERE -> {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (!online.equals(viewer)) {
                        candidates.add(online);
                    }
                }
                candidates.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            }
            default -> {
            }
        }
        return candidates;
    }

    private static Component pickerTitle(TpaMenuScreen screen) {
        return switch (screen) {
            case PLAYER_PICK_TPA -> Component.text("选择玩家 · 传送到对方", NamedTextColor.AQUA);
            case PLAYER_PICK_TPAHERE -> Component.text("选择玩家 · 召唤对方", NamedTextColor.AQUA);
            case PLAYER_PICK_TPACCEPT -> Component.text("选择玩家 · 接受传送", NamedTextColor.GREEN);
            case PLAYER_PICK_TPADENY -> Component.text("选择玩家 · 拒绝传送", NamedTextColor.RED);
            default -> Component.text("选择玩家", NamedTextColor.WHITE);
        };
    }

    private static ItemStack createPlayerHead(Player target, TpaMenuScreen screen) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwningPlayer(target);
        meta.displayName(Component.text(target.getName(), NamedTextColor.AQUA, TextDecoration.BOLD));
        meta.lore(List.of(
                Component.text(switch (screen) {
                    case PLAYER_PICK_TPA -> "点击向对方发起传送请求";
                    case PLAYER_PICK_TPAHERE -> "点击请求对方传送到你这里";
                    case PLAYER_PICK_TPACCEPT -> "点击接受该玩家的传送请求";
                    case PLAYER_PICK_TPADENY -> "点击拒绝该玩家的传送请求";
                    default -> "点击选择";
                }, NamedTextColor.GRAY)
        ));
        skull.setItemMeta(meta);
        return skull;
    }

    private static void setAction(
            Inventory inventory,
            int slot,
            Material material,
            NamedTextColor nameColor,
            String name,
            String... lore
    ) {
        List<Component> loreComponents = new ArrayList<>();
        for (String line : lore) {
            loreComponents.add(Component.text(line, NamedTextColor.GRAY));
        }
        ItemStack item = createItem(material, Component.text(name, nameColor), loreComponents);
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        inventory.setItem(slot, item);
    }

    private static void fillBackground(Inventory inventory) {
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "), List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
    }

    private static ItemStack createItem(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        if (lore != null && !lore.isEmpty()) {
            meta.lore(lore.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).toList());
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createSwitchButtonItem() {
        ItemStack item = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("切换 Dialog UI", NamedTextColor.AQUA).decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("点击切换到 Dialog 对话框界面", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }
}
