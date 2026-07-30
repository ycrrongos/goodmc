package com.goodvote.client.screen.widget;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * A scrollable widget that displays a list of commands with add/remove capability.
 */
public class CommandListWidget {
    private final List<String> commands = new ArrayList<>();
    private int scrollOffset = 0;
    private int x, y, width, height;
    private int itemHeight = 12;
    private String title;

    public CommandListWidget(int x, int y, int width, int height, String title) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.title = title;
    }

    public void setCommands(List<String> newCommands) {
        commands.clear();
        commands.addAll(newCommands);
        scrollOffset = 0;
    }

    public List<String> getCommands() {
        return new ArrayList<>(commands);
    }

    public void addCommand(String cmd) {
        if (!cmd.isEmpty()) {
            commands.add(cmd);
        }
    }

    public void removeCommand(int index) {
        if (index >= 0 && index < commands.size()) {
            commands.remove(index);
        }
    }

    public void removeLast() {
        if (!commands.isEmpty()) {
            commands.remove(commands.size() - 1);
        }
    }

    public boolean mouseScrolled(double amount) {
        int maxScroll = Math.max(0, commands.size() - getMaxVisible());
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - amount));
        return true;
    }

    private int getMaxVisible() {
        return height / itemHeight;
    }

    public void render(DrawContext context, net.minecraft.client.font.TextRenderer textRenderer) {
        // Title
        context.drawText(textRenderer,
                Text.literal(title + " (" + commands.size() + ")").withColor(0xFFAA00),
                x, y - 12, 0xFFFFFF, false);

        // Border
        context.fill(x, y, x + width, y + height, 0xFF1A1A1A);
        context.drawBorder(x, y, width, height, 0xFF444444);

        // Items
        int maxVisible = getMaxVisible();
        for (int i = 0; i < Math.min(maxVisible, commands.size()); i++) {
            int idx = i + scrollOffset;
            if (idx >= commands.size()) break;
            int itemY = y + i * itemHeight + 2;
            String text = (idx + 1) + ". " + commands.get(idx);
            // Truncate if too long
            if (textRenderer.getWidth(Text.literal(text)) > width - 8) {
                text = text.substring(0, Math.max(3, (width - 8) / 6)) + "...";
            }
            context.drawText(textRenderer, Text.literal(text), x + 4, itemY, 0xCCCCCC, false);
        }

        // Scroll indicator
        if (commands.size() > maxVisible) {
            int scrollBarHeight = height - 4;
            int thumbHeight = Math.max(4, scrollBarHeight * maxVisible / commands.size());
            int thumbY = y + 2 + (scrollBarHeight - thumbHeight) * scrollOffset / Math.max(1, commands.size() - maxVisible);
            context.fill(x + width - 3, thumbY, x + width - 1, thumbY + thumbHeight, 0xFF666666);
        }
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
