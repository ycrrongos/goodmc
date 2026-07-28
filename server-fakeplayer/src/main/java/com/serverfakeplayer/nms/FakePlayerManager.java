package com.serverfakeplayer.nms;

import com.mojang.authlib.GameProfile;
import com.serverfakeplayer.ServerFakePlayerPlugin;
import com.serverfakeplayer.authme.AuthMeHook;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

public final class FakePlayerManager {

    private final ServerFakePlayerPlugin plugin;
    private final Map<String, FakeServerPlayer> byName = new ConcurrentHashMap<>();
    private AuthMeHook authMeHook;

    public FakePlayerManager(ServerFakePlayerPlugin plugin) {
        this.plugin = plugin;
    }

    public void setAuthMeHook(AuthMeHook authMeHook) {
        this.authMeHook = authMeHook;
    }

    public Collection<FakeServerPlayer> all() {
        return byName.values();
    }

    public FakeServerPlayer get(String name) {
        return byName.get(name.toLowerCase(Locale.ROOT));
    }

    public boolean isFake(Player player) {
        return player != null && byName.containsKey(player.getName().toLowerCase(Locale.ROOT));
    }

    public boolean spawn(Player creator, String name) {
        if (byName.size() >= plugin.getConfig().getInt("max-fake-players", 20)) {
            creator.sendMessage(Component.text("假人数量已达上限。", NamedTextColor.RED));
            return false;
        }
        if (name.length() < 1 || name.length() > 16 || !name.matches("[A-Za-z0-9_]+")) {
            creator.sendMessage(Component.text("假人名称无效（1-16 位，仅字母数字下划线）。", NamedTextColor.RED));
            return false;
        }
        String key = name.toLowerCase(Locale.ROOT);
        if (byName.containsKey(key) || Bukkit.getPlayerExact(name) != null) {
            creator.sendMessage(Component.text("该名称已在线。", NamedTextColor.RED));
            return false;
        }

        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
        Location loc = creator.getLocation();
        ServerLevel level = ((CraftWorld) loc.getWorld()).getHandle();

        UUID uuid = UUIDUtil.createOfflinePlayerUUID(name);
        GameProfile profile = new GameProfile(uuid, name);

        try {
            ServerPlayer handle = ((CraftPlayer) creator).getHandle();
            GameProfile creatorProfile = handle.getGameProfile();
            if (creatorProfile.properties().containsKey("textures")) {
                profile.properties().putAll("textures", creatorProfile.properties().get("textures"));
            }
        } catch (Exception ignored) {
        }

        GameType gameType = mapGameType(plugin.getConfig().getString("default-gamemode", "SURVIVAL"));
        FakeServerPlayer fake = FakeServerPlayer.spawn(
                server,
                level,
                profile,
                new Vec3(loc.getX(), loc.getY(), loc.getZ()),
                loc.getYaw(),
                loc.getPitch(),
                gameType,
                creator.getName()
        );
        byName.put(key, fake);
        scheduleAuthMeLogin(fake);

        creator.sendMessage(Component.text("已生成假人 ", NamedTextColor.GREEN)
                .append(Component.text(name, NamedTextColor.AQUA)));
        return true;
    }

    private void scheduleAuthMeLogin(FakeServerPlayer fake) {
        if (authMeHook == null || !plugin.getConfig().getBoolean("authme.auto-login", true)) {
            return;
        }
        Runnable login = () -> {
            if (fake.isRemoved()) {
                return;
            }
            Player bukkit = fake.getBukkitEntity();
            if (bukkit != null && bukkit.isOnline()) {
                authMeHook.ensureLoggedIn(bukkit);
            }
        };
        Bukkit.getScheduler().runTask(plugin, login);
        Bukkit.getScheduler().runTaskLater(plugin, login, 1L);
        Bukkit.getScheduler().runTaskLater(plugin, login, 5L);
        Bukkit.getScheduler().runTaskLater(plugin, login, 20L);
    }

    public boolean kill(String name, String reason) {
        FakeServerPlayer fake = byName.remove(name.toLowerCase(Locale.ROOT));
        if (fake == null) {
            for (Map.Entry<String, FakeServerPlayer> entry : byName.entrySet()) {
                if (entry.getValue().getGameProfile().name().equalsIgnoreCase(name)) {
                    fake = byName.remove(entry.getKey());
                    break;
                }
            }
        }
        if (fake == null) {
            return false;
        }
        fake.discard(net.minecraft.network.chat.Component.literal(reason));
        return true;
    }

    public void removeAll(String reason) {
        for (String key : byName.keySet().toArray(String[]::new)) {
            kill(key, reason);
        }
    }

    public void tickActions() {
        byName.entrySet().removeIf(entry -> entry.getValue().isRemoved());
    }

    private static GameType mapGameType(String name) {
        try {
            GameMode mode = GameMode.valueOf(name.toUpperCase(Locale.ROOT));
            return switch (mode) {
                case CREATIVE -> GameType.CREATIVE;
                case ADVENTURE -> GameType.ADVENTURE;
                case SPECTATOR -> GameType.SPECTATOR;
                default -> GameType.SURVIVAL;
            };
        } catch (IllegalArgumentException exception) {
            return GameType.SURVIVAL;
        }
    }
}
