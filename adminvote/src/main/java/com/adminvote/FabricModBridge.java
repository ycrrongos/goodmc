package com.adminvote;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Bridge to the GoodVote Fabric client mod.
 * Players with the Fabric mod installed receive vote data via plugin messaging
 * so they can see a GUI instead of chat buttons.
 * Also receives vote responses from the GUI.
 */
public final class FabricModBridge implements PluginMessageListener {

    public static final String CHANNEL = "goodvote:main";

    private final JavaPlugin plugin;
    private final Set<UUID> modPlayers = ConcurrentHashMap.newKeySet();
    private AdminCommandVoteManager voteManager;

    public FabricModBridge(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        var messenger = plugin.getServer().getMessenger();
        messenger.registerIncomingPluginChannel(plugin, CHANNEL, this);
        messenger.registerOutgoingPluginChannel(plugin, CHANNEL);
    }

    public void unregister() {
        var messenger = plugin.getServer().getMessenger();
        messenger.unregisterIncomingPluginChannel(plugin, CHANNEL, this);
        messenger.unregisterOutgoingPluginChannel(plugin, CHANNEL);
        modPlayers.clear();
    }

    public void setVoteManager(AdminCommandVoteManager voteManager) {
        this.voteManager = voteManager;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(CHANNEL)) return;
        String data = new String(message, StandardCharsets.UTF_8);
        if ("hello".equals(data)) {
            modPlayers.add(player.getUniqueId());
            plugin.getLogger().info("检测到 GoodVote Fabric 模组客户端: " + player.getName());
            return;
        }
        // Handle vote from GUI: "vote:ACCEPT", "vote:REJECT", "vote:ABSTAIN"
        if (data.startsWith("vote:") && voteManager != null) {
            String choiceStr = data.substring(5);
            VoteRecord.VoteChoice choice;
            try {
                choice = VoteRecord.VoteChoice.valueOf(choiceStr);
            } catch (IllegalArgumentException e) {
                return;
            }
            // Find the active vote this player is eligible for
            for (var vote : voteManager.getActiveVotes()) {
                if (vote.isEligible(player.getUniqueId()) && !vote.hasVoted(player.getUniqueId())) {
                    voteManager.recordVote(player, vote.adminId(), choice);
                    break;
                }
            }
        }
    }

    public boolean hasMod(UUID playerId) {
        return modPlayers.contains(playerId);
    }

    public void onPlayerQuit(UUID playerId) {
        modPlayers.remove(playerId);
    }

    /** Send vote start to mod client */
    public void sendVoteStart(Player player, AdminCommandVote vote, int required) {
        String json = "{\"type\":\"start\",\"admin\":\"" + esc(vote.adminName())
                + "\",\"cmd\":\"" + esc(vote.command())
                + "\",\"required\":" + required
                + ",\"eligible\":" + vote.eligibleCount()
                + ",\"timeout\":" + (vote.timeoutMs() / 1000)
                + ",\"vtype\":\"" + vote.type().name() + "\"}";
        sendPayload(player, json);
    }

    /** Send vote update to mod client */
    public void sendVoteUpdate(Player player, AdminCommandVote vote, int required) {
        String json = "{\"type\":\"update\",\"accept\":" + vote.acceptedCount()
                + ",\"reject\":" + vote.rejectedCount()
                + ",\"abstain\":" + vote.abstainedCount()
                + ",\"required\":" + required
                + ",\"eligible\":" + vote.eligibleCount()
                + ",\"remaining\":" + (vote.remainingMs() / 1000) + "}";
        sendPayload(player, json);
    }

    /** Send vote conclusion to mod client */
    public void sendVoteConclude(Player player, AdminCommandVote vote) {
        String json = "{\"type\":\"conclude\",\"passed\":" + vote.isPassed()
                + ",\"cmd\":\"" + esc(vote.command())
                + "\",\"admin\":\"" + esc(vote.adminName()) + "\"}";
        sendPayload(player, json);
    }

    private void sendPayload(Player player, String json) {
        if (!hasMod(player.getUniqueId())) return;
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        player.sendPluginMessage(plugin, CHANNEL, data);
    }

    /** Broadcast vote start to all eligible mod clients */
    public void broadcastVoteStart(AdminCommandVote vote, int required, UUID excludeAdmin) {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (p.getUniqueId().equals(excludeAdmin)) continue;
            if (hasMod(p.getUniqueId())) {
                sendVoteStart(p, vote, required);
            }
        }
    }

    /** Broadcast vote update to all mod clients */
    public void broadcastVoteUpdate(AdminCommandVote vote, int required) {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (hasMod(p.getUniqueId())) {
                sendVoteUpdate(p, vote, required);
            }
        }
    }

    /** Broadcast vote conclusion to all mod clients */
    public void broadcastVoteConclude(AdminCommandVote vote) {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (hasMod(p.getUniqueId())) {
                sendVoteConclude(p, vote);
            }
        }
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
