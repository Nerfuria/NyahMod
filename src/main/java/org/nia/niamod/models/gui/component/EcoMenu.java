package org.nia.niamod.models.gui.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.nia.niamod.models.gui.render.UiRect;
import org.nia.niamod.models.gui.screens.NiaClickGuiScreen;
import org.nia.niamod.models.territory.ResourceAmounts;
import org.nia.niamod.models.territory.ResourceKind;
import org.nia.niamod.models.territory.Resources;
import org.nia.niamod.models.territory.TerritoryNode;
import org.nia.niamod.models.territory.TerritoryResourceColors;
import org.nia.niamod.models.territory.TerritoryResourceStore;
import org.nia.niamod.models.territory.TerritoryUpgrade;
import org.nia.niamod.models.gui.theme.ClickGuiTheme;
import org.nia.niamod.render.Render2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

public class EcoMenu {
    private static final TerritoryUpgrade[] ECONOMY_UPGRADES = new TerritoryUpgrade[]{
            TerritoryUpgrade.RESOURCE_STORAGE,
            TerritoryUpgrade.EMERALD_STORAGE,
            TerritoryUpgrade.RESOURCE_RATE,
            TerritoryUpgrade.EFFICIENT_RESOURCES,
            TerritoryUpgrade.EMERALD_RATE,
            TerritoryUpgrade.EFFICIENT_EMERALDS
    };
    private static final TerritoryUpgrade[] OTHER_UPGRADES = new TerritoryUpgrade[]{
            TerritoryUpgrade.GATHERING_EXPERIENCE,
            TerritoryUpgrade.MOB_EXPERIENCE,
            TerritoryUpgrade.MOB_DAMAGE,
            TerritoryUpgrade.PVP_DAMAGE,
            TerritoryUpgrade.XP_SEEKING,
            TerritoryUpgrade.TOME_SEEKING,
            TerritoryUpgrade.EMERALD_SEEKING
    };

    private static final int PANEL_MARGIN = 8;
    private static final int PANEL_WIDTH = 286;
    private static final int PANEL_MAX_HEIGHT = 560;
    private static final int HEADER_HEIGHT = 24;
    private static final int ROW_HEIGHT = 18;
    private static final int BUTTON_SIZE = 16;
    private static final int SCROLL_STEP = 26;

    private final List<DetailButton> buttons = new ArrayList<>();
    private boolean dragging;
    private boolean positioned;
    private int x;
    private int y;
    private int scrollOffset;
    private double dragOffsetX;
    private double dragOffsetY;

