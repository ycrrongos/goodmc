package com.adminvote;

import java.util.Locale;
import java.util.Set;
import org.bukkit.entity.Player;

public final class VanillaAdminCommands {

    private static final Set<String> COMMANDS = Set.of(
            "attribute",
            "ban",
            "ban-ip",
            "banlist",
            "clear",
            "clone",
            "data",
            "debug",
            "defaultgamemode",
            "deop",
            "difficulty",
            "effect",
            "execute",
            "experience",
            "fill",
            "gamemode",
            "gamerule",
            "give",
            "item",
            "jfr",
            "kick",
            "kill",
            "loot",
            "op",
            "pardon",
            "pardon-ip",
            "particle",
            "perf",
            "playsound",
            "publish",
            "random",
            "reload",
            "ride",
            "save-all",
            "save-off",
            "save-on",
            "seed",
            "setblock",
            "setidletimeout",
            "setworldspawn",
            "spreadplayers",
            "stop",
            "stopsound",
            "summon",
            "teleport",
            "tellraw",
            "time",
            "title",
            "tp",
            "transfer",
            "weather",
            "whitelist",
            "worldborder",
            "xp",
            "gm"
    );

    private VanillaAdminCommands() {
    }

    public static boolean isVanillaAdminCommand(String commandLine) {
        return COMMANDS.contains(parseCommandName(commandLine));
    }

    public static boolean isGameruleCommand(String commandLine) {
        return "gamerule".equals(parseCommandName(commandLine));
    }

    public static boolean isSelfKillCommand(String commandLine, Player player) {
        if (!"kill".equals(parseCommandName(commandLine))) {
            return false;
        }
        return isSelfKill(player.getName(), parseArguments(commandLine));
    }

    public static boolean isSelfKill(String playerName, String[] args) {
        if (args.length == 0) {
            return true;
        }
        if (args.length == 1) {
            String target = args[0].toLowerCase(Locale.ROOT);
            return target.equals(playerName.toLowerCase(Locale.ROOT))
                    || target.equals("@s")
                    || target.equals("@p");
        }
        return false;
    }

    public static String[] parseArguments(String commandLine) {
        if (!commandLine.startsWith("/")) {
            return new String[0];
        }

        String withoutSlash = commandLine.substring(1).trim();
        int spaceIndex = withoutSlash.indexOf(' ');
        if (spaceIndex == -1) {
            return new String[0];
        }

        String argsPart = withoutSlash.substring(spaceIndex + 1).trim();
        if (argsPart.isEmpty()) {
            return new String[0];
        }
        return argsPart.split("\\s+");
    }

    public static String parseCommandName(String commandLine) {
        if (!commandLine.startsWith("/")) {
            return "";
        }

        String withoutSlash = commandLine.substring(1);
        int spaceIndex = withoutSlash.indexOf(' ');
        String command = (spaceIndex == -1 ? withoutSlash : withoutSlash.substring(0, spaceIndex)).toLowerCase(Locale.ROOT);
        if (command.contains(":")) {
            command = command.substring(command.indexOf(':') + 1);
        }
        return command;
    }
}
