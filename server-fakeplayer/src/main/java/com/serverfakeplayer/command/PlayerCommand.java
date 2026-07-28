package com.serverfakeplayer.command;

import com.serverfakeplayer.action.EntityPlayerActionPack;
import com.serverfakeplayer.nms.FakePlayerManager;
import com.serverfakeplayer.nms.FakeServerPlayer;
import com.serverfakeplayer.permission.FakePlayerPermissionStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Carpet-inspired: /player &lt;name&gt; &lt;action&gt; ...
 */
public final class PlayerCommand implements CommandExecutor, TabCompleter {

    private static final List<String> ACTIONS = List.of(
            "spawn", "kill", "stop",
            "attack", "use", "jump",
            "sneak", "sprint",
            "look", "move"
    );

    private final FakePlayerManager manager;
    private final FakePlayerPermissionStore permissions;

    public PlayerCommand(FakePlayerManager manager, FakePlayerPermissionStore permissions) {
        this.manager = manager;
        this.permissions = permissions;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!permissions.canUse(sender)) {
            sender.sendMessage(Component.text("没有权限。请让管理员执行 /fakeop 你的名字", NamedTextColor.RED));
            return true;
        }
        if (args.length < 2) {
            sendUsage(sender);
            return true;
        }

        String name = args[0];
        String action = args[1].toLowerCase(Locale.ROOT);
        String[] rest = Arrays.copyOfRange(args, 2, args.length);

