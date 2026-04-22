package org.nia.niamod.models.gui.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;
import org.nia.niamod.models.gui.render.UiRect;
import org.nia.niamod.models.gui.screens.NiaClickGuiScreen;
import org.nia.niamod.models.territory.TerritoryNode;
import org.nia.niamod.models.territory.TerritoryResourceColors;
import org.nia.niamod.models.gui.theme.ClickGuiTheme;
import org.nia.niamod.render.Render2D;

import java.util.function.Consumer;

public class TerritoryWidget {
    public static final int CLIP_PADDING = 8;

    private static final int BORDER_DASH = 7;
    private static final int BORDER_GAP = 5;
    private static final int MAX_BORDER_DASHES_PER_SIDE = 6;
    private static final int LABEL_PADDING = 4;
    private static final double MARQUEE_PIXELS_PER_SECOND = 34.0;
    private static final long MARQUEE_PAUSE_MILLIS = 450L;
    private static final ItemStack CITY_ICON = new ItemStack(Items.EMERALD);

    private final TerritoryNode territory;
    private final boolean owned;
    private final Consumer<TerritoryWidget> selectionHandler;
    private UiRect bounds = new UiRect(0, 0, 1, 1);
    private int cachedNameMaxWidth = -1;
    private Component cachedName = Component.empty();
    private int cachedNameWidth;
    private Component fullName = Component.empty();
    private int fullNameWidth = -1;
    private boolean hoveredLastFrame;
    private long hoverStartedAt;

    public TerritoryWidget(TerritoryNode territory, boolean owned, Consumer<TerritoryWidget> selectionHandler) {
        this.territory = territory;
        this.owned = owned;
        this.selectionHandler = selectionHandler;
    }

    public static boolean intersects(UiRect rect, UiRect canvas, int padding) {
        return rect.right() >= canvas.x() - padding
                && rect.x() <= canvas.right() + padding
                && rect.bottom() >= canvas.y() - padding
                && rect.y() <= canvas.bottom() + padding;
    }

    public TerritoryNode territory() {
        return territory;
    }

    public void setBounds(UiRect bounds) {
        this.bounds = bounds;
    }

    public UiRect bounds() {
        return bounds;
    }

    public boolean owned() {
        return owned;
    }

