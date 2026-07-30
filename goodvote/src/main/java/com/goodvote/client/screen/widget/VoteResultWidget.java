package com.goodvote.client.screen.widget;

import net.minecraft.client.gui.DrawContext;

/**
 * Widget that displays vote result information.
 */
public class VoteResultWidget {
    private int acceptCount;
    private int rejectCount;
    private int abstainCount;
    private int eligibleCount;
    private boolean concluded;
    private boolean passed;

    public VoteResultWidget() {}

    public void update(int acceptCount, int rejectCount, int abstainCount, int eligibleCount) {
        this.acceptCount = acceptCount;
        this.rejectCount = rejectCount;
        this.abstainCount = abstainCount;
        this.eligibleCount = eligibleCount;
    }

    public void conclude(boolean passed) {
        this.concluded = true;
        this.passed = passed;
    }

    public void reset() {
        this.concluded = false;
        this.passed = false;
        this.acceptCount = 0;
        this.rejectCount = 0;
        this.abstainCount = 0;
        this.eligibleCount = 0;
    }

    public void render(DrawContext context, int x, int y, int width) {
        int barHeight = 6;
        int total = Math.max(1, eligibleCount);

        // Background bar
        context.fill(x, y, x + width, y + barHeight, 0xFF333333);

        // Accept portion (green)
        int acceptWidth = (int) ((float) acceptCount / total * width);
        context.fill(x, y, x + acceptWidth, y + barHeight, 0xFF55FF55);

        // Reject portion (red)
        int rejectWidth = (int) ((float) rejectCount / total * width);
        context.fill(x + acceptWidth, y, x + acceptWidth + rejectWidth, y + barHeight, 0xFFFF5555);

        // Abstain portion (gray)
        int abstainWidth = (int) ((float) abstainCount / total * width);
        context.fill(x + acceptWidth + rejectWidth, y,
                x + acceptWidth + rejectWidth + abstainWidth, y + barHeight, 0xFF888888);
    }

    public boolean isConcluded() { return concluded; }
    public boolean isPassed() { return passed; }
}
