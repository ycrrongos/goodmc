package com.goodvote.client.screen;

import com.goodvote.config.FilterMode;
import com.goodvote.network.GoodVotePackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin configuration screen for managing vote settings.
 */
public class ConfigScreen extends Screen {
    // Working copies of config data
    private FilterMode adminFilterMode;
    private List<String> adminCommandList;
    private boolean adminRequiresVote;
    private FilterMode playerFilterMode;
    private List<String> playerCommandList;
    private boolean playerRequestEnabled;
    private boolean configChangeRequiresVote;
    private int voteTimeoutSeconds;
    private int approvalPercent;
    private boolean afkDefaultAccept;
    private int afkThresholdSeconds;
    private boolean allowTargetSelectors;

    // UI state
    private String newCommandInput = "";
    private boolean editingAdminList = true; // true = admin, false = player
    private TextFieldWidget commandInputField;
    private TextFieldWidget timeoutField;
    private TextFieldWidget percentField;
    private TextFieldWidget afkThresholdField;
    private int scrollOffset = 0;

    public ConfigScreen(GoodVotePackets.ConfigData data) {
        super(Text.literal("投票配置"));
        this.adminFilterMode = FilterMode.fromId(data.adminFilterMode());
        this.adminCommandList = new ArrayList<>(data.adminCommandList());
        this.adminRequiresVote = data.adminRequiresVote();
        this.playerFilterMode = FilterMode.fromId(data.playerFilterMode());
        this.playerCommandList = new ArrayList<>(data.playerCommandList());
        this.playerRequestEnabled = data.playerRequestEnabled();
        this.configChangeRequiresVote = data.configChangeRequiresVote();
        this.voteTimeoutSeconds = data.voteTimeoutSeconds();
        this.approvalPercent = data.approvalPercent();
        this.afkDefaultAccept = data.afkDefaultAccept();
        this.afkThresholdSeconds = data.afkThresholdSeconds();
        this.allowTargetSelectors = data.allowTargetSelectors();
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int leftX = 20;
        int rightX = centerX + 20;
        int y = 30;
        int btnW = 120;
        int btnH = 20;

        // === Left panel: Admin Command Settings ===
        // Filter mode toggle
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("管理员命令: " + adminFilterMode.name()),
                btn -> { adminFilterMode = adminFilterMode.toggle(); updateButtons(); }
        ).dimensions(leftX, y, btnW + 40, btnH).build());

        // Toggle admin requires vote
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("需要投票: " + (adminRequiresVote ? "是" : "否")),
                btn -> { adminRequiresVote = !adminRequiresVote; updateButtons(); }
        ).dimensions(leftX, y + 25, btnW, btnH).build());

        // === Right panel: Player Request Settings ===
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("玩家请求: " + playerFilterMode.name()),
                btn -> { playerFilterMode = playerFilterMode.toggle(); updateButtons(); }
        ).dimensions(rightX, y, btnW + 40, btnH).build());

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("功能: " + (playerRequestEnabled ? "启用" : "禁用")),
                btn -> { playerRequestEnabled = !playerRequestEnabled; updateButtons(); }
        ).dimensions(rightX, y + 25, btnW, btnH).build());

        // === Bottom section: General Settings ===
        int bottomY = this.height - 80;

        // Allow target selectors
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("目标选择器: " + (allowTargetSelectors ? "允许" : "禁止")),
                btn -> { allowTargetSelectors = !allowTargetSelectors; updateButtons(); }
        ).dimensions(leftX, bottomY, btnW + 20, btnH).build());

        // Config change vote
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("配置变更投票: " + (configChangeRequiresVote ? "是" : "否")),
                btn -> { configChangeRequiresVote = !configChangeRequiresVote; updateButtons(); }
        ).dimensions(leftX + 160, bottomY, btnW + 20, btnH).build());

        // AFK default
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("AFK默认: " + (afkDefaultAccept ? "同意" : "拒绝")),
                btn -> { afkDefaultAccept = !afkDefaultAccept; updateButtons(); }
        ).dimensions(leftX + 340, bottomY, btnW + 20, btnH).build());

        // Command input field
        commandInputField = new TextFieldWidget(this.textRenderer, centerX - 80, bottomY + 25, 160, 18, Text.literal(""));
        commandInputField.setPlaceholder(Text.literal("输入命令..."));
        commandInputField.setMaxLength(256);
        this.addDrawableChild(commandInputField);

        // Add command button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("添加"),
                btn -> {
                    String cmd = commandInputField.getText().trim();
                    if (!cmd.isEmpty()) {
                        getCurrentList().add(cmd);
                        commandInputField.setText("");
                    }
                }
        ).dimensions(centerX + 85, bottomY + 25, 40, 18).build());

        // Remove last command button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("删除末尾"),
                btn -> {
                    List<String> list = getCurrentList();
                    if (!list.isEmpty()) list.remove(list.size() - 1);
                }
        ).dimensions(centerX + 130, bottomY + 25, 50, 18).build());

        // Save button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("保存配置").withColor(0x55FF55),
                btn -> saveConfig()
        ).dimensions(this.width - 100, bottomY, 80, btnH).build());

        // Cancel button
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("取消").withColor(0xFF5555),
                btn -> this.close()
        ).dimensions(this.width - 100, bottomY + 25, 80, btnH).build());

        // Tab toggle for viewing admin vs player command list
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal(editingAdminList ? "[管理员命令列表]" : "[玩家请求列表]"),
                btn -> { editingAdminList = !editingAdminList; updateButtons(); }
        ).dimensions(centerX - 60, y + 55, 120, btnH).build());
    }

    private List<String> getCurrentList() {
        return editingAdminList ? adminCommandList : playerCommandList;
    }

    private void updateButtons() {
        // Re-init to update button labels
        this.clearChildren();
        this.init();
    }

    private void saveConfig() {
        GoodVotePackets.ConfigData data = new GoodVotePackets.ConfigData(
                adminFilterMode.toId(), adminCommandList, adminRequiresVote,
                playerFilterMode.toId(), playerCommandList, playerRequestEnabled,
                configChangeRequiresVote, voteTimeoutSeconds, approvalPercent,
                afkDefaultAccept, afkThresholdSeconds, allowTargetSelectors
        );
        ClientPlayNetworking.send(new GoodVotePackets.ConfigSyncC2SPayload(data));
        this.close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int centerX = this.width / 2;
        List<String> currentList = getCurrentList();

        // Draw command list
        int listY = 100;
        int maxVisible = 8;
        context.drawText(this.textRenderer,
                Text.literal("命令列表 (" + currentList.size() + ")").withColor(0xFFAA00),
                20, listY - 12, 0xFFFFFF, false);

        for (int i = 0; i < Math.min(maxVisible, currentList.size()); i++) {
            int idx = i + scrollOffset;
            if (idx >= currentList.size()) break;
            String cmd = currentList.get(idx);
            int y = listY + i * 12;
            context.drawText(this.textRenderer,
                    Text.literal((idx + 1) + ". " + cmd),
                    25, y, 0xCCCCCC, false);
        }

        // Settings info on the right
        int infoX = centerX + 20;
        int infoY = 60;
        context.drawText(this.textRenderer, Text.literal("--- 通用设置 ---").withColor(0xFFAA00), infoX, infoY, 0xFFFFFF, false);
        context.drawText(this.textRenderer, Text.literal("投票超时: " + voteTimeoutSeconds + "秒"), infoX, infoY + 14, 0xCCCCCC, false);
        context.drawText(this.textRenderer, Text.literal("通过比例: " + approvalPercent + "%"), infoX, infoY + 28, 0xCCCCCC, false);
        context.drawText(this.textRenderer, Text.literal("AFK阈值: " + afkThresholdSeconds + "秒"), infoX, infoY + 42, 0xCCCCCC, false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        List<String> list = getCurrentList();
        int maxScroll = Math.max(0, list.size() - 8);
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - verticalAmount));
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
