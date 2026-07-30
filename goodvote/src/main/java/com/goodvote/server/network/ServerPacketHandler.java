package com.goodvote.server.network;

import com.goodvote.config.VoteConfig;
import com.goodvote.network.GoodVotePackets;
import com.goodvote.server.GoodVoteServer;
import com.goodvote.vote.VoteChoice;
import com.goodvote.vote.VoteManager;
import com.goodvote.vote.VoteSession;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Handles server-side packet registration and processing.
 */
public class ServerPacketHandler {

    public static void register() {
        // Register C2S payload types
        PayloadTypeRegistry.playC2S().register(
                GoodVotePackets.CastVotePayload.ID,
                GoodVotePackets.CastVotePayload.CODEC
        );
        PayloadTypeRegistry.playC2S().register(
                GoodVotePackets.ConfigSyncC2SPayload.ID,
                GoodVotePackets.ConfigSyncC2SPayload.CODEC
        );

        // Register S2C payload types
        PayloadTypeRegistry.playS2C().register(
                GoodVotePackets.OpenVotePayload.ID,
                GoodVotePackets.OpenVotePayload.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                GoodVotePackets.VoteUpdatePayload.ID,
                GoodVotePackets.VoteUpdatePayload.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                GoodVotePackets.VoteResultPayload.ID,
                GoodVotePackets.VoteResultPayload.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                GoodVotePackets.OpenConfigPayload.ID,
                GoodVotePackets.OpenConfigPayload.CODEC
        );
        PayloadTypeRegistry.playS2C().register(
                GoodVotePackets.ConfigSyncS2CPayload.ID,
                GoodVotePackets.ConfigSyncS2CPayload.CODEC
        );

        // Handle cast vote
        ServerPlayNetworking.registerGlobalReceiver(
                GoodVotePackets.CastVotePayload.ID,
                (payload, context) -> {
                    context.server().execute(() -> {
                        ServerPlayerEntity player = context.player();
                        GoodVoteServer server = GoodVoteServer.getInstance();
                        if (server == null) return;

                        VoteManager vm = server.getVoteManager();
                        VoteChoice choice = payload.getChoice();
                        VoteManager.VoteResult result = vm.castVote(
                                payload.sessionId(),
                                player.getUuid(),
                                player.getName().getString(),
                                choice
                        );

                        switch (result) {
                            case RECORDED -> player.sendMessage(
                                    Text.literal("已投票: " + choice.name()).styled(s -> s.withColor(0x55FF55)), true);
                            case PASSED -> player.sendMessage(
                                    Text.literal("投票已通过!").styled(s -> s.withColor(0x55FF55)), true);
                            case FAILED -> player.sendMessage(
                                    Text.literal("投票未通过").styled(s -> s.withColor(0xFF5555)), true);
                            case ALREADY_VOTED -> player.sendMessage(
                                    Text.literal("你已经投过票了").styled(s -> s.withColor(0xFFAA00)), true);
                            case NOT_FOUND -> player.sendMessage(
                                    Text.literal("投票不存在或已结束").styled(s -> s.withColor(0xFF5555)), true);
                            case NOT_ELIGIBLE -> player.sendMessage(
                                    Text.literal("你没有投票资格").styled(s -> s.withColor(0xFF5555)), true);
                            default -> {}
                        }
                    });
                }
        );

        // Handle config sync from client
        ServerPlayNetworking.registerGlobalReceiver(
                GoodVotePackets.ConfigSyncC2SPayload.ID,
                (payload, context) -> {
                    context.server().execute(() -> {
                        ServerPlayerEntity player = context.player();
                        if (!player.hasPermissionLevel(2)) {
                            player.sendMessage(Text.literal("你没有权限修改配置").styled(s -> s.withColor(0xFF5555)));
                            return;
                        }

                        GoodVoteServer server = GoodVoteServer.getInstance();
                        if (server == null) return;

                        VoteConfig newConfig = payload.data().toConfig();
                        if (server.getConfig().isConfigChangeRequiresVote()) {
                            server.startConfigChangeVote(player, "修改投票配置");
                        } else {
                            applyConfig(server, newConfig);
                            server.saveConfig();
                            player.sendMessage(Text.literal("配置已保存").styled(s -> s.withColor(0x55FF55)));
                        }
                    });
                }
        );
    }

    private static void applyConfig(GoodVoteServer server, VoteConfig c) {
        VoteConfig cfg = server.getConfig();
        cfg.setAdminCommandFilterMode(c.getAdminCommandFilterMode());
        cfg.setAdminCommandList(c.getAdminCommandList());
        cfg.setAdminCommandRequiresVote(c.isAdminCommandRequiresVote());
        cfg.setPlayerRequestFilterMode(c.getPlayerRequestFilterMode());
        cfg.setPlayerRequestCommandList(c.getPlayerRequestCommandList());
        cfg.setPlayerRequestEnabled(c.isPlayerRequestEnabled());
        cfg.setConfigChangeRequiresVote(c.isConfigChangeRequiresVote());
        cfg.setVoteTimeoutSeconds(c.getVoteTimeoutSeconds());
        cfg.setApprovalPercent(c.getApprovalPercent());
        cfg.setAfkDefaultAccept(c.isAfkDefaultAccept());
        cfg.setAfkThresholdSeconds(c.getAfkThresholdSeconds());
        cfg.setAllowTargetSelectors(c.isAllowTargetSelectors());
    }

    /**
     * Send open vote screen packet to a player and open the screen handler.
     */
    public static void sendOpenVoteScreen(ServerPlayerEntity player, VoteSession session, VoteConfig config) {
        boolean hasVoted = session.hasVoted(player.getUuid());
        int playerVoteChoice = hasVoted ? session.getVotes().get(player.getUuid()).choice().toId() : -1;

        GoodVotePackets.OpenVotePayload payload = new GoodVotePackets.OpenVotePayload(
                session.getId(),
                session.getType().toId(),
                session.getInitiatorName(),
                session.getContent(),
                session.getRemainingMs(),
                session.getEligibleCount(),
                session.getAcceptCount(),
                session.getRejectCount(),
                session.getAbstainCount(),
                hasVoted,
                playerVoteChoice
        );
        ServerPlayNetworking.send(player, payload);
    }
}
