package com.goodtpa.util;

/**
 * Utility class to check if Paper Dialog API is available.
 * Dialog API was introduced in MC 1.21.6 (Paper 26.2+).
 */
public final class DialogApiUtil {

    private static final boolean DIALOG_AVAILABLE;

    static {
        boolean available = false;
        try {
            Class.forName("io.papermc.paper.dialog.Dialog");
            available = true;
        } catch (ClassNotFoundException e) {
            // Dialog API not available
        }
        DIALOG_AVAILABLE = available;
    }

    private DialogApiUtil() {}

    /**
     * Check if Paper Dialog API is available on this server.
     * @return true if Dialog API is available (MC 1.21.6+), false otherwise
     */
    public static boolean isAvailable() {
        return DIALOG_AVAILABLE;
    }
}
