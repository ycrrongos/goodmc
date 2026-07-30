package com.adminvote;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class AdminCommandVoteManager {

    public static final String BYPASS_PERMISSION = "goodmc.bypass";

    private final JavaPlugin plugin;
    private AdminVoteConfig config;
    private final FabricModBridge modBridge;
    private VoteDialogManager dialogManager;
    private VoteInventoryGui voteGui;
    private final Map<UUID, AdminCommandVote> votesByAdmin = new ConcurrentHashMap<>();
    private final Set<UUID> executingApproved = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> lastActivity = new ConcurrentHashMap<>();

    public AdminCommandVoteManager(JavaPlugin plugin, AdminVoteConfig config, FabricModBridge modBridge) {
        this.plugin = plugin;
        this.config = config;
        this.modBridge = modBridge;
    }

    public void setDialogManager(VoteDialogManager dialogManager) {
        this.dialogManager = dialogManager;
    }

    public void setVoteGui(VoteInventoryGui voteGui) {
        this.voteGui = voteGui;
    }

    public void reloadConfig(AdminVoteConfig newConfig) {
        this.config = newConfig;
    }

    public AdminVoteConfig config() { return config; }

    // --- AFK tracking ---
    public void updateActivity(UUID playerId) {
        lastActivity.put(playerId, System.currentTimeMillis());
    }

    public boolean isPlayerAfk(UUID playerId) {
        Long last = lastActivity.get(playerId);
        if (last == null) return false;
        return System.currentTimeMillis() - last > config.afkThresholdSeconds() * 1000L;
    }

    // --- vote queries ---
    public boolean shouldRequireVote(Player player) {
        return player.isOp() && !player.hasPermission(BYPASS_PERMISSION);
    }

    public boolean isExecutingApproved(UUID adminId) {
        return executingApproved.contains(adminId);
    }

    public Optional<AdminCommandVote> getVote(UUID adminId) {
        AdminCommandVote vote = votesByAdmin.get(adminId);
        if (vote == null) return Optional.empty();
        if (vote.isConcluded() || vote.isExpired()) {
            votesByAdmin.remove(adminId);
            return Optional.empty();
        }
        return Optional.of(vote);
    }

    public Collection<AdminCommandVote> getActiveVotes() {
        return Collections.unmodifiableCollection(votesByAdmin.values());
    }

    public void removeVote(UUID adminId) {
        votesByAdmin.remove(adminId);
    }

    // --- begin vote ---
    public Optional<AdminCommandVote> beginVote(Player admin, String commandLine, AdminCommandVote.VoteType type) {
        if (!config.canEnterVoting(commandLine)) {
            admin.sendMessage(Component.text("该指令包含目标选择器，已被禁止发起投票。", NamedTextColor.RED));
            return Optional.empty();
        }

        Set<UUID> eligible = collectEligibleVoters(admin);
        long timeoutMs = config.voteTimeoutSeconds() * 1000L;
        AdminCommandVote vote = new AdminCommandVote(admin.getUniqueId(), admin.getName(),
                commandLine, type, timeoutMs, eligible);
        votesByAdmin.put(admin.getUniqueId(), vote);

        int totalOnline = Bukkit.getOnlinePlayers().size();
        int required = config.requiredYesVotes(commandLine, eligible.size(), totalOnline);

        broadcastVoteRequest(vote, eligible, required);
        sendBeginMessage(admin, commandLine, eligible.size(), required, totalOnline);

        // Fabric mod: send vote start to mod clients
        modBridge.broadcastVoteStart(vote, required, admin.getUniqueId());

        scheduleExpiry(admin.getUniqueId());
        return Optional.of(vote);
    }

    // --- record vote ---
    public VoteResult recordVote(Player voter, UUID adminId, VoteRecord.VoteChoice choice) {
        AdminCommandVote vote = votesByAdmin.get(adminId);
        if (vote == null || vote.isConcluded()) return VoteResult.NOT_FOUND;
        if (vote.isExpired()) {
            concludeExpiredVote(adminId, vote);
            return VoteResult.NOT_FOUND;
        }
        if (voter.getUniqueId().equals(adminId)) return VoteResult.SELF_VOTE;
        if (vote.hasVoted(voter.getUniqueId())) return VoteResult.ALREADY_VOTED;

        return applyVote(vote, adminId, choice, voter.getUniqueId(), voter.getName());
    }

    public VoteResult recordConsoleVote(CommandSender sender, UUID adminId) {
        if (!(sender instanceof ConsoleCommandSender)) return VoteResult.CONSOLE_ONLY;
        AdminCommandVote vote = votesByAdmin.get(adminId);
        if (vote == null || vote.isConcluded()) return VoteResult.NOT_FOUND;
        if (vote.isExpired()) {
            concludeExpiredVote(adminId, vote);
            return VoteResult.NOT_FOUND;
        }
        int eligible = countEligibleOnline(vote);
        if (eligible > 0) return VoteResult.CONSOLE_NO_PLAYERS_ONLY;

        votesByAdmin.remove(adminId);
        executeApprovedCommand(adminId, vote);
        return VoteResult.PASSED;
    }

    private VoteResult applyVote(AdminCommandVote vote, UUID adminId, VoteRecord.VoteChoice choice,
                                  UUID voterId, String voterName) {
        if (!vote.recordVote(voterId, voterName, choice)) return VoteResult.ALREADY_VOTED;

        Player admin = Bukkit.getPlayer(adminId);
        int eligible = vote.eligibleCount();
        int totalOnline = Bukkit.getOnlinePlayers().size();
        int required = config.requiredYesVotes(vote.command(), eligible, totalOnline);
        boolean allMustAgree = required >= eligible && eligible > 0;

        // one reject kills it when all-must-agree
        if (allMustAgree && choice == VoteRecord.VoteChoice.REJECT) {
            votesByAdmin.remove(adminId);
            vote.conclude(false);
            notifyVoteFailed(vote, false);
            closeDialogs(vote);
            modBridge.broadcastVoteConclude(vote);
            return VoteResult.FAILED;
        }

        if (vote.acceptedCount() >= required) {
            votesByAdmin.remove(adminId);
            vote.conclude(true);
            executeApprovedCommand(adminId, vote);
            closeDialogs(vote);
            modBridge.broadcastVoteConclude(vote);
            return VoteResult.PASSED;
        }

        if (vote.isDefinitivelyDecided(required)) {
            votesByAdmin.remove(adminId);
            vote.conclude(false);
            notifyVoteFailed(vote, false);
            closeDialogs(vote);
            modBridge.broadcastVoteConclude(vote);
            return VoteResult.FAILED;
        }

        broadcastVoteProgress(vote, eligible, required, allMustAgree);
        modBridge.broadcastVoteUpdate(vote, required);
        return VoteResult.RECORDED;
    }

    // --- helpers ---
    private Set<UUID> collectEligibleVoters(Player admin) {
        Set<UUID> eligible = new HashSet<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.getUniqueId().equals(admin.getUniqueId())) {
                eligible.add(online.getUniqueId());
            }
        }
        return eligible;
    }

    private int countEligibleOnline(AdminCommandVote vote) {
        int count = 0;
        for (UUID id : vote.eligibleVoters()) {
            if (Bukkit.getPlayer(id) != null) count++;
        }
        return count;
    }

    private void sendBeginMessage(Player admin, String commandLine, int eligibleVoters, int required, int totalOnline) {
        String rule = config.approvalRuleDescription(commandLine, eligibleVoters, totalOnline);
        if (eligibleVoters == 0) {
            admin.sendMessage(
                    Component.text("已发起指令投票：", NamedTextColor.YELLOW)
                            .append(Component.text(commandLine, NamedTextColor.AQUA))
                            .append(Component.text("。当前无其他玩家在线，请让控制台执行 ", NamedTextColor.YELLOW))
                            .append(Component.text("/adminvote yes " + admin.getName(), NamedTextColor.GOLD))
            );
            return;
        }
        admin.sendMessage(
                Component.text("已发起指令投票，", NamedTextColor.YELLOW)
                        .append(Component.text(rule, NamedTextColor.GOLD))
                        .append(Component.text("：", NamedTextColor.YELLOW))
                        .append(Component.text(commandLine, NamedTextColor.WHITE))
        );
    }

    private void broadcastVoteRequest(AdminCommandVote vote, Set<UUID> eligible, int required) {
        if (eligible.isEmpty()) {
            plugin.getLogger().info("管理员 " + vote.adminName() + " 请求执行指令 " + vote.command()
                    + "，当前无其他玩家在线，控制台可执行: adminvote yes " + vote.adminName());
            return;
        }

        String rule = config.approvalRuleDescription(vote.command(), eligible.size(), Bukkit.getOnlinePlayers().size());
        Component acceptBtn = voteButton("同意", NamedTextColor.GREEN, "/adminvote yes " + vote.adminName(), "点击同意");
        Component rejectBtn = voteButton("拒绝", NamedTextColor.RED, "/adminvote no " + vote.adminName(), "点击拒绝");
        Component abstainBtn = voteButton("弃权", NamedTextColor.GRAY, "/adminvote abstain " + vote.adminName(), "点击弃权");

        String typeLabel = vote.type() == AdminCommandVote.VoteType.PLAYER_REQUEST ? "玩家请求" : "管理员";
        Component message = Component.text(typeLabel + " ", NamedTextColor.YELLOW)
                .append(Component.text(vote.adminName(), NamedTextColor.AQUA))
                .append(Component.text(" 请求执行指令：", NamedTextColor.YELLOW))
                .append(Component.text(vote.command(), NamedTextColor.WHITE))
                .append(Component.text(" （" + rule + "）", NamedTextColor.GRAY))
                .append(Component.text(" "))
                .append(acceptBtn).append(Component.text(" "))
                .append(rejectBtn).append(Component.text(" "))
                .append(abstainBtn);

        for (UUID id : eligible) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            if (modBridge.hasMod(id)) continue; // Fabric mod players get GUI instead

            // Show Dialog GUI for vanilla clients
            if (dialogManager != null) {
                boolean dialogShown = dialogManager.showVoteDialog(p, vote, required);
                if (dialogShown) {
                    dialogManager.showVoteProgressBar(p, vote);
                } else {
                    // Fallback to chest GUI
                    if (voteGui != null) {
                        voteGui.showVoteGui(p, vote, required);
                        if (dialogManager != null) {
                            dialogManager.showVoteProgressBar(p, vote);
                        }
                    } else {
                        p.sendMessage(message);
                    }
                }
            } else if (voteGui != null) {
                voteGui.showVoteGui(p, vote, required);
            } else {
                p.sendMessage(message);
            }
        }
    }

    private void broadcastVoteProgress(AdminCommandVote vote, int eligible, int required, boolean allMustAgree) {
        Component progress = Component.text("指令投票进度：", NamedTextColor.GRAY)
                .append(Component.text(vote.acceptedCount() + "/" + required, NamedTextColor.GOLD))
                .append(Component.text(allMustAgree ? " 已同意（全体通过制）" : " 已同意", NamedTextColor.GRAY))
                .append(Component.text("，", NamedTextColor.GRAY))
                .append(Component.text(vote.rejectedCount(), NamedTextColor.RED))
                .append(Component.text(" 已拒绝，", NamedTextColor.GRAY))
                .append(Component.text(vote.abstainedCount(), NamedTextColor.GRAY))
                .append(Component.text(" 弃权（", NamedTextColor.GRAY))
                .append(Component.text(vote.command(), NamedTextColor.WHITE))
                .append(Component.text("）", NamedTextColor.GRAY));
        Bukkit.broadcast(progress);
    }

    private void notifyVoteFailed(AdminCommandVote vote, boolean expired) {
        String reason = expired ? "投票已超时。" : "同意人数不足，投票未通过。";
        Component message = Component.text("管理员 ", NamedTextColor.RED)
                .append(Component.text(vote.adminName(), NamedTextColor.AQUA))
                .append(Component.text(" 的指令投票失败：", NamedTextColor.RED))
                .append(Component.text(vote.command(), NamedTextColor.WHITE))
                .append(Component.text(" （" + reason + "）", NamedTextColor.GRAY));
        Bukkit.broadcast(message);

        Player admin = Bukkit.getPlayer(vote.adminId());
        if (admin != null) {
            admin.sendMessage(Component.text("指令投票未通过。", NamedTextColor.RED));
        }
    }

    private void executeApprovedCommand(UUID adminId, AdminCommandVote vote) {
        Player admin = Bukkit.getPlayer(adminId);
        if (admin == null) return;

        executingApproved.add(adminId);
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                Bukkit.dispatchCommand(admin, vote.command().substring(1));
                admin.sendMessage(
                        Component.text("指令投票已通过，已执行：", NamedTextColor.GREEN)
                                .append(Component.text(vote.command(), NamedTextColor.AQUA))
                );
                Bukkit.broadcast(
                        Component.text("管理员 ", NamedTextColor.GREEN)
                                .append(Component.text(vote.adminName(), NamedTextColor.AQUA))
                                .append(Component.text(" 的指令投票已通过。", NamedTextColor.GREEN))
                );
            } finally {
                executingApproved.remove(adminId);
            }
        });
    }

    private void concludeExpiredVote(UUID adminId, AdminCommandVote vote) {
        votesByAdmin.remove(adminId);
        // AFK handling: auto-accept for unvoted AFK players
        if (config.afkDefaultAccept()) {
            for (UUID id : vote.getUnvotedPlayers()) {
                Player p = Bukkit.getPlayer(id);
                if (p != null && isPlayerAfk(id)) {
                    vote.recordVote(id, p.getName(), VoteRecord.VoteChoice.ACCEPT);
                }
            }
        }
        int totalOnline = Bukkit.getOnlinePlayers().size();
        int required = config.requiredYesVotes(vote.command(), vote.eligibleCount(), totalOnline);
        boolean passed = vote.acceptedCount() >= required;
        vote.conclude(passed);
        if (passed) {
            executeApprovedCommand(adminId, vote);
        } else {
            notifyVoteFailed(vote, true);
        }
        closeDialogs(vote);
        modBridge.broadcastVoteConclude(vote);
    }

    private void scheduleExpiry(UUID adminId) {
        long ticks = 20L * (config.voteTimeoutSeconds() + 1);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            AdminCommandVote vote = votesByAdmin.get(adminId);
            if (vote != null && !vote.isConcluded() && vote.isExpired()) {
                concludeExpiredVote(adminId, vote);
            }
        }, ticks);
    }

    public void onPlayerQuit(UUID playerId) {
        lastActivity.remove(playerId);
        if (dialogManager != null) dialogManager.onPlayerQuit(playerId);
        modBridge.onPlayerQuit(playerId);
    }

    private void closeDialogs(AdminCommandVote vote) {
        if (voteGui != null) {
            voteGui.closeAllForVote(vote);
        }
        if (dialogManager != null) {
            dialogManager.closeAllForVote(vote);
            dialogManager.hideAllVoteProgressBars(vote);
        }
    }

    public void shutdown() {
        votesByAdmin.clear();
        executingApproved.clear();
        lastActivity.clear();
    }

    // --- status for /vote status ---
    public Component formatStatus() {
        var sessions = getActiveVotes();
        if (sessions.isEmpty()) {
            return Component.text("当前没有进行中的投票。", NamedTextColor.GRAY);
        }
        Component header = Component.text("=== 进行中的投票 ===", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true);
        Component body = Component.empty();
        for (AdminCommandVote v : sessions) {
            int totalOnline = Bukkit.getOnlinePlayers().size();
            int required = config.requiredYesVotes(v.command(), v.eligibleCount(), totalOnline);
            body = body.append(Component.newline())
                    .append(Component.text("[" + v.type().name() + "] ", NamedTextColor.GRAY))
                    .append(Component.text(v.adminName(), NamedTextColor.AQUA))
                    .append(Component.text(" - ", NamedTextColor.GRAY))
                    .append(Component.text(v.command(), NamedTextColor.WHITE))
                    .append(Component.text(" (同意:" + v.acceptedCount() + "/" + required
                            + " 拒绝:" + v.rejectedCount()
                            + " 弃权:" + v.abstainedCount()
                            + " 剩余:" + (v.remainingMs() / 1000) + "s)", NamedTextColor.GRAY));
        }
        return header.append(body);
    }

    private static Component voteButton(String label, NamedTextColor color, String command, String hover) {
        return Component.text("[" + label + "]")
                .color(color).decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand(command))
                .hoverEvent(HoverEvent.showText(Component.text(hover, color)));
    }

    public enum VoteResult {
        RECORDED, PASSED, FAILED, NOT_FOUND, ALREADY_VOTED, SELF_VOTE,
        CONSOLE_ONLY, CONSOLE_NO_PLAYERS_ONLY
    }
}
