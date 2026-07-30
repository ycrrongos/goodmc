package com.goodvote.network;

import com.goodvote.config.FilterMode;
import com.goodvote.config.VoteConfig;
import com.goodvote.vote.VoteChoice;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * All custom payload definitions for GoodVote networking.
 */
public class GoodVotePackets {

    // ==================== Helper: String List Codec ====================
    public static void writeStringList(PacketByteBuf buf, List<String> list) {
        buf.writeVarInt(list.size());
        for (String s : list) buf.writeString(s);
    }

    public static List<String> readStringList(PacketByteBuf buf) {
        int size = buf.readVarInt();
        List<String> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) list.add(buf.readString(32767));
        return list;
    }

    // ==================== S2C: Open Vote Screen ====================
    public record OpenVotePayload(
            UUID sessionId, int voteType, String initiatorName, String content,
            long remainingMs, int eligibleCount, int acceptCount, int rejectCount,
            int abstainCount, boolean hasVoted, int playerVoteChoice
    ) implements CustomPayload {
        public static final CustomPayload.Id<OpenVotePayload> ID = new Id<>(Identifier.of("goodvote", "open_vote"));
        public static final PacketCodec<PacketByteBuf, OpenVotePayload> CODEC =
                new PacketCodec<>() {
                    @Override
                    public OpenVotePayload decode(PacketByteBuf buf) {
                        return new OpenVotePayload(
                                buf.readUuid(), buf.readVarInt(),
                                buf.readString(32767), buf.readString(32767),
                                buf.readLong(), buf.readVarInt(),
                                buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                                buf.readBoolean(), buf.readVarInt()
                        );
                    }
                    @Override
                    public void encode(PacketByteBuf buf, OpenVotePayload p) {
                        buf.writeUuid(p.sessionId); buf.writeVarInt(p.voteType);
                        buf.writeString(p.initiatorName); buf.writeString(p.content);
                        buf.writeLong(p.remainingMs); buf.writeVarInt(p.eligibleCount);
                        buf.writeVarInt(p.acceptCount); buf.writeVarInt(p.rejectCount);
                        buf.writeVarInt(p.abstainCount); buf.writeBoolean(p.hasVoted);
                        buf.writeVarInt(p.playerVoteChoice);
                    }
                };
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    // ==================== C2S: Cast Vote ====================
    public record CastVotePayload(UUID sessionId, int choice) implements CustomPayload {
        public static final CustomPayload.Id<CastVotePayload> ID = new Id<>(Identifier.of("goodvote", "cast_vote"));
        public static final PacketCodec<PacketByteBuf, CastVotePayload> CODEC =
                new PacketCodec<>() {
                    @Override
                    public CastVotePayload decode(PacketByteBuf buf) {
                        return new CastVotePayload(buf.readUuid(), buf.readVarInt());
                    }
                    @Override
                    public void encode(PacketByteBuf buf, CastVotePayload p) {
                        buf.writeUuid(p.sessionId); buf.writeVarInt(p.choice);
                    }
                };
        @Override public Id<? extends CustomPayload> getId() { return ID; }
        public VoteChoice getChoice() { return VoteChoice.fromId(choice); }
    }

    // ==================== S2C: Vote Update ====================
    public record VoteUpdatePayload(
            UUID sessionId, int acceptCount, int rejectCount, int abstainCount,
            int eligibleCount, long remainingMs, boolean hasVoted, int playerVoteChoice
    ) implements CustomPayload {
        public static final CustomPayload.Id<VoteUpdatePayload> ID = new Id<>(Identifier.of("goodvote", "vote_update"));
        public static final PacketCodec<PacketByteBuf, VoteUpdatePayload> CODEC =
                new PacketCodec<>() {
                    @Override
                    public VoteUpdatePayload decode(PacketByteBuf buf) {
                        return new VoteUpdatePayload(
                                buf.readUuid(), buf.readVarInt(), buf.readVarInt(),
                                buf.readVarInt(), buf.readVarInt(), buf.readLong(),
                                buf.readBoolean(), buf.readVarInt()
                        );
                    }
                    @Override
                    public void encode(PacketByteBuf buf, VoteUpdatePayload p) {
                        buf.writeUuid(p.sessionId);
                        buf.writeVarInt(p.acceptCount); buf.writeVarInt(p.rejectCount);
                        buf.writeVarInt(p.abstainCount); buf.writeVarInt(p.eligibleCount);
                        buf.writeLong(p.remainingMs);
                        buf.writeBoolean(p.hasVoted); buf.writeVarInt(p.playerVoteChoice);
                    }
                };
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    // ==================== S2C: Vote Result ====================
    public record VoteResultPayload(
            UUID sessionId, boolean passed, String content, String initiatorName,
            int voteType, int acceptCount, int rejectCount, int abstainCount, int eligibleCount
    ) implements CustomPayload {
        public static final CustomPayload.Id<VoteResultPayload> ID = new Id<>(Identifier.of("goodvote", "vote_result"));
        public static final PacketCodec<PacketByteBuf, VoteResultPayload> CODEC =
                new PacketCodec<>() {
                    @Override
                    public VoteResultPayload decode(PacketByteBuf buf) {
                        return new VoteResultPayload(
                                buf.readUuid(), buf.readBoolean(),
                                buf.readString(32767), buf.readString(32767),
                                buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                                buf.readVarInt(), buf.readVarInt()
                        );
                    }
                    @Override
                    public void encode(PacketByteBuf buf, VoteResultPayload p) {
                        buf.writeUuid(p.sessionId); buf.writeBoolean(p.passed);
                        buf.writeString(p.content); buf.writeString(p.initiatorName);
                        buf.writeVarInt(p.voteType); buf.writeVarInt(p.acceptCount);
                        buf.writeVarInt(p.rejectCount); buf.writeVarInt(p.abstainCount);
                        buf.writeVarInt(p.eligibleCount);
                    }
                };
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    // ==================== S2C: Open Config Screen ====================
    public record OpenConfigPayload() implements CustomPayload {
        public static final CustomPayload.Id<OpenConfigPayload> ID = new Id<>(Identifier.of("goodvote", "open_config"));
        public static final PacketCodec<PacketByteBuf, OpenConfigPayload> CODEC =
                PacketCodec.unit(new OpenConfigPayload());
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    // ==================== Config Data Shared Structure ====================
    public record ConfigData(
            int adminFilterMode, List<String> adminCommandList, boolean adminRequiresVote,
            int playerFilterMode, List<String> playerCommandList, boolean playerRequestEnabled,
            boolean configChangeRequiresVote, int voteTimeoutSeconds, int approvalPercent,
            boolean afkDefaultAccept, int afkThresholdSeconds, boolean allowTargetSelectors
    ) {
        public static void writeToBuf(PacketByteBuf buf, ConfigData d) {
            buf.writeVarInt(d.adminFilterMode);
            writeStringList(buf, d.adminCommandList);
            buf.writeBoolean(d.adminRequiresVote);
            buf.writeVarInt(d.playerFilterMode);
            writeStringList(buf, d.playerCommandList);
            buf.writeBoolean(d.playerRequestEnabled);
            buf.writeBoolean(d.configChangeRequiresVote);
            buf.writeVarInt(d.voteTimeoutSeconds);
            buf.writeVarInt(d.approvalPercent);
            buf.writeBoolean(d.afkDefaultAccept);
            buf.writeVarInt(d.afkThresholdSeconds);
            buf.writeBoolean(d.allowTargetSelectors);
        }

        public static ConfigData readFromBuf(PacketByteBuf buf) {
            return new ConfigData(
                    buf.readVarInt(), readStringList(buf), buf.readBoolean(),
                    buf.readVarInt(), readStringList(buf), buf.readBoolean(),
                    buf.readBoolean(), buf.readVarInt(), buf.readVarInt(),
                    buf.readBoolean(), buf.readVarInt(), buf.readBoolean()
            );
        }

        public VoteConfig toConfig() {
            VoteConfig c = new VoteConfig();
            c.setAdminCommandFilterMode(FilterMode.fromId(adminFilterMode));
            c.setAdminCommandList(adminCommandList);
            c.setAdminCommandRequiresVote(adminRequiresVote);
            c.setPlayerRequestFilterMode(FilterMode.fromId(playerFilterMode));
            c.setPlayerRequestCommandList(playerCommandList);
            c.setPlayerRequestEnabled(playerRequestEnabled);
            c.setConfigChangeRequiresVote(configChangeRequiresVote);
            c.setVoteTimeoutSeconds(voteTimeoutSeconds);
            c.setApprovalPercent(approvalPercent);
            c.setAfkDefaultAccept(afkDefaultAccept);
            c.setAfkThresholdSeconds(afkThresholdSeconds);
            c.setAllowTargetSelectors(allowTargetSelectors);
            return c;
        }

        public static ConfigData fromConfig(VoteConfig config) {
            return new ConfigData(
                    config.getAdminCommandFilterMode().toId(),
                    config.getAdminCommandList(),
                    config.isAdminCommandRequiresVote(),
                    config.getPlayerRequestFilterMode().toId(),
                    config.getPlayerRequestCommandList(),
                    config.isPlayerRequestEnabled(),
                    config.isConfigChangeRequiresVote(),
                    config.getVoteTimeoutSeconds(),
                    config.getApprovalPercent(),
                    config.isAfkDefaultAccept(),
                    config.getAfkThresholdSeconds(),
                    config.isAllowTargetSelectors()
            );
        }
    }

    // ==================== C2S: Config Sync ====================
    public record ConfigSyncC2SPayload(ConfigData data) implements CustomPayload {
        public static final CustomPayload.Id<ConfigSyncC2SPayload> ID = new Id<>(Identifier.of("goodvote", "config_sync_c2s"));
        public static final PacketCodec<PacketByteBuf, ConfigSyncC2SPayload> CODEC =
                new PacketCodec<>() {
                    @Override
                    public ConfigSyncC2SPayload decode(PacketByteBuf buf) {
                        return new ConfigSyncC2SPayload(ConfigData.readFromBuf(buf));
                    }
                    @Override
                    public void encode(PacketByteBuf buf, ConfigSyncC2SPayload p) {
                        ConfigData.writeToBuf(buf, p.data);
                    }
                };
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }

    // ==================== S2C: Config Sync ====================
    public record ConfigSyncS2CPayload(ConfigData data) implements CustomPayload {
        public static final CustomPayload.Id<ConfigSyncS2CPayload> ID = new Id<>(Identifier.of("goodvote", "config_sync_s2c"));
        public static final PacketCodec<PacketByteBuf, ConfigSyncS2CPayload> CODEC =
                new PacketCodec<>() {
                    @Override
                    public ConfigSyncS2CPayload decode(PacketByteBuf buf) {
                        return new ConfigSyncS2CPayload(ConfigData.readFromBuf(buf));
                    }
                    @Override
                    public void encode(PacketByteBuf buf, ConfigSyncS2CPayload p) {
                        ConfigData.writeToBuf(buf, p.data);
                    }
                };
        @Override public Id<? extends CustomPayload> getId() { return ID; }
    }
}
