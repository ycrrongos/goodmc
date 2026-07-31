package com.serverfakeplayer.nms;

import com.serverfakeplayer.action.EntityPlayerActionPack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import com.mojang.authlib.GameProfile;

public final class FakeServerPlayer extends ServerPlayer {

    private final EntityPlayerActionPack actionPack;
    private final String creatorName;

    public FakeServerPlayer(
            MinecraftServer server,
            ServerLevel level,
            GameProfile profile,
            ClientInformation clientInformation,
            String creatorName
    ) {
        super(server, level, profile, clientInformation);
        this.creatorName = creatorName;
        this.actionPack = new EntityPlayerActionPack(this);
    }

    public EntityPlayerActionPack actionPack() {
        return actionPack;
    }

    public String creatorName() {
        return creatorName;
    }

    public static FakeServerPlayer spawn(
            MinecraftServer server,
            ServerLevel level,
            GameProfile profile,
            Vec3 pos,
            float yaw,
            float pitch,
            GameType gameType,
            String creatorName
    ) {
        FakeServerPlayer fake = new FakeServerPlayer(
                server,
                level,
                profile,
                ClientInformation.createDefault(),
                creatorName
        );
        fake.setPos(pos.x, pos.y, pos.z);
        fake.setYRot(yaw);
        fake.setXRot(pitch);
        fake.setYHeadRot(yaw);

        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        server.getPlayerList().placeNewPlayer(new FakeClientConnection(PacketFlow.SERVERBOUND), fake, cookie);

        fake.unsetRemoved();
        if (fake.getAttribute(Attributes.STEP_HEIGHT) != null) {
            fake.getAttribute(Attributes.STEP_HEIGHT).setBaseValue(0.6F);
        }
        fake.gameMode.changeGameModeForPlayer(gameType);
        fake.getEntityData().set(DATA_PLAYER_MODE_CUSTOMISATION, (byte) 0x7f);
        fake.setHealth(20.0F);
        return fake;
    }

    public void discard(Component reason) {
        if (connection != null) {
            // Mark as removed first
            setRemoved(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            // Disconnect the fake connection
            try {
                connection.disconnect(reason);
            } catch (Exception ignored) {
                // Fake connection may throw
            }
        }
        // Remove from server player list
        level().getServer().getPlayerList().remove(this);
    }

    @Override
    public void tick() {
        if (level().getServer().getTickCount() % 10 == 0) {
            connection.resetPosition();
            level().getChunkSource().move(this);
        }
        try {
            super.tick();
            this.doTick();
            actionPack.onUpdate();
        } catch (NullPointerException ignored) {
            // Paper edge cases with fake connections
        }
    }

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        setHealth(20.0F);
        this.foodData = new FoodData();
        discard(getCombatTracker().getDeathMessage());
    }

    @Override
    public String getIpAddress() {
        return "127.0.0.1";
    }
}
