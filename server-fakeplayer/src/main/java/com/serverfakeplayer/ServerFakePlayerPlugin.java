package com.serverfakeplayer;

import com.serverfakeplayer.action.ActionPackTicker;
import com.serverfakeplayer.authme.AuthMeHook;
import com.serverfakeplayer.authme.AuthMeListener;
import com.serverfakeplayer.command.FakeOpCommand;
import com.serverfakeplayer.command.PlayerCommand;
import com.serverfakeplayer.nms.FakePlayerManager;
import com.serverfakeplayer.permission.FakePlayerPermissionStore;
import org.bukkit.plugin.java.JavaPlugin;

public final class ServerFakePlayerPlugin extends JavaPlugin {

    private FakePlayerManager fakePlayerManager;
    private FakePlayerPermissionStore permissionStore;
    private AuthMeHook authMeHook;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("server-menu.yml", false);
        permissionStore = new FakePlayerPermissionStore(this);
        fakePlayerManager = new FakePlayerManager(this);

        authMeHook = new AuthMeHook(this);
        authMeHook.tryHook();
        fakePlayerManager.setAuthMeHook(authMeHook);

        getServer().getPluginManager().registerEvents(new ActionPackTicker(fakePlayerManager), this);
        getServer().getPluginManager().registerEvents(new AuthMeListener(this, authMeHook, fakePlayerManager), this);

        var playerCmd = getCommand("player");
        if (playerCmd != null) {
            PlayerCommand playerCommand = new PlayerCommand(fakePlayerManager, permissionStore);
            playerCmd.setExecutor(playerCommand);
            playerCmd.setTabCompleter(playerCommand);
        }

        FakeOpCommand fakeOp = new FakeOpCommand(permissionStore, true);
        FakeOpCommand fakeDeop = new FakeOpCommand(permissionStore, false);
        var fakeOpCmd = getCommand("fakeop");
        if (fakeOpCmd != null) {
            fakeOpCmd.setExecutor(fakeOp);
            fakeOpCmd.setTabCompleter(fakeOp);
        }
        var fakeDeopCmd = getCommand("fakedeop");
        if (fakeDeopCmd != null) {
            fakeDeopCmd.setExecutor(fakeDeop);
            fakeDeopCmd.setTabCompleter(fakeDeop);
        }

        getLogger().info("Server-FakePlayer enabled");
    }

    @Override
    public void onDisable() {
        if (fakePlayerManager != null) {
            fakePlayerManager.removeAll("Server shutting down");
            fakePlayerManager = null;
        }
        if (authMeHook != null) {
            authMeHook.unhook();
            authMeHook = null;
        }
    }

    public FakePlayerManager fakePlayerManager() {
        return fakePlayerManager;
    }

    public FakePlayerPermissionStore permissionStore() {
        return permissionStore;
    }
}
