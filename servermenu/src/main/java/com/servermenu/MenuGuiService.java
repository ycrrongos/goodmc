package com.servermenu;

import com.servermenu.util.DialogApiUtil;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.ArrayList;
import java.util.List;
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

public final class MenuGuiService {

    private static final int PLAYER_PAGE_SIZE = 45;

    private final MenuButtonRegistry registry;

    public MenuGuiService(MenuButtonRegistry registry) {
        this.registry = registry;
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
            // Build action buttons from registry
            List<ActionButton> actionButtons = new ArrayList<>();
            for (MenuButton button : registry.buttons()) {
                actionButtons.add(
                        ActionButton.builder(Component.text(button.name(), TextColor.color(0x55FFFF)))
                                .tooltip(Component.text(String.join("\n", button.lore())))
                                .action(io.papermc.paper.registry.data.dialog.action.DialogAction.staticAction(
                                        ClickEvent.runCommand(button.command())))
                                .build()
                );
            }

            // Add switch to chest UI button
            actionButtons.add(
                    ActionButton.builder(Component.text("切换箱子 UI", TextColor.color(0x55FFFF)))
                            .tooltip(Component.text("点击切换到箱子 GUI 界面"))
                            .action(io.papermc.paper.registry.data.dialog.action.DialogAction.staticAction(
                                    ClickEvent.runCommand("servermenu open")))
                            .build()
            );

            Dialog dialog = Dialog.create(builder -> builder.empty()
                    .base(io.papermc.paper.registry.data.dialog.DialogBase.builder(
                                    Component.text("服务器菜单", NamedTextColor.DARK_AQUA))
                            .canCloseWithEscape(true)
                            .body(List.of(
                                    DialogBody.plainMessage(Component.text("选择服务器操作", NamedTextColor.GRAY))
                            ))
                            .build()
                    )
                    .type(DialogType.multiAction(actionButtons, null, 3))
            );
            player.showDialog(dialog);
        } catch (Exception e) {
            player.sendMessage(Component.text("Dialog UI 不可用，使用箱子 GUI", NamedTextColor.YELLOW));
            openMainChestGui(player);
        }
    }

    private void openMainChestGui(Player player) {
        MenuGuiHolder holder = new MenuGuiHolder(MenuGuiHolder.Screen.MAIN);
        Inventory inventory = Bukkit.createInventory(holder, 54, Component.text("服务器菜单", NamedTextColor.DARK_AQUA));
        holder.setInventory(inventory);
        fillBackground(inventory);

        for (MenuButton button : registry.buttons()) {
            inventory.setItem(button.slot(), createActionItem(button));
        }

        // Switch to Dialog UI button
        inventory.setItem(MenuGuiHolder.SWITCH_UI_SLOT, createSwitchButtonItem());

        inventory.setItem(MenuGuiHolder.BACK_SLOT, createItem(
                Material.BARRIER,
                Component.text("关闭", NamedTextColor.RED),
                List.of(Component.text("关闭菜单", NamedTextColor.GRAY))
        ));

        player.openInventory(inventory);
    }

    public void openPlayerPicker(Player viewer, MenuButton pendingButton, int page) {
        List<Player> candidates = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(viewer)) {
                candidates.add(online);
            }
        }
        candidates.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        int totalPages = Math.max(1, (int) Math.ceil(candidates.size() / (double) PLAYER_PAGE_SIZE));
        int safePage = Math.min(Math.max(page, 0), totalPages - 1);

        MenuGuiHolder holder = new MenuGuiHolder(MenuGuiHolder.Screen.PLAYER_PICK, safePage, pendingButton);
        Inventory inventory = Bukkit.createInventory(
                holder,
                54,
                Component.text("选择玩家 · " + pendingButton.name(), NamedTextColor.AQUA)
        );
        holder.setInventory(inventory);
        fillBackground(inventory);

        int start = safePage * PLAYER_PAGE_SIZE;
        int end = Math.min(start + PLAYER_PAGE_SIZE, candidates.size());
        for (int i = start; i < end; i++) {
            Player target = candidates.get(i);
            int slot = i - start;
            inventory.setItem(slot, createPlayerHead(target));
            holder.mapSlot(slot, target.getUniqueId());
        }

        if (safePage > 0) {
            inventory.setItem(45, createItem(Material.ARROW, Component.text("上一页", NamedTextColor.YELLOW), List.of()));
        }
        if (end < candidates.size()) {
            inventory.setItem(53, createItem(Material.ARROW, Component.text("下一页", NamedTextColor.YELLOW), List.of()));
        }

        inventory.setItem(MenuGuiHolder.BACK_SLOT, createItem(
                Material.ARROW,
                Component.text("返回主菜单", NamedTextColor.YELLOW),
                List.of()
        ));

        if (candidates.isEmpty()) {
            inventory.setItem(22, createItem(
                    Material.BARRIER,
                    Component.text("没有可选玩家", NamedTextColor.RED),
                    List.of(Component.text("当前没有其他在线玩家", NamedTextColor.GRAY))
            ));
        }

        viewer.openInventory(inventory);
    }

    private static ItemStack createActionItem(MenuButton button) {
        List<Component> lore = new ArrayList<>();
        for (String line : button.lore()) {
            lore.add(Component.text(line, NamedTextColor.GRAY));
        }
        ItemStack item = createItem(button.material(), Component.text(button.name(), NamedTextColor.AQUA), lore);
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createPlayerHead(Player target) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setOwningPlayer(target);
        meta.displayName(Component.text(target.getName(), NamedTextColor.AQUA, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text("点击选择该玩家", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)));
        skull.setItemMeta(meta);
        return skull;
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
