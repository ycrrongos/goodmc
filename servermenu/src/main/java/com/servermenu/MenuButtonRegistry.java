package com.servermenu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class MenuButtonRegistry {

    private final JavaPlugin plugin;
    private final Map<Integer, MenuButton> buttonsBySlot = new LinkedHashMap<>();

    public MenuButtonRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        buttonsBySlot.clear();
        List<Plugin> plugins = new ArrayList<>(List.of(plugin.getServer().getPluginManager().getPlugins()));
        plugins.sort(Comparator.comparing(Plugin::getName, String.CASE_INSENSITIVE_ORDER));

        for (Plugin other : plugins) {
            if (other.getName().equalsIgnoreCase(plugin.getName())) {
                continue;
            }
            YamlConfiguration contribution = loadContribution(other);
            if (contribution == null) {
                continue;
            }
            mergeButtons(contribution.getMapList("buttons"), false);
        }

        FileConfiguration local = plugin.getConfig();
        mergeButtons(local.getMapList("buttons"), true);
    }

    public List<MenuButton> buttons() {
        List<MenuButton> list = new ArrayList<>(buttonsBySlot.values());
        list.sort(Comparator.comparingInt(MenuButton::slot));
        return Collections.unmodifiableList(list);
    }

    private YamlConfiguration loadContribution(Plugin other) {
        java.io.File dataFile = new java.io.File(other.getDataFolder(), "server-menu.yml");
        if (dataFile.isFile()) {
            return YamlConfiguration.loadConfiguration(dataFile);
        }
        java.io.InputStream stream = other.getResource("server-menu.yml");
        if (stream == null) {
            return null;
        }
        return YamlConfiguration.loadConfiguration(
                new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)
        );
    }

    private void mergeButtons(List<Map<?, ?>> rawButtons, boolean override) {
        for (Map<?, ?> raw : rawButtons) {
            MenuButton button = parseButton(raw);
            if (button == null) {
                continue;
            }
            if (!override && buttonsBySlot.containsKey(button.slot())) {
                continue;
            }
            if (!isCommandAvailable(button.command())) {
                continue;
            }
            buttonsBySlot.put(button.slot(), button);
        }
    }

    private MenuButton parseButton(Map<?, ?> raw) {
        Object slotObj = raw.get("slot");
        if (!(slotObj instanceof Number number)) {
            return null;
        }
        int slot = number.intValue();
        if (slot < 0 || slot >= 54 || slot == MenuGuiHolder.BACK_SLOT) {
            return null;
        }

        String materialName = String.valueOf(raw.get("material") != null ? raw.get("material") : "STONE");
        Material material = Material.matchMaterial(materialName);
        if (material == null || material.isAir()) {
            material = Material.STONE;
        }

        String name = String.valueOf(raw.get("name") != null ? raw.get("name") : "按钮");
        List<String> lore = new ArrayList<>();
        Object loreObj = raw.get("lore");
        if (loreObj instanceof List<?> list) {
            for (Object line : list) {
                lore.add(String.valueOf(line));
            }
        }

        Object commandObj = raw.get("command");
        String command = commandObj == null ? "" : String.valueOf(commandObj).trim();
        if (command.isEmpty()) {
            return null;
        }

        boolean pickPlayer = Boolean.parseBoolean(String.valueOf(raw.get("pick-player") != null ? raw.get("pick-player") : "false"));
        boolean chatInput = Boolean.parseBoolean(String.valueOf(raw.get("chat-input") != null ? raw.get("chat-input") : "false"));
        String prompt = String.valueOf(raw.get("prompt") != null ? raw.get("prompt") : "请在聊天栏输入内容。输入取消可放弃。");

        return new MenuButton(slot, material, name, lore, command, pickPlayer, chatInput, prompt);
    }

    private boolean isCommandAvailable(String commandTemplate) {
        String first = commandTemplate.split("\\s+")[0].toLowerCase(Locale.ROOT);
        if (first.startsWith("/")) {
            first = first.substring(1);
        }
        if (first.contains(":")) {
            first = first.substring(first.indexOf(':') + 1);
        }
        first = first.replace("{player}", "x").replace("{input}", "x");
        return plugin.getServer().getPluginCommand(first) != null;
    }
}
