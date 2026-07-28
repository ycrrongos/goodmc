package com.goodtpa.waypoint;

import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;

public final class WaypointCreateSession {

    private Location location;
    private String name;
    private Material icon = Material.LODESTONE;
    private boolean isPublic = true;
    private boolean awaitingName;

    public Location location() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location == null ? null : location.clone();
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.awaitingName = false;
    }

    public Material icon() {
        return icon;
    }

    public void setIcon(Material icon) {
        this.icon = icon;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public boolean isAwaitingName() {
        return awaitingName;
    }

    public void beginNameInput() {
        this.awaitingName = true;
    }

    public void cancelNameInput() {
        this.awaitingName = false;
    }

    public boolean isReady() {
        return location != null && name != null && !name.isBlank() && icon != null;
    }

    public Waypoint toWaypoint(UUID ownerId) {
        return new Waypoint(UUID.randomUUID(), ownerId, isPublic, name.trim(), icon, location);
    }
}
