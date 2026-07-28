package com.servermenu;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class MenuGuiHolder implements InventoryHolder {

    public static final int BACK_SLOT = 49;

    public enum Screen {
        MAIN,
        PLAYER_PICK
    }

    private final Screen screen;
    private final int page;
    private final MenuButton pendingButton;
    private final Map<Integer, UUID> playerAtSlot = new HashMap<>();
    private Inventory inventory;

    public MenuGuiHolder(Screen screen) {
        this(screen, 0, null);
    }

    public MenuGuiHolder(Screen screen, int page, MenuButton pendingButton) {
        this.screen = screen;
        this.page = page;
        this.pendingButton = pendingButton;
    }

    public Screen screen() {
        return screen;
    }

    public int page() {
        return page;
    }

    public MenuButton pendingButton() {
        return pendingButton;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    void mapSlot(int slot, UUID playerId) {
        playerAtSlot.put(slot, playerId);
    }

    UUID playerAt(int slot) {
        return playerAtSlot.get(slot);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
