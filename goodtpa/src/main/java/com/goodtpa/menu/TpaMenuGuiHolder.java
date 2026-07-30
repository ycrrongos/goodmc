package com.goodtpa.menu;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class TpaMenuGuiHolder implements InventoryHolder {

    public static final int BACK_SLOT = 49;
    public static final int SWITCH_UI_SLOT = 48;

    private final TpaMenuScreen screen;
    private final int page;
    private final Map<Integer, UUID> playerAtSlot = new HashMap<>();
    private Inventory inventory;

    public TpaMenuGuiHolder(TpaMenuScreen screen) {
        this(screen, 0);
    }

    public TpaMenuGuiHolder(TpaMenuScreen screen, int page) {
        this.screen = screen;
        this.page = page;
    }

    public TpaMenuScreen screen() {
        return screen;
    }

    public int page() {
        return page;
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
