package org.nia.niamod.models.gui.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.nia.niamod.models.gui.render.UiRect;
import org.nia.niamod.models.gui.screens.NiaClickGuiScreen;
import org.nia.niamod.models.gui.territory.Resources;
import org.nia.niamod.models.gui.territory.ResourceKind;
import org.nia.niamod.models.gui.territory.TerritoryDefenseState;
import org.nia.niamod.models.gui.territory.TerritoryNode;
import org.nia.niamod.models.gui.territory.TerritoryResourceColors;
import org.nia.niamod.models.gui.territory.TowerControls;
import org.nia.niamod.models.gui.territory.TowerStat;
import org.nia.niamod.models.gui.territory.TreasuryLevel;
import org.nia.niamod.models.gui.theme.ClickGuiTheme;
import org.nia.niamod.render.Render2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

public class EcoMenu {
    private static final int PANEL_MARGIN = 8;
    private static final int PANEL_WIDTH = 268;
    private static final int PANEL_MAX_HEIGHT = 396;
    private static final int HEADER_HEIGHT = 24;
    private static final int ROW_HEIGHT = 18;
    private static final int BUTTON_SIZE = 16;

    private final List<DetailButton> buttons = new ArrayList<>();
    private boolean dragging;
    private boolean positioned;
    private int x;
    private int y;
    private double dragOffsetX;
    private double dragOffsetY;

