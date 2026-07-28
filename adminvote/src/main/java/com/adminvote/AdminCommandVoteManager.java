package com.adminvote;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
    private final AdminVoteConfig config;
    private final Map<UUID, AdminCommandVote> votesByAdmin = new ConcurrentHashMap<>();
    private final Set<UUID> executingApproved = ConcurrentHashMap.newKeySet();

    public AdminCommandVoteManager(JavaPlugin plugin, AdminVoteConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public boolean shouldRequireVote(Player player) {
        return player.isOp() && !player.hasPermission(BYPASS_PERMISSION);
    }

    public boolean isExecutingApproved(UUID adminId) {
        return executingApproved.contains(adminId);
    }

    public Optional<AdminCommandVote> beginVote(Player admin, String commandLine) {
        int eligibleVoters = countEligibleVoters(admin);
        AdminCommandVote vote = new AdminCommandVote(admin.getUniqueId(), admin.getName(), commandLine);
        votesByAdmin.put(admin.getUniqueId(), vote);
        broadcastVoteRequest(vote, eligibleVoters);
        sendBeginMessage(admin, commandLine, eligibleVoters);
        scheduleExpiry(admin.getUniqueId());
        return Optional.of(vote);
    }

    public Optional<AdminCommandVote> getVote(UUID adminId) {
        AdminCommandVote vote = votesByAdmin.get(adminId);
        if (vote == null) {
            return Optional.empty();
        }
        if (vote.isExpired()) {
            votesByAdmin.remove(adminId);
            return Optional.empty();
        }
        return Optional.of(vote);
    }

    public void removeVote(UUID adminId) {
        votesByAdmin.remove(adminId);
    }

    public VoteResult recordVote(Player voter, UUID adminId, boolean accepted) {
        AdminCommandVote vote = votesByAdmin.get(adminId);
        if (vote == null || vote.isExpired()) {
            return VoteResult.NOT_FOUND;
        }
        if (voter.getUniqueId().equals(adminId)) {
            return VoteResult.SELF_VOTE;
        }
        if (vote.hasVoted(voter.getUniqueId())) {
            return VoteResult.ALREADY_VOTED;
        }

        return applyVote(vote, adminId, accepted, voter.getUniqueId());
    }

    public VoteResult recordConsoleVote(CommandSender sender, UUID adminId, boolean accepted) {
        if (!(sender instanceof ConsoleCommandSender)) {
            return VoteResult.CONSOLE_ONLY;
        }
        if (!accepted) {
            return VoteResult.CONSOLE_YES_ONLY;
        }

        AdminCommandVote vote = votesByAdmin.get(adminId);
        if (vote == null || vote.isExpired()) {
            return VoteResult.NOT_FOUND;
        }

        int eligible = countEligibleVoters(Bukkit.getPlayer(adminId));
        if (eligible > 0) {
            return VoteResult.CONSOLE_NO_PLAYERS_ONLY;
        }

        votesByAdmin.remove(adminId);
        executeApprovedCommand(adminId, vote);
        return VoteResult.PASSED;
    }

    private VoteResult applyVote(AdminCommandVote vote, UUID adminId, boolean accepted, UUID voterId) {
        boolean recorded = accepted ? vote.recordYes(voterId) : vote.recordNo(voterId);
        if (!recorded) {
            return VoteResult.ALREADY_VOTED;
        }

        Player admin = Bukkit.getPlayer(adminId);
        int eligible = countEligibleVoters(admin);
        int totalOnline = Bukkit.getOnlinePlayers().size();
        int required = config.requiredYesVotes(vote.command(), eligible, totalOnline);
        boolean allMustAgree = required >= eligible && eligible > 0;

        if (allMustAgree && !accepted) {
            votesByAdmin.remove(adminId);
            notifyVoteFailed(vote, false);
            return VoteResult.FAILED;
        }

        if (vote.acceptedCount() >= required) {
            votesByAdmin.remove(adminId);
            executeApprovedCommand(adminId, vote);
            return VoteResult.PASSED;
        }
        if (!allMustAgree && vote.rejectedCount() > eligible - required) {
            votesByAdmin.remove(adminId);
            notifyVoteFailed(vote, false);
            return VoteResult.FAILED;
        }

        broadcastVoteProgress(vote, eligible, required, allMustAgree);
        return VoteResult.RECORDED;
    }

    private int countEligibleVoters(Player admin) {
        if (admin == null) {
            return 0;
        }
        int count = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.getUniqueId().equals(admin.getUniqueId())) {
                count++;
            }
        }
        return count;
    }

    private void sendBeginMessage(Player admin, String commandLine, int eligibleVoters) {
        if (eligibleVoters == 0) {
            admin.sendMessage(
                    Component.text("已发起指令投票：", NamedTextColor.YELLOW)
                            .append(Component.text(commandLine, NamedTextColor.AQUA))
                            .append(Component.text("。当前无其他玩家在线，请让控制台执行 ", NamedTextColor.YELLOW))
                            .append(Component.text("/adminvote yes " + admin.getName(), NamedTextColor.GOLD))
            );
            return;
        }

        int totalOnline = Bukkit.getOnlinePlayers().size();
        int required = config.requiredYesVotes(commandLine, eligibleVoters, totalOnline);
        String rule = config.approvalRuleDescription(commandLine, eligibleVoters, totalOnline);
        admin.sendMessage(
                Component.text("已发起指令投票，", NamedTextColor.YELLOW)
                        .append(Component.text(rule, NamedTextColor.GOLD))
                        .append(Component.text("（", NamedTextColor.YELLOW))
                        .append(Component.text(required + "/" + eligibleVoters, NamedTextColor.AQUA))
                        .append(Component.text("）：", NamedTextColor.YELLOW))
                        .append(Component.text(commandLine, NamedTextColor.WHITE))
        );
    }

    private void broadcastVoteRequest(AdminCommandVote vote, int eligibleVoters) {
        if (eligibleVoters == 0) {
            plugin.getLogger().info(
                    "管理员 " + vote.adminName() + " 请求执行指令 " + vote.command()
                            + "，当前无其他玩家在线，控制台可执行: adminvote yes " + vote.adminName()
            );
            return;
        }

        int totalOnline = Bukkit.getOnlinePlayers().size();
        String requirement = config.approvalRuleDescription(vote.command(), eligibleVoters, totalOnline);
        Component acceptButton = voteButton("同意", NamedTextColor.GREEN, "/adminvote yes " + vote.adminName(), "点击同意该指令");
        Component denyButton = voteButton("拒绝", NamedTextColor.RED, "/adminvote no " + vote.adminName(), "点击拒绝该指令");

        Component message = Component.text("管理员 ", NamedTextColor.YELLOW)
                .append(Component.text(vote.adminName(), NamedTextColor.AQUA))
                .append(Component.text(" 请求执行指令：", NamedTextColor.YELLOW))
                .append(Component.text(vote.command(), NamedTextColor.WHITE))
                .append(Component.text(" （" + requirement + "）", NamedTextColor.GRAY))
                .append(Component.text(" "))
                .append(acceptButton)
                .append(Component.text(" "))
                .append(denyButton);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.getUniqueId().equals(vote.adminId())) {
                online.sendMessage(message);
            }
        }
    }

    private void broadcastVoteProgress(AdminCommandVote vote, int eligible, int required, boolean allMustAgree) {
        Component progress = Component.text("指令投票进度：", NamedTextColor.GRAY)
                .append(Component.text(vote.acceptedCount() + "/" + required, NamedTextColor.GOLD))
                .append(Component.text(allMustAgree ? " 已同意（全体通过制）" : " 已同意", NamedTextColor.GRAY))
                .append(Component.text("，", NamedTextColor.GRAY))
                .append(Component.text(vote.rejectedCount(), NamedTextColor.RED))
                .append(Component.text(" 已拒绝（", NamedTextColor.GRAY))
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
        if (admin == null) {
            return;
        }

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

    private void scheduleExpiry(UUID adminId) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            AdminCommandVote vote = votesByAdmin.get(adminId);
            if (vote != null && vote.isExpired()) {
                votesByAdmin.remove(adminId);
                notifyVoteFailed(vote, true);
            }
        }, 20L * 60L);
    }

    private static Component voteButton(String label, NamedTextColor color, String command, String hover) {
        return Component.text("[" + label + "]")
                .color(color)
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand(command))
                .hoverEvent(HoverEvent.showText(Component.text(hover, color)));
    }

    public void shutdown() {
        votesByAdmin.clear();
        executingApproved.clear();
    }

    public enum VoteResult {
        RECORDED,
        PASSED,
        FAILED,
        NOT_FOUND,
        ALREADY_VOTED,
        SELF_VOTE,
        CONSOLE_ONLY,
        CONSOLE_YES_ONLY,
        CONSOLE_NO_PLAYERS_ONLY
    }
}
