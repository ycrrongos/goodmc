package com.qqbridge;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class QqBridgeService {

    private final JavaPlugin plugin;
    private final QqBridgeConfig config;
    private final QqCursorStorage cursorStorage;
    private final QqBridgeClient client;
    private final ConcurrentLinkedQueue<String> sendQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean sending = new AtomicBoolean(false);

    private BukkitTask pollTask;
    private long consecutivePollFailures;
    private long consecutiveSendFailures;

    public QqBridgeService(JavaPlugin plugin, QqBridgeConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.cursorStorage = new QqCursorStorage(plugin);
        this.client = new QqBridgeClient(config);
    }

    public void start() {
        stop();
        if (!config.qqBridgeEnabled()) {
            plugin.getLogger().info("QQ 群服互联未启用。");
            return;
        }

        cursorStorage.load();
        plugin.getLogger().info(
                "QQ 群服互联已启动，API: " + config.qqBridgeApiUrl()
                        + "，cursor=" + cursorStorage.cursor()
                        + "，目标群: " + config.qqBridgeDefaultGroup()
        );

        long intervalTicks = Math.max(20L, config.qqBridgePollIntervalSeconds() * 20L);
        pollTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin,
                this::pollSafely,
                20L,
                intervalTicks
        );
    }

    public void stop() {
        if (pollTask != null) {
            pollTask.cancel();
            pollTask = null;
        }
        sendQueue.clear();
        sending.set(false);
    }

    public void enqueueSend(String message) {
        if (!config.qqBridgeEnabled() || message == null || message.isBlank()) {
            return;
        }
        sendQueue.offer(message);
        drainSendQueue();
    }

    private void drainSendQueue() {
        if (!sending.compareAndSet(false, true)) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::processSendQueue);
    }

    private void processSendQueue() {
        try {
            String message;
            while ((message = sendQueue.poll()) != null) {
                sendOnce(message);
            }
        } finally {
            sending.set(false);
            if (!sendQueue.isEmpty()) {
                drainSendQueue();
            }
        }
    }

    private void sendOnce(String message) {
        try {
            client.sendMessage(message, config.qqBridgeDefaultGroup());
            consecutiveSendFailures = 0;
        } catch (Exception exception) {
            consecutiveSendFailures++;
            if (consecutiveSendFailures == 1 || consecutiveSendFailures % 10 == 0) {
                plugin.getLogger().log(
                        Level.WARNING,
                        "QQ 消息发送失败（已连续 " + consecutiveSendFailures + " 次）: " + exception.getMessage(),
                        consecutiveSendFailures == 1 ? exception : null
                );
            }
        }
    }

    private void pollSafely() {
        try {
            pollOnce();
            consecutivePollFailures = 0;
        } catch (Exception exception) {
            consecutivePollFailures++;
            if (consecutivePollFailures == 1 || consecutivePollFailures % 20 == 0) {
                plugin.getLogger().log(
                        Level.WARNING,
                        "QQ 消息轮询失败（已连续 " + consecutivePollFailures + " 次）: " + exception.getMessage(),
                        consecutivePollFailures == 1 ? exception : null
                );
            }
        }
    }

    private void pollOnce() throws Exception {
        QqPollResponse pollResponse = client.pollMessages(
                cursorStorage.cursor(),
                config.qqBridgePollLimit()
        );

        if (pollResponse.messages().isEmpty()) {
            if (pollResponse.nextCursor() > cursorStorage.cursor()) {
                cursorStorage.save(pollResponse.nextCursor());
            }
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> broadcastMessages(pollResponse));
    }

    private void broadcastMessages(QqPollResponse pollResponse) {
        String groupFilter = config.qqBridgeReceiveGroupContains();
        for (QqMessage message : pollResponse.messages()) {
            if (message.message() == null || message.message().isBlank()) {
                continue;
            }
            if (groupFilter != null && !groupFilter.isBlank()) {
                String group = message.group();
                if (group == null || !group.contains(groupFilter)) {
                    continue;
                }
            }
            Bukkit.getServer().sendMessage(formatMessage(message));
        }
        cursorStorage.save(pollResponse.nextCursor());
    }

    private Component formatMessage(QqMessage message) {
        String sender = message.sender() == null || message.sender().isBlank() ? "未知" : message.sender();
        Component prefix;
        if (config.qqBridgeShowGroup() && message.group() != null && !message.group().isBlank()) {
            prefix = Component.text("[QQ/" + message.group() + "] ", NamedTextColor.AQUA);
        } else {
            prefix = Component.text("[QQ] ", NamedTextColor.AQUA);
        }
        return prefix
                .append(Component.text(sender, NamedTextColor.YELLOW))
                .append(Component.text(": ", NamedTextColor.GRAY))
                .append(Component.text(message.message(), NamedTextColor.WHITE));
    }
}
