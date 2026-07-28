package com.serverfakeplayer.authme;

import java.lang.reflect.Method;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Soft-depend AuthMe hook via reflection (no compile-time AuthMe dependency).
 */
public final class AuthMeHook {

    private final JavaPlugin plugin;
    private Object api;
    private Method forceLogin;
    private Method isRegisteredName;
    private Method isRegisteredPlayer;
    private Method forceRegister;
    private Method registerPlayer;
    private boolean available;

    public AuthMeHook(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void tryHook() {
        available = false;
        api = null;
        Plugin authMe = Bukkit.getPluginManager().getPlugin("AuthMe");
        if (authMe == null || !authMe.isEnabled()) {
            plugin.getLogger().info("AuthMe not found; skip AuthMe integration.");
            return;
        }
        try {
            Class<?> apiClass = Class.forName("fr.xephi.authme.api.v3.AuthMeApi");
            Method getInstance = apiClass.getMethod("getInstance");
            api = getInstance.invoke(null);
            if (api == null) {
                plugin.getLogger().warning("AuthMe is enabled but AuthMeApi is not ready yet.");
                return;
            }
            forceLogin = findMethod(apiClass, "forceLogin", Player.class);
            isRegisteredName = findMethod(apiClass, "isRegistered", String.class);
            isRegisteredPlayer = findMethod(apiClass, "isRegistered", Player.class);
            forceRegister = findMethod(apiClass, "forceRegister", Player.class, String.class);
            registerPlayer = findMethod(apiClass, "registerPlayer", String.class, String.class);
            available = forceLogin != null;
            if (available) {
                plugin.getLogger().info("Hooked AuthMe (" + authMe.getDescription().getVersion() + ").");
            } else {
                plugin.getLogger().warning("AuthMeApi.forceLogin not found; cannot auto-login fake players.");
            }
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to hook AuthMe", exception);
            available = false;
        }
    }

    public void unhook() {
        available = false;
        api = null;
    }

    public boolean isAvailable() {
        return available && api != null && Bukkit.getPluginManager().isPluginEnabled("AuthMe");
    }

    /**
     * Ensure the player is treated as authenticated by AuthMe.
     */
    public boolean ensureLoggedIn(Player player) {
        if (!isAvailable() || player == null || !player.isOnline()) {
            return false;
        }
        try {
            if (!isRegistered(player)) {
                String password = "SfP_" + Long.toHexString(System.nanoTime());
                boolean registered = false;
                if (forceRegister != null) {
                    Object result = forceRegister.invoke(api, player, password);
                    registered = result == null || Boolean.TRUE.equals(result);
                } else if (registerPlayer != null) {
                    Object result = registerPlayer.invoke(api, player.getName(), password);
                    registered = Boolean.TRUE.equals(result);
                }
                if (!registered) {
                    plugin.getLogger().fine("AuthMe register skipped/failed for " + player.getName());
                }
            }
            forceLogin.invoke(api, player);
            return true;
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().log(Level.WARNING, "AuthMe forceLogin failed for " + player.getName(), exception);
            return false;
        }
    }

    private boolean isRegistered(Player player) throws ReflectiveOperationException {
        if (isRegisteredPlayer != null) {
            Object result = isRegisteredPlayer.invoke(api, player);
            return Boolean.TRUE.equals(result);
        }
        if (isRegisteredName != null) {
            Object result = isRegisteredName.invoke(api, player.getName());
            return Boolean.TRUE.equals(result);
        }
        return false;
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... params) {
        try {
            return type.getMethod(name, params);
        } catch (NoSuchMethodException exception) {
            return null;
        }
    }
}