    public void render(GuiGraphics g, Font font, int mouseX, int mouseY, ClickGuiTheme theme, long now, UiRect canvas, boolean headquarters, boolean selected) {
        if (!intersects(bounds, canvas, CLIP_PADDING)) {
            return;
        }

        if (!owned) {
            drawForeignTerritory(g, canvas);
            return;
        }

        boolean hovered = contains(mouseX, mouseY);
        int fillAlpha = hovered ? 62 : 42;
        int borderAlpha = headquarters || territory.isRainbow() ? 255 : hovered ? 245 : 215;

        if (headquarters) {
            Render2D.clippedRect(g, bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), canvas, Render2D.withAlpha(TerritoryResourceColors.headquarterColor(), fillAlpha));
        } else if (territory.isRainbow()) {
            drawRainbowFill(g, bounds, canvas, fillAlpha);
        } else {
            Render2D.clippedRect(g, bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), canvas, Render2D.withAlpha(territory.resourceColor(), fillAlpha));
        }

        if (selected) {
            int selectedAlpha = (int) Math.round(170 + Math.sin(System.currentTimeMillis() / 115.0) * 55);
            double phase = System.currentTimeMillis() / 42.0;
            if (headquarters) {
                int color = Render2D.withAlpha(TerritoryResourceColors.headquarterColor(), selectedAlpha);
                drawDashedBorder(g, bounds, canvas, phase, 2, (distance, length) -> color);
            } else if (territory.isRainbow()) {
                drawDashedBorder(g, bounds, canvas, phase, 2, (distance, length) -> TerritoryResourceColors.rainbowColor(distance / Math.max(1.0, length), selectedAlpha));
            } else {
                int color = Render2D.withAlpha(territory.resourceColor(), selectedAlpha);
                drawDashedBorder(g, bounds, canvas, phase, 2, (distance, length) -> color);
            }
        } else if (territory.heldUnderTenMinutes(now)) {
            int flashAlpha = (int) Math.round(138 + Math.sin(System.currentTimeMillis() / 135.0) * 72);
            double phase = System.currentTimeMillis() / 62.0;
            if (headquarters) {
                int color = Render2D.withAlpha(TerritoryResourceColors.headquarterColor(), flashAlpha);
                drawDashedBorder(g, bounds, canvas, phase, 1, (distance, length) -> color);
            } else if (territory.isRainbow()) {
                drawDashedBorder(g, bounds, canvas, phase, 1, (distance, length) -> TerritoryResourceColors.rainbowColor(distance / Math.max(1.0, length), flashAlpha));
            } else {
                int color = Render2D.withAlpha(territory.resourceColor(), flashAlpha);
                drawDashedBorder(g, bounds, canvas, phase, 1, (distance, length) -> color);
            }
        } else if (headquarters) {
            drawSolidBorder(g, bounds, canvas, Render2D.withAlpha(TerritoryResourceColors.headquarterColor(), borderAlpha), 2);
        } else if (territory.isRainbow()) {
            drawRainbowBorder(g, bounds, canvas, borderAlpha, 2);
        } else {
            drawSolidBorder(g, bounds, canvas, Render2D.withAlpha(territory.resourceColor(), borderAlpha), 2);
        }

        if (hovered && !hoveredLastFrame) {
            hoverStartedAt = now;
        }
        hoveredLastFrame = hovered;
        drawLabels(g, font, theme, canvas, hovered, now, headquarters);
        if (territory.isCity()) {
            drawCityMarker(g, canvas);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (owned && button == GLFW.GLFW_MOUSE_BUTTON_LEFT && contains(mouseX, mouseY)) {
            selectionHandler.accept(this);
            return true;
        }
        return false;
    }

    public double centerX() {
        return bounds.x() + bounds.width() / 2.0;
    }

    public double centerY() {
        return bounds.y() + bounds.height() / 2.0;
    }

    private void drawForeignTerritory(GuiGraphics g, UiRect canvas) {
        Render2D.clippedRect(g, bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), canvas, Render2D.withAlpha(0x000000, 120));
        drawSolidBorder(g, bounds, canvas, Render2D.withAlpha(0x000000, 235), 2);
    }

    private void drawLabels(GuiGraphics g, Font font, ClickGuiTheme theme, UiRect canvas, boolean hovered, long now, boolean headquarters) {
        UiRect clip = labelClip(canvas);
        if (clip == null) {
            return;
        }

        int nameWidth = fullNameWidth(font);
        int labelW = clip.width();
        int labelX = clip.x();
        int blockH = font.lineHeight * 2 + 2;
        int nameY = bounds.centerY() - blockH / 2;

        if (hovered && nameWidth > labelW) {
            drawMarqueeName(g, font, labelX, nameY, labelW, nameWidth, theme.textColor(), now, clip);
        } else {
            Component nameText = nameText(font, labelW);
            int nameX = labelX + (labelW - cachedNameWidth) / 2;
            drawClippedText(g, font, nameText, nameX, nameY, theme.textColor(), clip);
        }

        String fittedTag = fit(font, territory.tag(), labelW);
        Component tagText = NiaClickGuiScreen.styled(fittedTag);
        int tagWidth = font.width(tagText);
        int tagX = labelX + (labelW - tagWidth) / 2;
        int tagColor = headquarters
                ? TerritoryResourceColors.headquarterColor()
                : territory.isRainbow() ? TerritoryResourceColors.rainbowColor(0.72, 255) : Render2D.withAlpha(territory.resourceColor(), 255);
        drawClippedText(g, font, tagText, tagX, nameY + font.lineHeight + 2, tagColor, clip);
    }

    private void drawCityMarker(GuiGraphics g, UiRect canvas) {
        UiRect clip = intersection(bounds, canvas);
        if (clip == null) {
            return;
        }

        int iconX = bounds.width() >= 18 ? bounds.right() - 17 : bounds.centerX() - 8;
        int iconY = bounds.height() >= 18 ? bounds.y() + 1 : bounds.centerY() - 8;
        g.enableScissor(clip.x(), clip.y(), clip.right(), clip.bottom());
        try {
            g.renderItem(CITY_ICON, iconX, iconY);
        } finally {
            g.disableScissor();
        }
    }

    private void drawMarqueeName(GuiGraphics g, Font font, int labelX, int nameY, int labelW, int nameWidth, int textColor, long now, UiRect clip) {
        double scrollRange = Math.max(1.0, nameWidth - labelW);
        double scrollMillis = scrollRange / MARQUEE_PIXELS_PER_SECOND * 1000.0;
        double cycle = MARQUEE_PAUSE_MILLIS * 2.0 + scrollMillis;
        double elapsed = positiveModulo(now - hoverStartedAt, cycle);
        double offset;
        if (elapsed < MARQUEE_PAUSE_MILLIS) {
            offset = 0.0;
        } else if (elapsed < MARQUEE_PAUSE_MILLIS + scrollMillis) {
            offset = -scrollRange * ((elapsed - MARQUEE_PAUSE_MILLIS) / scrollMillis);
        } else {
            offset = -scrollRange;
        }

        int clipTop = Math.max(clip.y(), nameY - 1);
        int clipBottom = Math.min(clip.bottom(), nameY + font.lineHeight + 1);
        if (clipBottom <= clipTop) {
            return;
        }
        g.enableScissor(clip.x(), clipTop, clip.right(), clipBottom);
        try {
            g.drawString(font, fullNameText(font), labelX + (int) Math.round(offset), nameY, textColor, false);
        } finally {
            g.disableScissor();
        }
    }

    private void drawClippedText(GuiGraphics g, Font font, Component text, int x, int y, int color, UiRect clip) {
        int clipTop = Math.max(clip.y(), y - 1);
        int clipBottom = Math.min(clip.bottom(), y + font.lineHeight + 1);
        if (clipBottom <= clipTop) {
            return;
        }

        g.enableScissor(clip.x(), clipTop, clip.right(), clipBottom);
        try {
            g.drawString(font, text, x, y, color, false);
        } finally {
            g.disableScissor();
        }
    }

    private UiRect labelClip(UiRect canvas) {
        int inset = bounds.width() > LABEL_PADDING * 2 ? LABEL_PADDING : 0;
        int left = Math.max(bounds.x() + inset, canvas.x());
        int right = Math.min(bounds.right() - inset, canvas.right());
        int top = Math.max(bounds.y(), canvas.y());
        int bottom = Math.min(bounds.bottom(), canvas.bottom());
        if (right <= left || bottom <= top) {
            return null;
        }
        return new UiRect(left, top, right - left, bottom - top);
    }

    private Component nameText(Font font, int maxTextWidth) {
        if (maxTextWidth != cachedNameMaxWidth) {
            cachedName = NiaClickGuiScreen.styled(fit(font, territory.name(), maxTextWidth));
            cachedNameWidth = font.width(cachedName);
            cachedNameMaxWidth = maxTextWidth;
        }
        return cachedName;
    }

    private Component fullNameText(Font font) {
        if (fullNameWidth < 0) {
            fullName = NiaClickGuiScreen.styled(territory.name());
            fullNameWidth = font.width(fullName);
        }
        return fullName;
    }

    private int fullNameWidth(Font font) {
        if (fullNameWidth < 0) {
            fullNameText(font);
        }
        return fullNameWidth;
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= bounds.x() && mouseX <= bounds.right() && mouseY >= bounds.y() && mouseY <= bounds.bottom();
    }

    private String fit(Font font, String text, int maxWidth) {
        if (text == null) {
            return "";
        }
        if (font.width(NiaClickGuiScreen.styled(text)) <= maxWidth) {
            return text;
        }

        String trimmed = text;
        while (!trimmed.isEmpty() && font.width(NiaClickGuiScreen.styled(trimmed + "...")) > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed.isEmpty() ? "" : trimmed + "...";
    }

    private void drawSolidBorder(GuiGraphics g, UiRect bounds, UiRect canvas, int color, int thickness) {
        Render2D.clippedRect(g, bounds.x(), bounds.y(), bounds.right(), bounds.y() + thickness, canvas, color);
        Render2D.clippedRect(g, bounds.x(), bounds.bottom() - thickness, bounds.right(), bounds.bottom(), canvas, color);
        Render2D.clippedRect(g, bounds.x(), bounds.y() + thickness, bounds.x() + thickness, bounds.bottom() - thickness, canvas, color);
        Render2D.clippedRect(g, bounds.right() - thickness, bounds.y() + thickness, bounds.right(), bounds.bottom() - thickness, canvas, color);
    }

    private void drawRainbowBorder(GuiGraphics g, UiRect bounds, UiRect canvas, int alpha, int thickness) {
        if (intersection(bounds, canvas) == null) {
            return;
        }

        drawRainbowRect(g, bounds.x(), bounds.y(), bounds.right(), bounds.y() + thickness, bounds, canvas, alpha);
        drawRainbowRect(g, bounds.x(), bounds.bottom() - thickness, bounds.right(), bounds.bottom(), bounds, canvas, alpha);
        Render2D.clippedRect(g, bounds.x(), bounds.y() + thickness, bounds.x() + thickness, bounds.bottom() - thickness, canvas, TerritoryResourceColors.rainbowColor(0.0, alpha));
        Render2D.clippedRect(g, bounds.right() - thickness, bounds.y() + thickness, bounds.right(), bounds.bottom() - thickness, canvas, TerritoryResourceColors.rainbowColor(1.0, alpha));
    }

    private void drawDashedBorder(GuiGraphics g, UiRect bounds, UiRect canvas, double phase, int thickness, Render2D.DashColorProvider colorProvider) {
        Render2D.dashedLineClipped(g, bounds.x(), bounds.y(), bounds.right(), bounds.y(), canvas, thickness, phase, BORDER_DASH, BORDER_GAP, MAX_BORDER_DASHES_PER_SIDE, colorProvider);
        Render2D.dashedLineClipped(g, bounds.right(), bounds.y(), bounds.right(), bounds.bottom(), canvas, thickness, phase, BORDER_DASH, BORDER_GAP, MAX_BORDER_DASHES_PER_SIDE, colorProvider);
        Render2D.dashedLineClipped(g, bounds.right(), bounds.bottom(), bounds.x(), bounds.bottom(), canvas, thickness, phase, BORDER_DASH, BORDER_GAP, MAX_BORDER_DASHES_PER_SIDE, colorProvider);
        Render2D.dashedLineClipped(g, bounds.x(), bounds.bottom(), bounds.x(), bounds.y(), canvas, thickness, phase, BORDER_DASH, BORDER_GAP, MAX_BORDER_DASHES_PER_SIDE, colorProvider);
    }

    private void drawRainbowFill(GuiGraphics g, UiRect bounds, UiRect canvas, int alpha) {
        drawRainbowRect(g, bounds.x(), bounds.y(), bounds.right(), bounds.bottom(), bounds, canvas, alpha);
    }

    private void drawRainbowRect(GuiGraphics g, int left, int top, int right, int bottom, UiRect gradientBounds, UiRect canvas, int alpha) {
        int x1 = Math.max(left, canvas.x());
        int y1 = Math.max(top, canvas.y());
        int x2 = Math.min(right, canvas.right());
        int y2 = Math.min(bottom, canvas.bottom());
        if (x2 <= x1 || y2 <= y1) {
            return;
        }

        Render2D.horizontalMultiGradient(
                g,
                x1,
                y1,
                x2,
                y2,
                gradientBounds.x(),
                Math.max(1, gradientBounds.width()),
                TerritoryResourceColors.rainbowStops(alpha)
        );
    }

    private UiRect intersection(UiRect a, UiRect b) {
        int x1 = Math.max(a.x(), b.x());
        int y1 = Math.max(a.y(), b.y());
        int x2 = Math.min(a.right(), b.right());
        int y2 = Math.min(a.bottom(), b.bottom());
        if (x2 <= x1 || y2 <= y1) {
            return null;
        }
        return new UiRect(x1, y1, x2 - x1, y2 - y1);
    }

    private double positiveModulo(double value, double modulo) {
        double result = value % modulo;
        return result < 0 ? result + modulo : result;
    }

}
