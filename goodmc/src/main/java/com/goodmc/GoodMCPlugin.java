package com.goodmc;

import com.goodmc.command.FireballUnlockCommand;
import com.goodmc.command.KillCommand;
import com.goodmc.command.ScCommand;
import com.goodmc.command.SeedCommand;
import com.goodmc.command.TntGuiCommand;
import com.goodmc.command.TntTrajCommand;
import com.goodmc.command.TntUnlockCommand;
import com.goodmc.enchant.CompatibleEnchantsListener;
import com.goodmc.listener.CreeperExplosionListener;
import com.goodmc.listener.FireChargeListener;
import com.goodmc.listener.GrindstoneCurseListener;
import com.goodmc.listener.GrindstoneExtractorListener;
import com.goodmc.listener.JoinHintListener;
import com.goodmc.listener.PistonEndRodListener;
import com.goodmc.listener.SnowGolemProjectileImmunityListener;
import com.goodmc.listener.SnowballListener;
import com.goodmc.listener.TntThrowListener;
import com.goodmc.listener.TntTrajectoryGui;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class GoodMCPlugin extends JavaPlugin {

    private GoodMCPluginConfig pluginConfig;

    @Override
    public void onEnable() {
        pluginConfig = new GoodMCPluginConfig(this);
        pluginConfig.load();
        saveResource("server-menu.yml", false);

        var pm = getServer().getPluginManager();
        pm.registerEvents(new SnowballListener(), this);
        pm.registerEvents(new SnowGolemProjectileImmunityListener(), this);
        TntThrowListener tntListener = new TntThrowListener(this);
        pm.registerEvents(tntListener, this);
        TntTrajectoryGui trajectoryGui = new TntTrajectoryGui(this, tntListener);
        pm.registerEvents(trajectoryGui, this);
        tntListener.setTrajectoryGui(trajectoryGui);
        registerCommand("tnt_unlock", new TntUnlockCommand(tntListener));
        registerCommand("tnt_traj", new TntTrajCommand(tntListener));
        registerCommand("tnt_gui", new TntGuiCommand(tntListener));
        
        FireChargeListener fireChargeListener = new FireChargeListener();
        pm.registerEvents(fireChargeListener, this);
        registerCommand("fireball_unlock", new FireballUnlockCommand(fireChargeListener));
        pm.registerEvents(new CreeperExplosionListener(), this);
        pm.registerEvents(new GrindstoneExtractorListener(), this);
        pm.registerEvents(new GrindstoneCurseListener(), this);
        pm.registerEvents(new CompatibleEnchantsListener(pluginConfig), this);
        pm.registerEvents(new JoinHintListener(pluginConfig), this);
        pm.registerEvents(new PistonEndRodListener(), this);

        KillCommand killCommand = new KillCommand();
        if (pluginConfig.commandEnabled("kill")) {
            registerCommand("kill", killCommand);
            pm.registerEvents(killCommand, this);
        }
        if (pluginConfig.commandEnabled("seed")) {
            registerCommand("seed", new SeedCommand());
        }
        if (pluginConfig.commandEnabled("sc")) {
            registerCommand("sc", new ScCommand(this));
        }

        getLogger().info("GoodMC enabled");
    }

    public GoodMCPluginConfig pluginConfig() {
        return pluginConfig;
    }

    private void registerCommand(String name, CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Command not found in plugin.yml: " + name);
            return;
        }
        command.setExecutor(executor);
    }
}
