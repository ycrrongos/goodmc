package com.goodtpa.waypoint;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public final class WaypointGuiHolder implements InventoryHolder {

    public static final int TAB_PUBLIC = 10;
    public static final int TAB_CREATE = 12;
    public static final int TAB_DELETE_MODE = 14;
    public static final int TAB_PRIVATE = 16;

    public static final int[] WAYPOINT_SLOTS = {
            19, 20, 21, 22, 23, 24, 25,
            37, 38, 39, 40, 41, 42, 43
    };

    public static final int CREATE_NAME = 22;
    public static final int CREATE_ICON = 24;
    public static final int CREATE_VISIBILITY = 31;
    public static final int CREATE_CONFIRM = 40;

    public static final int BACK_BUTTON = 49;
    public static final int PAGE_PREVIOUS = 45;
    public static final int PAGE_NEXT = 53;
    public static final int SWITCH_UI_SLOT = 48;

    public static final int PAGE_SIZE = WAYPOINT_SLOTS.length;

    private final WaypointTab tab;
    private final int page;
    private final Map<Integer, UUID> slotWaypoints = new HashMap<>();
    private Inventory inventory;

    public WaypointGuiHolder(WaypointTab tab, int page) {
        this.tab = tab;
        this.page = page;
    }

    public WaypointTab tab() {
        return tab;
    }

    public int page() {
        return page;
    }

    public void mapSlot(int slot, UUID waypointId) {
        slotWaypoints.put(slot, waypointId);
    }

    public UUID waypointAt(int slot) {
        return slotWaypoints.get(slot);
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
