package com.goodtpa.waypoint;

import org.bukkit.Location;

public final class WaypointPlayerState {

    private boolean deleteMode;
    private int publicPage;
    private int privatePage;
    private Location backLocation;

    public boolean deleteMode() {
        return deleteMode;
    }

    public void setDeleteMode(boolean deleteMode) {
        this.deleteMode = deleteMode;
    }

    public int page(WaypointTab tab) {
        return switch (tab) {
            case PUBLIC -> publicPage;
            case PRIVATE -> privatePage;
            case CREATE -> 0;
        };
    }

    public void setPage(WaypointTab tab, int page) {
        int safePage = Math.max(0, page);
        switch (tab) {
            case PUBLIC -> publicPage = safePage;
            case PRIVATE -> privatePage = safePage;
            case CREATE -> {
            }
        }
    }

    public Location backLocation() {
        return backLocation;
    }

    public void setBackLocation(Location backLocation) {
        this.backLocation = backLocation == null ? null : backLocation.clone();
    }

    public void clearBackLocation() {
        this.backLocation = null;
    }
}