    public void render(
            GuiGraphics g,
            Font font,
            int mouseX,
            int mouseY,
            ClickGuiTheme theme,
            TerritoryWidget selectedTerritory,
            String guildName,
            TowerControls controls,
            TerritoryDefenseState defenseState,
            int ownedConnections,
            int totalConnections,
            int screenWidth,
            int screenHeight,
            BiConsumer<TerritoryNode, TowerControls> controlsChanged,
            Runnable close
    ) {
        buttons.clear();
        if (selectedTerritory == null) {
            return;
        }

        TerritoryNode territory = selectedTerritory.territory();
        TerritoryDefenseState state = defenseState == null ? TerritoryDefenseState.EMPTY : defenseState;
        UiRect panel = panelBounds(screenWidth, screenHeight);
        if (panel.width() <= 0 || panel.height() <= 0) {
            return;
        }

        Render2D.shaderRoundedSurface(g, panel.x(), panel.y(), panel.width(), panel.height(), 4, theme.background(), panelBorderColor(theme));
        g.fill(panel.x() + 1, panel.y() + 1, panel.right() - 1, panel.y() + HEADER_HEIGHT, headerFillColor(theme));

        int contentX = panel.x() + 10;
        int contentW = panel.width() - 20;
        int right = contentX + contentW;
        int rowY = panel.y() + 7;

        UiRect closeButton = new UiRect(panel.right() - 27, panel.y() + 5, 18, 18);
        drawPlainButton(g, font, closeButton, "X", closeButton.contains(mouseX, mouseY), theme);
        buttons.add(new DetailButton(closeButton, close));

        drawFittedString(g, font, territory.name(), contentX, rowY + 2, Math.max(1, closeButton.x() - contentX - 8), theme.textColor());
        rowY += 24;
        rowY = drawResourceSummary(g, font, territory.resources(), contentX, rowY, contentW, theme);
        rowY += 4;
        drawDivider(g, panel, rowY, theme);
        rowY += 8;

        int guildTagW = Math.min(contentW, Math.max(36, font.width(guildName) + 8));
        g.fill(contentX, rowY - 2, contentX + guildTagW, rowY + font.lineHeight + 2, Render2D.withAlpha(theme.accentColor(), 230));
        drawFittedString(g, font, guildName, contentX + 4, rowY, guildTagW - 8, theme.textColor());
        rowY += 18;

        drawFittedString(g, font, "Time held: " + formatHeldTime(territory.acquiredMillis()), contentX, rowY, contentW, theme.secondaryText());
        rowY += 12;
        TreasuryLevel treasury = state.treasury();
        drawFittedString(g, font, "Treasury: ", contentX, rowY, contentW, theme.secondaryText());
        int treasuryPrefixW = font.width(NiaClickGuiScreen.styled("Treasury: "));
        drawFittedString(g, font, treasury.label(), contentX + treasuryPrefixW, rowY, Math.max(1, contentW - treasuryPrefixW), treasury.color(theme));
        rowY += 17;
        drawDivider(g, panel, rowY, theme);
        rowY += 9;

        drawFittedString(g, font, "Tower", contentX, rowY, contentW, theme.textColor());
        rowY += 18;
        rowY = drawHqRow(g, font, contentX, rowY, contentW, controls, mouseX, mouseY, theme, () -> {
            controls.toggleHq();
            controlsChanged.accept(territory, controls);
        });
        rowY = drawTowerStatRow(g, font, territory, controls, state, TowerStat.DAMAGE, contentX, right, rowY, mouseX, mouseY, theme, controlsChanged);
        rowY = drawTowerStatRow(g, font, territory, controls, state, TowerStat.ATTACK_SPEED, contentX, right, rowY, mouseX, mouseY, theme, controlsChanged);
        rowY = drawTowerStatRow(g, font, territory, controls, state, TowerStat.HEALTH, contentX, right, rowY, mouseX, mouseY, theme, controlsChanged);
        rowY = drawTowerStatRow(g, font, territory, controls, state, TowerStat.DEFENSE, contentX, right, rowY, mouseX, mouseY, theme, controlsChanged);
        rowY = drawTowerStatRow(g, font, territory, controls, state, TowerStat.AURA, contentX, right, rowY, mouseX, mouseY, theme, controlsChanged);
        rowY = drawTowerStatRow(g, font, territory, controls, state, TowerStat.VOLLEY, contentX, right, rowY, mouseX, mouseY, theme, controlsChanged);
        drawConnectionRow(g, font, ownedConnections, totalConnections, contentX, right, rowY, theme);
        rowY += ROW_HEIGHT + 5;
        drawDivider(g, panel, rowY, theme);
        rowY += 9;

        drawFittedString(g, font, "Avg DPS: " + formatStat(state.averageDps()), contentX, rowY, contentW, theme.secondaryText());
        rowY += 12;
        drawFittedString(g, font, "EHP: " + formatStat(state.effectiveHealth()), contentX, rowY, contentW, theme.secondaryText());
        rowY += 12;
        drawFittedString(g, font, "Defense: " + formatStat(state.defenseSummary()), contentX, rowY, contentW, theme.secondaryText());
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, int screenWidth, int screenHeight) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }

        UiRect panel = panelBounds(screenWidth, screenHeight);
        if (!panel.contains(mouseX, mouseY)) {
            return false;
        }

        for (DetailButton detailButton : buttons) {
            if (detailButton.bounds().contains(mouseX, mouseY)) {
                detailButton.onClick().run();
                return true;
            }
        }
        if (headerBounds(panel).contains(mouseX, mouseY)) {
            dragging = true;
            dragOffsetX = mouseX - panel.x();
            dragOffsetY = mouseY - panel.y();
            return true;
        }
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        if (!dragging) {
            return false;
        }

        UiRect panel = panelBounds(screenWidth, screenHeight);
        x = clampInt((int) Math.round(mouseX - dragOffsetX), PANEL_MARGIN, Math.max(PANEL_MARGIN, screenWidth - panel.width() - PANEL_MARGIN));
        y = clampInt((int) Math.round(mouseY - dragOffsetY), PANEL_MARGIN, Math.max(PANEL_MARGIN, screenHeight - panel.height() - PANEL_MARGIN));
        return true;
    }

    public boolean mouseReleased() {
        if (!dragging) {
            return false;
        }
        dragging = false;
        return true;
    }

    public void stopDragging() {
        dragging = false;
    }

    public void ensurePosition(int screenWidth, int screenHeight) {
        int panelW = Math.min(PANEL_WIDTH, Math.max(1, screenWidth - PANEL_MARGIN * 2));
        int panelH = Math.min(PANEL_MAX_HEIGHT, Math.max(1, screenHeight - PANEL_MARGIN * 2));
        ensurePosition(screenWidth, screenHeight, panelW, panelH);
    }

    private UiRect panelBounds(int screenWidth, int screenHeight) {
        int panelW = Math.min(PANEL_WIDTH, Math.max(1, screenWidth - PANEL_MARGIN * 2));
        int panelH = Math.min(PANEL_MAX_HEIGHT, Math.max(1, screenHeight - PANEL_MARGIN * 2));
        ensurePosition(screenWidth, screenHeight, panelW, panelH);
        return new UiRect(x, y, panelW, panelH);
    }

    private void ensurePosition(int screenWidth, int screenHeight, int panelW, int panelH) {
        if (!positioned) {
            x = screenWidth - panelW - PANEL_MARGIN;
            y = PANEL_MARGIN;
            positioned = true;
        }

        x = clampInt(x, PANEL_MARGIN, Math.max(PANEL_MARGIN, screenWidth - panelW - PANEL_MARGIN));
        y = clampInt(y, PANEL_MARGIN, Math.max(PANEL_MARGIN, screenHeight - panelH - PANEL_MARGIN));
    }

    private UiRect headerBounds(UiRect panel) {
        return new UiRect(panel.x(), panel.y(), panel.width(), HEADER_HEIGHT);
    }

    private int drawResourceSummary(GuiGraphics g, Font font, Resources resources, int x, int y, int maxWidth, ClickGuiTheme theme) {
        int startY = y;
        y = drawResourceRate(g, font, x, y, maxWidth, TerritoryResourceColors.cityColor(), resources.emeralds(), "emeralds", theme);
        y = drawResourceRate(g, font, x, y, maxWidth, TerritoryResourceColors.configuredColor(ResourceKind.CROPS), resources.crops(), "crops", theme);
        y = drawResourceRate(g, font, x, y, maxWidth, TerritoryResourceColors.configuredColor(ResourceKind.WOOD), resources.wood(), "wood", theme);
        y = drawResourceRate(g, font, x, y, maxWidth, TerritoryResourceColors.configuredColor(ResourceKind.ORE), resources.ore(), "ore", theme);
        y = drawResourceRate(g, font, x, y, maxWidth, TerritoryResourceColors.configuredColor(ResourceKind.FISH), resources.fish(), "fish", theme);
        if (y == startY) {
            drawFittedString(g, font, "No resource production", x, y, maxWidth, theme.trinaryText());
            y += 12;
        }
        return y;
    }

    private int drawResourceRate(GuiGraphics g, Font font, int x, int y, int maxWidth, int color, int amount, String label, ClickGuiTheme theme) {
        if (amount <= 0) {
            return y;
        }
        g.fill(x + 2, y + 3, x + 10, y + 9, color);
        drawFittedString(g, font, "+" + formatNumber(amount) + " " + label + " per hour", x + 15, y, Math.max(1, maxWidth - 15), theme.secondaryText());
        return y + 12;
    }

    private int drawHqRow(GuiGraphics g, Font font, int x, int y, int maxWidth, TowerControls controls, int mouseX, int mouseY, ClickGuiTheme theme, Runnable onClick) {
        UiRect box = new UiRect(x, y + 2, 14, 14);
        boolean hovered = new UiRect(x, y, Math.min(maxWidth, 72), ROW_HEIGHT).contains(mouseX, mouseY);
        g.fill(box.x(), box.y(), box.right(), box.bottom(), controls.hq() ? Render2D.withAlpha(theme.accentColor(), 210) : controlFillColor(theme, hovered));
        if (controls.hq()) {
            drawFittedString(g, font, "v", box.x() + 4, box.y() + 3, 9, theme.textColor());
        }
        if (hovered) {
            Render2D.outline(g, box, Render2D.withAlpha(theme.accentColor(), 150));
        }
        drawFittedString(g, font, "HQ", x + 20, y + 4, Math.max(1, maxWidth - 20), theme.textColor());
        buttons.add(new DetailButton(new UiRect(x, y, Math.min(maxWidth, 72), ROW_HEIGHT), onClick));
        return y + ROW_HEIGHT;
    }

    private int drawTowerStatRow(
            GuiGraphics g,
            Font font,
            TerritoryNode territory,
            TowerControls controls,
            TerritoryDefenseState defenseState,
            TowerStat stat,
            int x,
            int right,
            int y,
            int mouseX,
            int mouseY,
            ClickGuiTheme theme,
            BiConsumer<TerritoryNode, TowerControls> controlsChanged
    ) {
        int controlsW = 70;
        String label = stat.label() + ": " + formatStat(defenseState.towerStat(stat));
        drawFittedString(g, font, label, x, y + 4, Math.max(1, right - x - controlsW - 8), theme.textColor());

        int plusX = right - BUTTON_SIZE;
        int valueX = plusX - 25;
        int minusX = valueX - BUTTON_SIZE - 6;
        UiRect minus = new UiRect(minusX, y + 2, BUTTON_SIZE, BUTTON_SIZE);
        UiRect plus = new UiRect(plusX, y + 2, BUTTON_SIZE, BUTTON_SIZE);
        UiRect value = new UiRect(valueX, y + 2, 22, BUTTON_SIZE);
        drawPlainButton(g, font, minus, "-", minus.contains(mouseX, mouseY), theme);
        drawValuePill(g, font, value, Integer.toString(controls.level(stat)), theme);
        drawPlainButton(g, font, plus, "+", plus.contains(mouseX, mouseY), theme);
        buttons.add(new DetailButton(minus, () -> {
            controls.adjust(stat, -1);
            controlsChanged.accept(territory, controls);
        }));
        buttons.add(new DetailButton(plus, () -> {
            controls.adjust(stat, 1);
            controlsChanged.accept(territory, controls);
        }));
        return y + ROW_HEIGHT;
    }

    private void drawConnectionRow(GuiGraphics g, Font font, int owned, int total, int x, int right, int y, ClickGuiTheme theme) {
        drawFittedString(g, font, "Connections: " + owned + "/" + total, x, y + 4, Math.max(1, right - x - 34), theme.textColor());
        drawValuePill(g, font, new UiRect(right - 22, y + 2, 22, BUTTON_SIZE), Integer.toString(owned), theme);
    }

    private void drawDivider(GuiGraphics g, UiRect panel, int y, ClickGuiTheme theme) {
        g.fill(panel.x(), y, panel.right(), y + 1, dividerColor(theme));
    }

    private void drawPlainButton(GuiGraphics g, Font font, UiRect rect, String label, boolean hovered, ClickGuiTheme theme) {
        g.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), controlFillColor(theme, hovered));
        int color = hovered ? theme.textColor() : theme.secondaryText();
        Component text = NiaClickGuiScreen.styled(label);
        g.drawString(font, text, rect.x() + (rect.width() - font.width(text)) / 2, rect.y() + (rect.height() - font.lineHeight) / 2 + 1, color, false);
    }

    private void drawValuePill(GuiGraphics g, Font font, UiRect rect, String value, ClickGuiTheme theme) {
        g.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), valueFillColor(theme));
        Component text = NiaClickGuiScreen.styled(value);
        g.drawString(font, text, rect.x() + (rect.width() - font.width(text)) / 2, rect.y() + (rect.height() - font.lineHeight) / 2 + 1, theme.textColor(), false);
    }

    private void drawFittedString(GuiGraphics g, Font font, String text, int x, int y, int maxWidth, int color) {
        if (maxWidth <= 0) {
            return;
        }
        g.drawString(font, NiaClickGuiScreen.styled(fit(font, text, maxWidth)), x, y, color, false);
    }

    private String formatHeldTime(long acquiredMillis) {
        if (acquiredMillis <= 0) {
            return "Unknown";
        }
        long seconds = Math.max(0L, (System.currentTimeMillis() - acquiredMillis) / 1000L);
        long days = seconds / 86_400L;
        seconds %= 86_400L;
        long hours = seconds / 3_600L;
        seconds %= 3_600L;
        long minutes = seconds / 60L;
        seconds %= 60L;
        if (days > 0) {
            return days + "d " + hours + "h " + minutes + "m " + seconds + "s";
        }
        if (hours > 0) {
            return hours + "h " + minutes + "m " + seconds + "s";
        }
        return minutes + "m " + seconds + "s";
    }

    private String formatNumber(int value) {
        return String.format(Locale.ROOT, "%,d", value).replace(',', ' ');
    }

    private String formatStat(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001) {
            return formatNumber((int) Math.round(value));
        }
        return String.format(Locale.ROOT, "%.2f", value);
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

    private int panelBorderColor(ClickGuiTheme theme) {
        return Render2D.withAlpha(theme.accentColor(), 86);
    }

    private int headerFillColor(ClickGuiTheme theme) {
        return Render2D.withAlpha(theme.accentColor(), 28);
    }

    private int dividerColor(ClickGuiTheme theme) {
        return Render2D.withAlpha(theme.textColor(), 42);
    }

    private int controlFillColor(ClickGuiTheme theme, boolean hovered) {
        return hovered ? Render2D.lerpColor(theme.secondary(), theme.accentColor(), 0.24f) : theme.secondary();
    }

    private int valueFillColor(ClickGuiTheme theme) {
        return Render2D.lerpColor(theme.background(), theme.secondary(), 0.7f);
    }

    private int clampInt(int value, int min, int max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
