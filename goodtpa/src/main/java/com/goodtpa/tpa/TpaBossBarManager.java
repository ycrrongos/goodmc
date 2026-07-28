package com.goodtpa.tpa;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

final class TpaBossBarManager {

    private final Map<String, TrackedRequest> tracked = new HashMap<>();

    void startTracking(TpaRequest request, Player requester, Player target) {
        String key = key(request.requesterId(), request.targetId());
        stopTracking(request.requesterId(), request.targetId());

        TrackedRequest trackedRequest = new TrackedRequest(
                request,
                requester.getName(),
                target.getName(),
                BossBar.bossBar(
                        Component.empty(),
                        1.0f,
                        BossBar.Color.GREEN,
                        BossBar.Overlay.PROGRESS
                ),
                BossBar.bossBar(
                        Component.empty(),
                        1.0f,
                        BossBar.Color.GREEN,
                        BossBar.Overlay.PROGRESS
                )
        );
        tracked.put(key, trackedRequest);
        refresh(trackedRequest);
        requester.showBossBar(trackedRequest.requesterBar());
        target.showBossBar(trackedRequest.targetBar());
    }

    void stopTracking(UUID requesterId, UUID targetId) {
        TrackedRequest trackedRequest = tracked.remove(key(requesterId, targetId));
        if (trackedRequest == null) {
            return;
        }

        hideBar(requesterId, trackedRequest.requesterBar());
        hideBar(targetId, trackedRequest.targetBar());
    }

    void stopAllForPlayer(UUID playerId) {
        Iterator<Map.Entry<String, TrackedRequest>> iterator = tracked.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, TrackedRequest> entry = iterator.next();
            TrackedRequest trackedRequest = entry.getValue();
            if (!trackedRequest.request.requesterId().equals(playerId)
                    && !trackedRequest.request.targetId().equals(playerId)) {
                continue;
            }

            hideBar(trackedRequest.request.requesterId(), trackedRequest.requesterBar());
            hideBar(trackedRequest.request.targetId(), trackedRequest.targetBar());
            iterator.remove();
        }
    }

    void refreshAll() {
        for (TrackedRequest trackedRequest : tracked.values()) {
            refresh(trackedRequest);
        }
    }

    private void refresh(TrackedRequest trackedRequest) {
        TpaRequest request = trackedRequest.request;
        long remainingMs = request.remainingMillis();
        long timeoutMs = request.timeoutMillis();
        float progress = timeoutMs <= 0 ? 0.0f : Math.max(0.0f, (float) remainingMs / timeoutMs);
        int remainingSeconds = (int) Math.ceil(remainingMs / 1000.0);

        BossBar.Color color = progress > 0.5f
                ? BossBar.Color.GREEN
                : progress > 0.2f ? BossBar.Color.YELLOW : BossBar.Color.RED;

        String requesterName = trackedRequest.requesterName;
        String targetName = trackedRequest.targetName;
        String requesterTitle = switch (request.type()) {
            case TO_TARGET -> "等待 " + targetName + " 回应 · 剩余 " + remainingSeconds + " 秒";
            case TO_REQUESTER -> "等待 " + targetName + " 回应 · 剩余 " + remainingSeconds + " 秒";
        };
        String targetTitle = switch (request.type()) {
            case TO_TARGET -> requesterName + " 请求传送到你这里 · 剩余 " + remainingSeconds + " 秒";
            case TO_REQUESTER -> requesterName + " 请求你传送到他那里 · 剩余 " + remainingSeconds + " 秒";
        };

        updateBar(trackedRequest.requesterBar(), requesterTitle, progress, color);
        updateBar(trackedRequest.targetBar(), targetTitle, progress, color);
    }

    private static void updateBar(BossBar bar, String title, float progress, BossBar.Color color) {
        bar.name(Component.text(title, NamedTextColor.WHITE));
        bar.progress(progress);
        bar.color(color);
    }

    private static void hideBar(UUID playerId, BossBar bar) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            player.hideBossBar(bar);
        }
    }

    private static String key(UUID requesterId, UUID targetId) {
        return requesterId + ":" + targetId;
    }

    private record TrackedRequest(
            TpaRequest request,
            String requesterName,
            String targetName,
            BossBar requesterBar,
            BossBar targetBar
    ) {
    }
}
