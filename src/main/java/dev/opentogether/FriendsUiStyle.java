package dev.opentogether;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;

/**
 * Texture-free Minecraft-native UI: no external fonts, assets or shaders.
 * Colors follow a subdued charcoal/teal palette and all geometry is relative
 * to the active GUI scale.
 */
final class FriendsUiStyle {
    static final int TEXT = 0xFFF0F6FF;
    static final int MUTED = 0xFFA6B6C9;
    static final int ACCENT = 0xFF69E2BE;
    static final int WARNING = 0xFFFFC276;
    static final int PANEL = 0xEF111D2B;
    static final int FIELD = 0xFF1B3040;
    static final int SUCCESS_BG = 0xFF143D36;

    private FriendsUiStyle() {}

    static int panelWidth(int screenWidth) {
        return Math.max(210, Math.min(370, screenWidth - 24));
    }

    static int panelLeft(int screenWidth) {
        return (screenWidth - panelWidth(screenWidth)) / 2;
    }

    static int panelTop(int screenHeight) {
        return Math.max(4, (screenHeight - 225) / 2);
    }

    static void drawPanel(GuiGraphics g, int screenWidth, int screenHeight, String heading, String subtitle) {
        // Dark backdrop with a restrained highlight; no textures to load.
        g.fill(0, 0, screenWidth, screenHeight, 0xAA080E17);
        int x = panelLeft(screenWidth), y = panelTop(screenHeight), w = panelWidth(screenWidth);
        g.fill(x + 3, y + 4, x + w + 3, y + 229, 0x66000000);
        g.fill(x, y, x + w, y + 225, PANEL);
        g.fill(x, y, x + w, y + 2, ACCENT);
        g.fill(x, y + 2, x + 1, y + 225, 0xFF385265);
        g.fill(x + w - 1, y + 2, x + w, y + 225, 0xFF385265);
        g.fill(x, y + 224, x + w, y + 225, 0xFF385265);
        g.fill(x + 15, y + 57, x + w - 15, y + 58, 0xFF2C4356);
    }

    static void heading(GuiGraphics g, Font font, int screenWidth, int screenHeight,
                        String title, String subTitle) {
        int x = panelLeft(screenWidth), y = panelTop(screenHeight), w = panelWidth(screenWidth);
        text(g, font, title, x + 16, y + 15, TEXT, w - 56);
        text(g, font, subTitle, x + 16, y + 34, MUTED, w - 32);
    }

    static void text(GuiGraphics g, Font font, String value, int x, int y, int color, int maxWidth) {
        if (value == null) return;
        String printable = value;
        if (font.width(printable) > maxWidth) {
            while (!printable.isEmpty() && font.width(printable + "...") > maxWidth)
                printable = printable.substring(0, printable.length() - 1);
            printable += "...";
        }
        g.drawString(font, printable, x, y, color, false);
    }

    static void field(GuiGraphics g, int x, int y, int width, int height, boolean success) {
        g.fill(x, y, x + width, y + height, success ? SUCCESS_BG : FIELD);
        g.fill(x, y, x + 2, y + height, success ? ACCENT : 0xFF47647B);
    }
}
