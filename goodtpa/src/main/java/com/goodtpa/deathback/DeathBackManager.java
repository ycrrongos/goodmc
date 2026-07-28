package com.goodtpa.deathback;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Location;

public final class DeathBackManager {

    private final Map<UUID, Location> deathLocations = new HashMap<>();

    public void setDeathLocation(UUID playerId, Location location) {
        deathLocations.put(playerId, location.clone());
    }

    public Optional<Location> getDeathLocation(UUID playerId) {
        return Optional.ofNullable(deathLocations.get(playerId));
    }

    public void clearDeathLocation(UUID playerId) {
        deathLocations.remove(playerId);
    }
}
