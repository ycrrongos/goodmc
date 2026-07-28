package com.goodtpa.waypoint;

import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;

public final class Waypoint {

    private final UUID id;
    private final UUID ownerId;
    private final boolean isPublic;
    private String name;
    private Material icon;
    private String worldName;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;

    public Waypoint(
            UUID id,
            UUID ownerId,
            boolean isPublic,
            String name,
            Material icon,
            Location location
    ) {
        this(
                id,
                ownerId,
                isPublic,
                name,
                icon,
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getYaw(),
                location.getPitch()
        );
    }

    public Waypoint(
            UUID id,
            UUID ownerId,
            boolean isPublic,
            String name,
            Material icon,
            String worldName,
            double x,
            double y,
            double z,
            float yaw,
            float pitch
    ) {
        this.id = id;
        this.ownerId = ownerId;
        this.isPublic = isPublic;
        this.name = name;
        this.icon = icon;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public UUID id() {
        return id;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Material icon() {
        return icon;
    }

    public void setIcon(Material icon) {
        this.icon = icon;
    }

    public Location toLocation(org.bukkit.World world) {
        return new Location(world, x, y, z, yaw, pitch);
    }

    public String worldName() {
        return worldName;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }
}