        return switch (action) {
            case "spawn" -> handleSpawn(sender, name);
            case "kill" -> handleKill(sender, name);
            case "stop" -> handleStop(sender, name);
            case "attack", "use", "jump" -> handleTimedAction(sender, name, action, rest);
            case "sneak" -> handleToggle(sender, name, true, rest);
            case "sprint" -> handleToggle(sender, name, false, rest);
            case "look" -> handleLook(sender, name, rest);
            case "move" -> handleMove(sender, name, rest);
            default -> {
                sendUsage(sender);
                yield true;
            }
        };
    }

    private boolean handleSpawn(CommandSender sender, String name) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("只能由玩家生成假人。", NamedTextColor.RED));
            return true;
        }
        if (!permissions.canSpawn(sender)) {
            sender.sendMessage(Component.text("没有生成权限。请让管理员执行 /fakeop 你的名字", NamedTextColor.RED));
            return true;
        }
        manager.spawn(player, name);
        return true;
    }

    private boolean handleKill(CommandSender sender, String name) {
        if (manager.kill(name, "Killed")) {
            sender.sendMessage(Component.text("已移除假人 " + name, NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("找不到假人 " + name, NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleStop(CommandSender sender, String name) {
        FakeServerPlayer fake = requireFake(sender, name);
        if (fake == null) {
            return true;
        }
        fake.actionPack().stopAll();
        sender.sendMessage(Component.text(name + " 已停止所有动作", NamedTextColor.GREEN));
        return true;
    }

    private boolean handleTimedAction(CommandSender sender, String name, String action, String[] rest) {
        FakeServerPlayer fake = requireFake(sender, name);
        if (fake == null) {
            return true;
        }
        EntityPlayerActionPack.Action packAction = parseOnceContinuous(rest);
        EntityPlayerActionPack.ActionType type = switch (action) {
            case "attack" -> EntityPlayerActionPack.ActionType.ATTACK;
            case "use" -> EntityPlayerActionPack.ActionType.USE;
            case "jump" -> EntityPlayerActionPack.ActionType.JUMP;
            default -> null;
        };
        if (type == null) {
            return true;
        }
        fake.actionPack().start(type, packAction);
        sender.sendMessage(Component.text(name + " " + action + " " + describe(packAction), NamedTextColor.GREEN));
        return true;
    }

    private boolean handleToggle(CommandSender sender, String name, boolean sneak, String[] rest) {
        FakeServerPlayer fake = requireFake(sender, name);
        if (fake == null) {
            return true;
        }
        boolean enable = rest.length == 0 || !rest[0].equalsIgnoreCase("false");
        if (sneak) {
            fake.actionPack().setSneaking(enable);
            sender.sendMessage(Component.text(name + " sneak " + enable, NamedTextColor.GREEN));
        } else {
            fake.actionPack().setSprinting(enable);
            sender.sendMessage(Component.text(name + " sprint " + enable, NamedTextColor.GREEN));
        }
        return true;
    }

    private boolean handleLook(CommandSender sender, String name, String[] rest) {
        FakeServerPlayer fake = requireFake(sender, name);
        if (fake == null) {
            return true;
        }
        if (rest.length == 0) {
            sender.sendMessage(Component.text("用法: /player <名> look <north|south|east|west|up|down|yaw pitch|at x y z>", NamedTextColor.RED));
            return true;
        }
        String first = rest[0].toLowerCase(Locale.ROOT);
        Direction dir = switch (first) {
            case "north" -> Direction.NORTH;
            case "south" -> Direction.SOUTH;
            case "east" -> Direction.EAST;
            case "west" -> Direction.WEST;
            case "up" -> Direction.UP;
            case "down" -> Direction.DOWN;
            default -> null;
        };
        if (dir != null) {
            fake.actionPack().look(dir);
            sender.sendMessage(Component.text(name + " look " + first, NamedTextColor.GREEN));
            return true;
        }
        if (first.equals("at") && rest.length >= 4) {
            try {
                double x = Double.parseDouble(rest[1]);
                double y = Double.parseDouble(rest[2]);
                double z = Double.parseDouble(rest[3]);
                fake.actionPack().lookAt(new Vec3(x, y, z));
                sender.sendMessage(Component.text(name + " look at " + x + " " + y + " " + z, NamedTextColor.GREEN));
            } catch (NumberFormatException exception) {
                sender.sendMessage(Component.text("坐标无效。", NamedTextColor.RED));
            }
            return true;
        }
        if (rest.length >= 2) {
            try {
                float yaw = Float.parseFloat(rest[0]);
                float pitch = Float.parseFloat(rest[1]);
                fake.actionPack().look(yaw, pitch);
                sender.sendMessage(Component.text(name + " look " + yaw + " " + pitch, NamedTextColor.GREEN));
            } catch (NumberFormatException exception) {
                sender.sendMessage(Component.text("角度无效。", NamedTextColor.RED));
            }
            return true;
        }
        sender.sendMessage(Component.text("用法: /player <名> look <方向|yaw pitch|at x y z>", NamedTextColor.RED));
        return true;
    }

    private boolean handleMove(CommandSender sender, String name, String[] rest) {
        FakeServerPlayer fake = requireFake(sender, name);
        if (fake == null) {
            return true;
        }
        if (rest.length == 0 || rest[0].equalsIgnoreCase("stop")) {
            fake.actionPack().setForward(0).setStrafing(0);
            sender.sendMessage(Component.text(name + " move stop", NamedTextColor.GREEN));
            return true;
        }
        String dir = rest[0].toLowerCase(Locale.ROOT);
        switch (dir) {
            case "forward" -> fake.actionPack().setForward(1.0F);
            case "backward" -> fake.actionPack().setForward(-1.0F);
            case "left" -> fake.actionPack().setStrafing(1.0F);
            case "right" -> fake.actionPack().setStrafing(-1.0F);
            default -> {
                sender.sendMessage(Component.text("用法: /player <名> move <forward|backward|left|right|stop>", NamedTextColor.RED));
                return true;
            }
        }
        sender.sendMessage(Component.text(name + " move " + dir, NamedTextColor.GREEN));
        return true;
    }

    private FakeServerPlayer requireFake(CommandSender sender, String name) {
        FakeServerPlayer fake = manager.get(name);
        if (fake == null) {
            sender.sendMessage(Component.text("找不到假人 " + name + "（请先 /player " + name + " spawn）", NamedTextColor.RED));
        }
        return fake;
    }

    private static EntityPlayerActionPack.Action parseOnceContinuous(String[] rest) {
        if (rest.length > 0 && rest[0].equalsIgnoreCase("continuous")) {
            return EntityPlayerActionPack.Action.continuous();
        }
        return EntityPlayerActionPack.Action.once();
    }

    private static String describe(EntityPlayerActionPack.Action action) {
        return action.limit == 1 ? "once" : "continuous";
    }

    private static void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("用法:", NamedTextColor.GOLD));
        sender.sendMessage(Component.text(" /player <名> spawn|kill|stop", NamedTextColor.GRAY));
        sender.sendMessage(Component.text(" /player <名> attack|use|jump [once|continuous]", NamedTextColor.GRAY));
        sender.sendMessage(Component.text(" /player <名> sneak|sprint [true|false]", NamedTextColor.GRAY));
        sender.sendMessage(Component.text(" /player <名> look <方向|yaw pitch|at x y z>", NamedTextColor.GRAY));
        sender.sendMessage(Component.text(" /player <名> move <forward|backward|left|right|stop>", NamedTextColor.GRAY));
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (!permissions.canUse(sender)) {
            return List.of();
        }
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            for (FakeServerPlayer fake : manager.all()) {
                String n = fake.getGameProfile().name();
                if (n.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    names.add(n);
                }
            }
            return names;
        }
        if (args.length == 2) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return ACTIONS.stream().filter(a -> a.startsWith(prefix)).toList();
        }
        if (args.length == 3) {
            String action = args[1].toLowerCase(Locale.ROOT);
            return switch (action) {
                case "attack", "use", "jump" -> filter(args[2], "once", "continuous");
                case "sneak", "sprint" -> filter(args[2], "true", "false");
                case "look" -> filter(args[2], "north", "south", "east", "west", "up", "down", "at");
                case "move" -> filter(args[2], "forward", "backward", "left", "right", "stop");
                default -> List.of();
            };
        }
        return List.of();
    }

    private static List<String> filter(String prefix, String... options) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String option : options) {
            if (option.startsWith(p)) {
                out.add(option);
            }
        }
        return out;
    }
}
