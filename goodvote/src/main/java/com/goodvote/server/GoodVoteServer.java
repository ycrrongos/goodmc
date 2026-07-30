package com.goodvote.server;

import com.goodvote.GoodVoteMod;
import com.goodvote.config.VoteConfig;
import com.goodvote.data.ConfigStorage;
import com.goodvote.network.GoodVotePackets;
import com.goodvote.server.command.PlayerRequestCommand;
import com.goodvote.server.command.VoteCommand;
import com.goodvote.server.listener.ServerEventListener;
import com.goodvote.server.network.ServerPacketHandler;
import com.goodvote.vote.VoteManager;
import com.goodvote.vote.VoteSession;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.*;
import java.util.stream.Collectors;

public class GoodVoteServer implements DedicatedServerModInitializer {
    private static GoodVoteServer instance;

    private ConfigStorage configStorage;
    private VoteConfig config;
    private VoteManager voteManager;
    private MinecraftServer server;

    @Override
    public void onInitializeServer() {
        instance = this;

        // Load config
        configStorage = new ConfigStorage();
        config = configStorage.load();

        // Init vote manager
        voteManager = new VoteManager(config, new VoteManager.VoteCallback() {
            @Override
            public void onVoteStarted(VoteSession session) {}

            @Override
            public void onVoteUpdated(VoteSession session) {
                broadcastVoteUpdate(session);
            }

            @Override
            public void onVoteConcluded(VoteSession session, boolean passed) {
                broadcastVoteResult(session, passed);
                if (passed) {
                    executeApprovedAction(session);
                }
            }

            @Override
            public void openVoteScreen(ServerPlayerEntity player, VoteSession session) {
                ServerPacketHandler.sendOpenVoteScreen(player, session, config);
            }

            @Override
            public void broadcastMessage(String message) {
                if (server != null) {
                    server.getPlayerManager().broadcast(
                            Text.literal(message), false);
                }
            }

            @Override
            public void sendActionBar(ServerPlayerEntity player, String message) {
                player.sendMessage(Text.literal(message), true);
            }
        });

        // Register packets
        ServerPacketHandler.register();

        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            VoteCommand.register(dispatcher, this);
            PlayerRequestCommand.register(dispatcher, this);
        });

        // Register tick event
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (voteManager != null) voteManager.tick(world);
        });

        // Register event listeners
        ServerEventListener.register(this);

        GoodVoteMod.LOGGER.info("GoodVote server initialized");
    }

    public static GoodVoteServer getInstance() { return instance; }
    public VoteConfig getConfig() { return config; }
    public VoteManager getVoteManager() { return voteManager; }
    public ConfigStorage getConfigStorage() { return configStorage; }
    public MinecraftServer getServer() { return server; }

    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    public void reloadConfig() {
        config = configStorage.load();
        GoodVoteMod.LOGGER.info("GoodVote config reloaded");
    }

    public void saveConfig() {
        configStorage.save(config);
    }

    public VoteSession startAdminCommandVote(ServerPlayerEntity admin, String command) {
        if (!config.canEnterVoting(command)) {
            admin.sendMessage(Text.literal("该命令包含目标选择器，已被禁止投票").styled(s -> s.withColor(0xFF5555)));
            return null;
        }
        if (!config.requiresVoteForAdminCommand(command)) {
            return null;
        }

        Set<UUID> eligibleVoters = getEligibleVoters(admin);
        if (eligibleVoters.isEmpty()) {
            admin.sendMessage(Text.literal("没有其他在线玩家可以投票").styled(s -> s.withColor(0xFFAA00)));
            return null;
        }

        VoteSession session = voteManager.startVote(
                com.goodvote.vote.VoteType.COMMAND,
                admin.getUuid(), admin.getName().getString(),
                command, eligibleVoters
        );

        List<ServerPlayerEntity> eligiblePlayers = getEligiblePlayerList(eligibleVoters);
        voteManager.broadcastVoteStart(session, eligiblePlayers);
        return session;
    }

    public VoteSession startPlayerRequestVote(ServerPlayerEntity player, String command) {
        if (!config.canEnterVoting(command)) {
            player.sendMessage(Text.literal("该命令包含目标选择器，已被禁止投票").styled(s -> s.withColor(0xFF5555)));
            return null;
        }
        if (!config.isPlayerRequestAllowed(command)) {
            player.sendMessage(Text.literal("该命令不允许被请求执行").styled(s -> s.withColor(0xFF5555)));
            return null;
        }

        Set<UUID> eligibleVoters = server.getPlayerManager().getPlayerList()
                .stream()
                .filter(p -> !p.getUuid().equals(player.getUuid()))
                .map(ServerPlayerEntity::getUuid)
                .collect(Collectors.toSet());

        if (eligibleVoters.isEmpty()) {
            player.sendMessage(Text.literal("没有其他在线玩家可以投票").styled(s -> s.withColor(0xFFAA00)));
            return null;
        }

        VoteSession session = voteManager.startVote(
                com.goodvote.vote.VoteType.PLAYER_REQUEST,
                player.getUuid(), player.getName().getString(),
                command, eligibleVoters
        );

        List<ServerPlayerEntity> eligiblePlayers = getEligiblePlayerList(eligibleVoters);
        voteManager.broadcastVoteStart(session, eligiblePlayers);
        return session;
    }

    public VoteSession startConfigChangeVote(ServerPlayerEntity initiator, String changeDescription) {
        Set<UUID> eligibleVoters = server.getPlayerManager().getPlayerList()
                .stream()
                .filter(p -> !p.getUuid().equals(initiator.getUuid()))
                .map(ServerPlayerEntity::getUuid)
                .collect(Collectors.toSet());

        if (eligibleVoters.isEmpty()) {
            initiator.sendMessage(Text.literal("没有其他在线玩家可以投票").styled(s -> s.withColor(0xFFAA00)));
            return null;
        }

        VoteSession session = voteManager.startVote(
                com.goodvote.vote.VoteType.CONFIG_CHANGE,
                initiator.getUuid(), initiator.getName().getString(),
                changeDescription, eligibleVoters
        );

        List<ServerPlayerEntity> eligiblePlayers = getEligiblePlayerList(eligibleVoters);
        voteManager.broadcastVoteStart(session, eligiblePlayers);
        return session;
    }

    private Set<UUID> getEligibleVoters(ServerPlayerEntity initiator) {
        return server.getPlayerManager().getPlayerList()
                .stream()
                .filter(p -> !p.getUuid().equals(initiator.getUuid()))
                .map(ServerPlayerEntity::getUuid)
                .collect(Collectors.toSet());
    }

    private List<ServerPlayerEntity> getEligiblePlayerList(Set<UUID> eligibleVoters) {
        return server.getPlayerManager().getPlayerList()
                .stream()
                .filter(p -> eligibleVoters.contains(p.getUuid()))
                .collect(Collectors.toList());
    }

    private void broadcastVoteUpdate(VoteSession session) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (session.isEligible(player.getUuid())) {
                boolean hasVoted = session.hasVoted(player.getUuid());
                int choice = hasVoted ? session.getVotes().get(player.getUuid()).choice().toId() : -1;
                GoodVotePackets.VoteUpdatePayload update = new GoodVotePackets.VoteUpdatePayload(
                        session.getId(),
                        session.getAcceptCount(), session.getRejectCount(),
                        session.getAbstainCount(), session.getEligibleCount(),
                        session.getRemainingMs(), hasVoted, choice
                );
                ServerPlayNetworking.send(player, update);
            }
        }
    }

    private void broadcastVoteResult(VoteSession session, boolean passed) {
        GoodVotePackets.VoteResultPayload result = new GoodVotePackets.VoteResultPayload(
                session.getId(), passed, session.getContent(), session.getInitiatorName(),
                session.getType().toId(),
                session.getAcceptCount(), session.getRejectCount(),
                session.getAbstainCount(), session.getEligibleCount()
        );
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (session.isEligible(player.getUuid())) {
                ServerPlayNetworking.send(player, result);
            }
        }
    }

    private void executeApprovedAction(VoteSession session) {
        if (server == null) return;

        switch (session.getType()) {
            case COMMAND, PLAYER_REQUEST -> {
                server.execute(() -> {
                    String cmd = session.getContent();
                    if (cmd.startsWith("/")) cmd = cmd.substring(1);
                    ServerPlayerEntity initiator = server.getPlayerManager().getPlayer(session.getInitiatorId());
                    if (initiator != null) {
                        server.getCommandManager().executeWithPrefix(initiator.getCommandSource(), cmd);
                        initiator.sendMessage(Text.literal("投票已通过，命令已执行: " + session.getContent())
                                .styled(s -> s.withColor(0x55FF55)));
                    } else {
                        server.getCommandManager().executeWithPrefix(server.getCommandSource(), cmd);
                    }
                });
            }
            case CONFIG_CHANGE -> {
                // Config changes are handled by the caller
            }
        }
    }
}
