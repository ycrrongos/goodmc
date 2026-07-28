package com.servermenu;

import java.util.List;
import org.bukkit.Material;

public record MenuButton(
        int slot,
        Material material,
        String name,
        List<String> lore,
        String command,
        boolean pickPlayer,
        boolean chatInput,
        String prompt
) {
}