    public void render(
            GuiGraphics g,
            Font font,
            int mouseX,
            int mouseY,
            ClickGuiTheme theme,
            DetailData data,
            int screenWidth,
            int screenHeight,
            BiConsumer<TerritoryUpgrade, Integer> upgradeAdjusted,
            IntConsumer taxAdjusted,
            Runnable headquartersSet,
            Runnable bordersToggled,
            Runnable routeToggled,
            Runnable loadoutsOpened,
            Runnable close
    ) {
        buttons.clear();
        if (data == null || data.selectedTerritory() == null) {
            return;
        }

        TerritoryWidget selectedTerritory = data.selectedTerritory();
        TerritoryNode territory = selectedTerritory.territory();
        Map<TerritoryUpgrade, Integer> upgradeLevels = data.upgradeLevels();
        TerritoryResourceStore store = data.resourceStore();
        Resources perHourResources = data.producedResources();
        UiRect panel = panelBounds(screenWidth, screenHeight, perHourResources);
        if (panel.width() <= 0 || panel.height() <= 0) {
            return;
        }
        UiRect body = bodyBounds(panel);
        int bodyContentHeight = bodyContentHeight(perHourResources);
        int maxScroll = maxScroll(perHourResources, body.height());
        clampScroll(maxScroll);

        Render2D.shaderRoundedSurface(g, panel.x(), panel.y(), panel.width(), panel.height(), 4, theme.background(), panelBorderColor(theme));
        g.fill(panel.x() + 1, panel.y() + 1, panel.right() - 1, panel.y() + HEADER_HEIGHT, headerFillColor(theme));

        int contentX = panel.x() + 10;
        int contentW = panel.width() - 20;
        int right = contentX + contentW;
        int rowY = panel.y() + 7;

        UiRect closeButton = new UiRect(panel.right() - 27, panel.y() + 5, 18, 18);
        drawPlainButton(g, font, closeButton, "X", closeButton.contains(mouseX, mouseY), theme);
        buttons.add(new DetailButton(closeButton, close, false));

        drawFittedString(g, font, territory.name(), contentX, rowY + 2, Math.max(1, closeButton.x() - contentX - 8), theme.textColor());
        rowY = body.y() + 7 - scrollOffset;

        g.enableScissor(body.x(), body.y(), body.right(), body.bottom());
        rowY = drawResourceSummary(g, font, perHourResources, contentX, rowY, contentW, theme);
        rowY += 4;
        drawDivider(g, panel, rowY, theme);
        rowY += 8;

        int guildTagW = Math.min(contentW, Math.max(36, font.width(data.guildName()) + 8));
        g.fill(contentX, rowY - 2, contentX + guildTagW, rowY + font.lineHeight + 2, Render2D.withAlpha(theme.accentColor(), 230));
        drawFittedString(g, font, data.guildName(), contentX + 4, rowY, guildTagW - 8, theme.textColor());
        rowY += 18;

        rowY = drawInfoLine(g, font, contentX, rowY, contentW, "Time held: " + formatHeldTime(territory.acquiredMillis()), theme.secondaryText());
        rowY = drawStoredResources(g, font, contentX, rowY, contentW, store, theme);
        rowY = drawInfoLine(g, font, contentX, rowY, contentW, "Connections: " + data.ownedConnections() + "/" + data.totalConnections(), theme.secondaryText());
        rowY = drawInfoLine(g, font, contentX, rowY, contentW, "External: " + data.externalConnections(), theme.secondaryText());
        if (!data.loadoutName().isBlank()) {
            rowY = drawInfoLine(g, font, contentX, rowY, contentW, "Loadout: " + data.loadoutName(), theme.secondaryText());
        }
        rowY += 3;
        rowY = drawTaxRow(g, font, contentX, right, rowY, mouseX, mouseY, theme, data.taxPercent(), taxAdjusted);
        rowY = drawSplitButtons(
                g,
                font,
                contentX,
                right,
                rowY,
                mouseX,
                mouseY,
                theme,
                data.headquarters() ? "HQ Set" : "Set HQ",
                headquartersSet,
                data.bordersOpen() ? "Open" : "Closed",
                bordersToggled
        );
        rowY = drawFullButton(g, font, contentX, right, rowY, mouseX, mouseY, theme, "Route: " + data.routeLabel(), routeToggled);
        rowY = drawFullButton(g, font, contentX, right, rowY, mouseX, mouseY, theme, "Loadouts", loadoutsOpened);
        rowY += 2;
        drawDivider(g, panel, rowY, theme);
        rowY += 9;

        drawFittedString(g, font, "Combat", contentX, rowY, contentW, theme.textColor());
        rowY += 18;
        rowY = drawUpgradeRow(g, font, contentX, right, rowY, mouseX, mouseY, theme, TerritoryUpgrade.DAMAGE, "Damage: " + formatDamage(data.damage()), upgradeLevels, upgradeAdjusted);
        rowY = drawUpgradeRow(g, font, contentX, right, rowY, mouseX, mouseY, theme, TerritoryUpgrade.ATTACK, "Attack Speed: " + formatStat(data.attackSpeed()), upgradeLevels, upgradeAdjusted);
        rowY = drawUpgradeRow(g, font, contentX, right, rowY, mouseX, mouseY, theme, TerritoryUpgrade.HEALTH, "Health: " + formatStat(data.health()), upgradeLevels, upgradeAdjusted);
        rowY = drawUpgradeRow(g, font, contentX, right, rowY, mouseX, mouseY, theme, TerritoryUpgrade.DEFENSE, "Defense: " + formatPercent(data.defense()), upgradeLevels, upgradeAdjusted);
        rowY = drawUpgradeRow(g, font, contentX, right, rowY, mouseX, mouseY, theme, TerritoryUpgrade.TOWER_AURA, TerritoryUpgrade.TOWER_AURA.label(), upgradeLevels, upgradeAdjusted);
        rowY = drawUpgradeRow(g, font, contentX, right, rowY, mouseX, mouseY, theme, TerritoryUpgrade.STRONGER_MINIONS, TerritoryUpgrade.STRONGER_MINIONS.label(), upgradeLevels, upgradeAdjusted);
        rowY = drawUpgradeRow(g, font, contentX, right, rowY, mouseX, mouseY, theme, TerritoryUpgrade.TOWER_VOLLEY, TerritoryUpgrade.TOWER_VOLLEY.label(), upgradeLevels, upgradeAdjusted);
        rowY = drawUpgradeRow(g, font, contentX, right, rowY, mouseX, mouseY, theme, TerritoryUpgrade.TOWER_MULTI_ATTACKS, TerritoryUpgrade.TOWER_MULTI_ATTACKS.label(), upgradeLevels, upgradeAdjusted);
        rowY += 5;
        drawDivider(g, panel, rowY, theme);
        rowY += 9;

        drawFittedString(g, font, "Economy", contentX, rowY, contentW, theme.textColor());
        rowY += 18;
        for (TerritoryUpgrade upgrade : ECONOMY_UPGRADES) {
            rowY = drawUpgradeRow(
                    g,
                    font,
                    contentX,
                    right,
                    rowY,
                    mouseX,
                    mouseY,
                    theme,
                    upgrade,
                    upgrade.label(),
                    upgradeLevels,
                    upgradeAdjusted
            );
        }

        rowY += 5;
        drawDivider(g, panel, rowY, theme);
        rowY += 9;
        drawFittedString(g, font, "Other Buffs", contentX, rowY, contentW, theme.textColor());
        rowY += 18;
        for (TerritoryUpgrade upgrade : OTHER_UPGRADES) {
            rowY = drawUpgradeRow(
                    g,
                    font,
                    contentX,
                    right,
                    rowY,
                    mouseX,
                    mouseY,
                    theme,
                    upgrade,
                    upgrade.label(),
                    upgradeLevels,
                    upgradeAdjusted
            );
        }
        g.disableScissor();

        renderScrollbar(g, body, theme, bodyContentHeight, maxScroll);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, int screenWidth, int screenHeight, Resources producedResources) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }

        Resources resources = producedResources == null ? Resources.EMPTY : producedResources;
        UiRect panel = panelBounds(screenWidth, screenHeight, resources);
        if (!panel.contains(mouseX, mouseY)) {
            return false;
        }
        UiRect body = bodyBounds(panel);

        for (DetailButton detailButton : buttons) {
            if (detailButton.clipToBody() && !body.contains(mouseX, mouseY)) {
                continue;
            }
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

    public boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount, int screenWidth, int screenHeight, Resources producedResources) {
        if (verticalAmount == 0.0) {
            return false;
        }

        Resources resources = producedResources == null ? Resources.EMPTY : producedResources;
        UiRect panel = panelBounds(screenWidth, screenHeight, resources);
        if (!panel.contains(mouseX, mouseY)) {
            return false;
        }

        int maxScroll = maxScroll(resources, bodyBounds(panel).height());
        if (maxScroll <= 0) {
            return false;
        }

        int delta = (int) Math.round(-verticalAmount * SCROLL_STEP);
        if (delta == 0) {
            delta = verticalAmount < 0.0 ? SCROLL_STEP : -SCROLL_STEP;
        }

        int previous = scrollOffset;
        scrollOffset = clampInt(scrollOffset + delta, 0, maxScroll);
        return scrollOffset != previous;
    }

    public boolean contains(double mouseX, double mouseY, int screenWidth, int screenHeight, Resources producedResources) {
        Resources resources = producedResources == null ? Resources.EMPTY : producedResources;
        return panelBounds(screenWidth, screenHeight, resources).contains(mouseX, mouseY);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int screenWidth, int screenHeight, Resources producedResources) {
        if (!dragging) {
            return false;
        }

        Resources resources = producedResources == null ? Resources.EMPTY : producedResources;
        UiRect panel = panelBounds(screenWidth, screenHeight, resources);
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

    public void resetScroll() {
        scrollOffset = 0;
    }

    public void ensurePosition(int screenWidth, int screenHeight, Resources resources) {
        int panelW = Math.min(PANEL_WIDTH, Math.max(1, screenWidth - PANEL_MARGIN * 2));
        int panelH = panelHeight(screenHeight, resources);
        ensurePosition(screenWidth, screenHeight, panelW, panelH);
    }

    private UiRect panelBounds(int screenWidth, int screenHeight, Resources resources) {
        int panelW = Math.min(PANEL_WIDTH, Math.max(1, screenWidth - PANEL_MARGIN * 2));
        int panelH = panelHeight(screenHeight, resources);
        ensurePosition(screenWidth, screenHeight, panelW, panelH);
        return new UiRect(x, y, panelW, panelH);
    }

    private int panelHeight(int screenHeight, Resources resources) {
        int available = Math.max(HEADER_HEIGHT + 1, screenHeight - PANEL_MARGIN * 2);
        int maxBodyHeight = Math.max(1, Math.min(PANEL_MAX_HEIGHT, available) - HEADER_HEIGHT);
        return HEADER_HEIGHT + Math.min(bodyContentHeight(resources), maxBodyHeight);
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

    private UiRect bodyBounds(UiRect panel) {
        return new UiRect(panel.x() + 1, panel.y() + HEADER_HEIGHT, Math.max(1, panel.width() - 2), Math.max(1, panel.height() - HEADER_HEIGHT));
    }

    private int bodyContentHeight(Resources resources) {
        return 7
                + resourceSummaryHeight(resources)
                + 4
                + 8
                + 18
                + 12
                + 60
                + 12
                + 12
                + 12
                + 3
                + ROW_HEIGHT * 4
                + 2
                + 9
                + 18
                + ROW_HEIGHT * 8
                + 5
                + 9
                + 18
                + ROW_HEIGHT * ECONOMY_UPGRADES.length
                + 5
                + 9
                + 18
                + ROW_HEIGHT * OTHER_UPGRADES.length
                + 10;
    }

    private int maxScroll(Resources resources, int bodyHeight) {
        return Math.max(0, bodyContentHeight(resources) - Math.max(1, bodyHeight));
    }

    private void clampScroll(int maxScroll) {
        scrollOffset = clampInt(scrollOffset, 0, maxScroll);
    }

    private int resourceSummaryHeight(Resources resources) {
        int rows = 0;
        if (resources != null) {
            if (resources.emeralds() > 0) {
                rows++;
            }
            if (resources.crops() > 0) {
                rows++;
            }
            if (resources.wood() > 0) {
                rows++;
            }
            if (resources.ore() > 0) {
                rows++;
            }
            if (resources.fish() > 0) {
                rows++;
            }
        }
        return Math.max(1, rows) * 12;
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

    private int drawInfoLine(GuiGraphics g, Font font, int x, int y, int maxWidth, String text, int color) {
        drawFittedString(g, font, text, x, y, maxWidth, color);
        return y + 12;
    }

    private int drawTaxRow(GuiGraphics g, Font font, int x, int right, int y, int mouseX, int mouseY, ClickGuiTheme theme, int taxPercent, IntConsumer taxAdjusted) {
        drawFittedString(g, font, "Tax", x, y + 4, Math.max(1, right - x - 78), theme.textColor());

        int plusX = right - BUTTON_SIZE;
        int valueX = plusX - 31;
        int minusX = valueX - BUTTON_SIZE - 6;
        UiRect minus = new UiRect(minusX, y + 2, BUTTON_SIZE, BUTTON_SIZE);
        UiRect value = new UiRect(valueX, y + 2, 28, BUTTON_SIZE);
        UiRect plus = new UiRect(plusX, y + 2, BUTTON_SIZE, BUTTON_SIZE);

        drawPlainButton(g, font, minus, "-", minus.contains(mouseX, mouseY), theme);
        drawValuePill(g, font, value, Math.max(0, Math.min(70, taxPercent)) + "%", theme);
        drawPlainButton(g, font, plus, "+", plus.contains(mouseX, mouseY), theme);
        buttons.add(new DetailButton(minus, () -> taxAdjusted.accept(-5), true));
        buttons.add(new DetailButton(plus, () -> taxAdjusted.accept(5), true));
        return y + ROW_HEIGHT;
    }

    private int drawSplitButtons(
            GuiGraphics g,
            Font font,
            int x,
            int right,
            int y,
            int mouseX,
            int mouseY,
            ClickGuiTheme theme,
            String leftLabel,
            Runnable leftClick,
            String rightLabel,
            Runnable rightClick
    ) {
        int gap = 6;
        int width = Math.max(1, right - x);
        int buttonW = (width - gap) / 2;
        UiRect left = new UiRect(x, y + 2, buttonW, BUTTON_SIZE);
        UiRect rightButton = new UiRect(x + buttonW + gap, y + 2, width - buttonW - gap, BUTTON_SIZE);

        drawPlainButton(g, font, left, leftLabel, left.contains(mouseX, mouseY), theme);
        drawPlainButton(g, font, rightButton, rightLabel, rightButton.contains(mouseX, mouseY), theme);
        buttons.add(new DetailButton(left, leftClick, true));
        buttons.add(new DetailButton(rightButton, rightClick, true));
        return y + ROW_HEIGHT;
    }

    private int drawFullButton(GuiGraphics g, Font font, int x, int right, int y, int mouseX, int mouseY, ClickGuiTheme theme, String label, Runnable onClick) {
        UiRect button = new UiRect(x, y + 2, Math.max(1, right - x), BUTTON_SIZE);
        drawPlainButton(g, font, button, label, button.contains(mouseX, mouseY), theme);
        buttons.add(new DetailButton(button, onClick, true));
        return y + ROW_HEIGHT;
    }

    private int drawUpgradeRow(
            GuiGraphics g,
            Font font,
            int x,
            int right,
            int y,
            int mouseX,
            int mouseY,
            ClickGuiTheme theme,
            TerritoryUpgrade upgrade,
            String label,
            Map<TerritoryUpgrade, Integer> upgradeLevels,
            BiConsumer<TerritoryUpgrade, Integer> upgradeAdjusted
    ) {
        int controlsW = 70;
        int costW = 48;
        boolean hovered = new UiRect(x, y, Math.max(1, right - x), ROW_HEIGHT).contains(mouseX, mouseY);
        long upgradeCost = upgrade.cost(upgradeLevel(upgradeLevels, upgrade));
        boolean showCost = upgradeCost > 0L;
        int labelW = showCost ? right - x - controlsW - costW - 12 : right - x - controlsW - 8;
        drawFittedString(g, font, label, x, y + 4, Math.max(1, labelW), theme.textColor());

        int plusX = right - BUTTON_SIZE;
        int valueX = plusX - 25;
        int minusX = valueX - BUTTON_SIZE - 6;
        UiRect cost = new UiRect(minusX - costW - 6, y + 2, costW, BUTTON_SIZE);
        UiRect minus = new UiRect(minusX, y + 2, BUTTON_SIZE, BUTTON_SIZE);
        UiRect plus = new UiRect(plusX, y + 2, BUTTON_SIZE, BUTTON_SIZE);
        UiRect value = new UiRect(valueX, y + 2, 22, BUTTON_SIZE);
        if (showCost) {
            drawCostPill(g, font, cost, upgradeCost, upgrade.costResource(), theme);
        }
        drawPlainButton(g, font, minus, "-", minus.contains(mouseX, mouseY), theme);
        drawValuePill(g, font, value, Integer.toString(upgradeLevel(upgradeLevels, upgrade)), theme);
        drawPlainButton(g, font, plus, "+", plus.contains(mouseX, mouseY), theme);
        buttons.add(new DetailButton(minus, () -> upgradeAdjusted.accept(upgrade, -1), true));
        buttons.add(new DetailButton(plus, () -> upgradeAdjusted.accept(upgrade, 1), true));
        return y + ROW_HEIGHT;
    }

    private void drawCostPill(GuiGraphics g, Font font, UiRect rect, long cost, ResourceKind resource, ClickGuiTheme theme) {
        int color = TerritoryResourceColors.configuredColor(resource);
        g.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), Render2D.withAlpha(color, 52));
        Render2D.outline(g, rect, Render2D.withAlpha(color, 130));
        Component text = NiaClickGuiScreen.styled(fit(font, formatShortNumber(cost) + shortResourceName(resource), Math.max(1, rect.width() - 4)));
        g.drawString(font, text, rect.x() + (rect.width() - font.width(text)) / 2, rect.y() + (rect.height() - font.lineHeight) / 2 + 1, theme.textColor(), false);
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

    private int upgradeLevel(Map<TerritoryUpgrade, Integer> upgradeLevels, TerritoryUpgrade upgrade) {
        return upgradeLevels == null ? 0 : upgradeLevels.getOrDefault(upgrade, 0);
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

    private int drawStoredResources(GuiGraphics g, Font font, int x, int y, int maxWidth, TerritoryResourceStore store, ClickGuiTheme theme) {
        if (store == null || store.max().empty()) {
            return drawInfoLine(g, font, x, y, maxWidth, "Stored Resources: Unknown", theme.secondaryText());
        }

        ResourceAmounts current = store.current();
        ResourceAmounts max = store.max();
        y = drawInfoLine(g, font, x, y, maxWidth, storedResourceLine("Emeralds", current.emeralds(), max.emeralds()), theme.secondaryText());
        y = drawInfoLine(g, font, x, y, maxWidth, storedResourceLine("Ore", current.ore(), max.ore()), theme.secondaryText());
        y = drawInfoLine(g, font, x, y, maxWidth, storedResourceLine("Crops", current.crops(), max.crops()), theme.secondaryText());
        y = drawInfoLine(g, font, x, y, maxWidth, storedResourceLine("Fish", current.fish(), max.fish()), theme.secondaryText());
        return drawInfoLine(g, font, x, y, maxWidth, storedResourceLine("Wood", current.wood(), max.wood()), theme.secondaryText());
    }

    private String storedResourceLine(String label, long current, long max) {
        return label + ": " + formatNumber(current) + " / " + formatNumber(max);
    }

    private String formatDamage(DamageRange damage) {
        if (damage == null) {
            return "0 - 0";
        }
        return formatStat(damage.min()) + "-" + formatStat(damage.max());
    }

    private String formatNumber(long value) {
        return String.format(Locale.ROOT, "%,d", value).replace(',', ' ');
    }

    private String formatShortNumber(long value) {
        long abs = Math.abs(value);
        if (abs >= 1_000_000L) {
            return String.format(Locale.ROOT, "%.1fm", value / 1_000_000.0);
        }
        if (abs >= 1_000L) {
            return String.format(Locale.ROOT, "%.1fk", value / 1_000.0);
        }
        return Long.toString(value);
    }

    private String shortResourceName(ResourceKind resource) {
        return switch (resource) {
            case EMERALDS -> "e";
            case ORE -> "o";
            case CROPS -> "c";
            case FISH -> "f";
            case WOOD -> "w";
            case ALL -> "r";
            case NONE -> "-";
        };
    }

    private String formatStat(double value) {
        if (Double.isInfinite(value)) {
            return "Infinity";
        }
        if (Math.abs(value - Math.rint(value)) < 0.0001) {
            return formatNumber(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private String formatPercent(double value) {
        if (Double.isInfinite(value)) {
            return "Infinity";
        }
        return String.format(Locale.ROOT, "%.1f%%", value * 100.0);
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

    private void renderScrollbar(GuiGraphics g, UiRect body, ClickGuiTheme theme, int bodyContentHeight, int maxScroll) {
        if (maxScroll <= 0 || body.height() <= 0) {
            return;
        }

        int trackX = body.right() - 4;
        int trackTop = body.y() + 2;
        int trackBottom = body.bottom() - 2;
        int trackHeight = Math.max(1, trackBottom - trackTop);
        int thumbHeight = Math.max(18, Math.round((body.height() / (float) Math.max(1, bodyContentHeight)) * body.height()));
        thumbHeight = Math.min(trackHeight, thumbHeight);
        int thumbTravel = Math.max(0, trackHeight - thumbHeight);
        int thumbY = trackTop + Math.round((scrollOffset / (float) maxScroll) * thumbTravel);

        g.fill(trackX, trackTop, trackX + 2, trackBottom, Render2D.withAlpha(theme.secondaryText(), 55));
        g.fill(trackX - 1, thumbY, trackX + 3, thumbY + thumbHeight, Render2D.withAlpha(theme.accentColor(), 170));
    }

    private int clampInt(int value, int min, int max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    public record DetailData(
            TerritoryWidget selectedTerritory,
            String guildName,
            Map<TerritoryUpgrade, Integer> upgradeLevels,
            TerritoryResourceStore resourceStore,
            Resources producedResources,
            DamageRange damage,
            double attackSpeed,
            double health,
            double defense,
            int ownedConnections,
            int totalConnections,
            int externalConnections,
            int taxPercent,
            boolean headquarters,
            boolean bordersOpen,
            String routeLabel,
            String loadoutName
    ) {
        public DetailData {
            guildName = guildName == null || guildName.isBlank() ? "Guild" : guildName;
            upgradeLevels = upgradeLevels == null ? Map.of() : upgradeLevels;
            resourceStore = resourceStore == null ? TerritoryResourceStore.EMPTY : resourceStore;
            producedResources = producedResources == null ? Resources.EMPTY : producedResources;
            damage = damage == null ? DamageRange.EMPTY : damage;
            routeLabel = routeLabel == null || routeLabel.isBlank() ? "Fastest" : routeLabel;
            loadoutName = loadoutName == null ? "" : loadoutName.trim();
        }
    }

    public record DamageRange(double min, double max) {
        public static final DamageRange EMPTY = new DamageRange(0.0, 0.0);
    }

    private record DetailButton(UiRect bounds, Runnable onClick, boolean clipToBody) {
    }
}
