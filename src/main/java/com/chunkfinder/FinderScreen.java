package com.chunkfinder;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public class FinderScreen extends Screen {
    private static final int PINK = 0xFFF050F8;
    private static final int BG = 0xE6101014;
    private static final int DIM = 0xFF3A3A44;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GREY = 0xFFAAAAAA;

    private static final int W = 260;
    private static final int H = 266;
    private static final String[] SLIDER_NAMES = {"Simulation Distance", "Sensitivity", "Alpha"};
    private static final int[] SLIDER_MIN = {1, 1, 0};
    private static final int[] SLIDER_MAX = {16, 10, 100};

    private final ChunkFinderConfig cfg = ChunkFinderConfig.get();
    private final Marker[] markers = Marker.values();
    private int px;
    private int py;
    private int dragging = -1;

    public FinderScreen() {
        super(Component.literal("Chunk Finder"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void layout() {
        px = (width - W) / 2;
        py = Math.max(4, (height - H) / 2);
    }

    // ----- values -----
    private int sliderValue(int i) {
        return switch (i) {
            case 0 -> cfg.simulationDistance;
            case 1 -> cfg.sensitivity;
            default -> cfg.alpha;
        };
    }

    private void setSlider(int i, int v) {
        switch (i) {
            case 0 -> cfg.simulationDistance = v;
            case 1 -> cfg.sensitivity = v;
            default -> cfg.alpha = v;
        }
    }

    private boolean toggleValue(int i) {
        return switch (i) {
            case 0 -> cfg.susChunkFinder;
            case 1 -> cfg.lightFinder;
            default -> cfg.chatAlerts;
        };
    }

    private void flipToggle(int i) {
        switch (i) {
            case 0 -> cfg.susChunkFinder = !cfg.susChunkFinder;
            case 1 -> cfg.lightFinder = !cfg.lightFinder;
            default -> cfg.chatAlerts = !cfg.chatAlerts;
        }
        cfg.changed();
    }

    private static final String[] TOGGLE_NAMES = {"Sus Chunk Finder", "Light Finder", "Chat Alerts"};

    // ----- geometry -----
    private int toggleY(int i) {
        return py + 24 + i * 15;
    }

    private int sliderY(int i) {
        return py + 76 + i * 26;
    }

    private int boxX(int i) {
        return px + 8 + (i / 6) * ((W - 16) / 2);
    }

    private int boxY(int i) {
        return py + 170 + (i % 6) * 14;
    }

    // ----- drawing -----
    private void border(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        g.fill(x1, y1, x2, y1 + 1, color);
        g.fill(x1, y2 - 1, x2, y2, color);
        g.fill(x1, y1, x1 + 1, y2, color);
        g.fill(x2 - 1, y1, x2, y2, color);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        layout();
        g.fill(px, py, px + W, py + H, BG);
        border(g, px, py, px + W, py + H, PINK);
        g.drawString(font, "CHUNK FINDER", px + 8, py + 8, PINK, true);

        for (int i = 0; i < 3; i++) {
            int y = toggleY(i);
            boolean on = toggleValue(i);
            g.drawString(font, TOGGLE_NAMES[i], px + 8, y + 2, WHITE, true);
            int sx = px + W - 8 - 24;
            g.fill(sx, y + 1, sx + 24, y + 11, on ? PINK : DIM);
            int kx = on ? sx + 14 : sx + 2;
            g.fill(kx, y + 3, kx + 8, y + 9, WHITE);
        }

        for (int i = 0; i < 3; i++) {
            int y = sliderY(i);
            g.drawString(font, SLIDER_NAMES[i], px + 8, y, WHITE, true);
            String val = String.valueOf(sliderValue(i));
            g.drawString(font, val, px + W - 8 - font.width(val), y, PINK, true);
            int bx = px + 8;
            int bw = W - 16;
            g.fill(bx, y + 13, bx + bw, y + 16, DIM);
            float t = (sliderValue(i) - SLIDER_MIN[i]) / (float) (SLIDER_MAX[i] - SLIDER_MIN[i]);
            int fillX = bx + Math.round(t * bw);
            g.fill(bx, y + 13, fillX, y + 16, PINK);
            g.fill(fillX - 2, y + 10, fillX + 2, y + 19, PINK);
        }

        g.drawString(font, "Detect blocks", px + 8, py + 156, GREY, true);
        for (int i = 0; i < markers.length; i++) {
            int x = boxX(i);
            int y = boxY(i);
            boolean on = cfg.isOn(markers[i]);
            border(g, x, y, x + 9, y + 9, on ? PINK : DIM);
            if (on) {
                g.fill(x + 2, y + 2, x + 7, y + 7, PINK);
            }
            g.drawString(font, markers[i].label, x + 14, y + 1, WHITE, true);
        }

        String key = ChunkFinderClient.openKey != null
                ? ChunkFinderClient.openKey.getTranslatedKeyMessage().getString() : "Caps Lock";
        g.drawString(font, "Open/close: " + key, px + 8, py + H - 14, GREY, true);
    }

    // ----- input -----
    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void dragSlider(int i, double mx) {
        double t = Mth.clamp((mx - (px + 8)) / (W - 16), 0.0, 1.0);
        int v = SLIDER_MIN[i] + (int) Math.round(t * (SLIDER_MAX[i] - SLIDER_MIN[i]));
        setSlider(i, v);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        layout();
        if (event.button() == 0) {
            double mx = event.x();
            double my = event.y();
            for (int i = 0; i < 3; i++) {
                if (inside(mx, my, px + 8, toggleY(i), W - 16, 13)) {
                    flipToggle(i);
                    return true;
                }
            }
            for (int i = 0; i < 3; i++) {
                if (inside(mx, my, px + 8, sliderY(i) - 2, W - 16, 24)) {
                    dragging = i;
                    dragSlider(i, mx);
                    return true;
                }
            }
            for (int i = 0; i < markers.length; i++) {
                if (inside(mx, my, boxX(i), boxY(i) - 1, (W - 16) / 2 - 4, 12)) {
                    cfg.set(markers[i], !cfg.isOn(markers[i]));
                    cfg.changed();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging >= 0) {
            dragSlider(dragging, event.x());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging >= 0) {
            dragging = -1;
            cfg.changed();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_CAPS_LOCK) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        cfg.save();
        super.onClose();
    }
}
