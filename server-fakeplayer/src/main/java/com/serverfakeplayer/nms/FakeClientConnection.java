package com.serverfakeplayer.nms;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import org.jetbrains.annotations.Nullable;

/**
 * No-op network connection for fake players (Carpet FakeClientConnection style).
 */
public final class FakeClientConnection extends Connection {

    public FakeClientConnection(PacketFlow flow) {
        super(flow);
        try {
            // Keep channel open so various vanilla checks pass
            var channelField = Connection.class.getDeclaredField("channel");
            channelField.setAccessible(true);
            channelField.set(this, new EmbeddedChannel());
        } catch (ReflectiveOperationException ignored) {
            // Best-effort; some Paper builds rename the field
        }
    }

    @Override
    public void setReadOnly() {
    }

    @Override
    public void send(Packet<?> packet, @Nullable ChannelFutureListener listener, boolean flush) {
    }

    @Override
    public void handleDisconnection() {
    }

    @Override
    public void setListenerForServerboundHandshake(PacketListener listener) {
    }

    @Override
    public <T extends PacketListener> void setupInboundProtocol(ProtocolInfo<T> protocolInfo, T listener) {
    }
}
