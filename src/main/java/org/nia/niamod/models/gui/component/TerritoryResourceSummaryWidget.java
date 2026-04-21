package org.nia.niamod.models.gui.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.nia.niamod.models.gui.render.UiRect;
import org.nia.niamod.models.gui.screens.NiaClickGuiScreen;
import org.nia.niamod.models.gui.territory.ResourceKind;
import org.nia.niamod.models.gui.territory.Resources;
import org.nia.niamod.models.gui.territory.TerritoryResourceColors;
import org.nia.niamod.models.gui.theme.ClickGuiTheme;
import org.nia.niamod.render.Render2D;

import java.util.Locale;

public class TerritoryResourceSummaryWidget {
    private static final int MARGIN = 8;
    private static final int WIDTH = 252;
    private static final int HEADER_HEIGHT = 18;
    private static final int ROW_HEIGHT = 13;
    private static final int PAD = 8;

    public int height(boolean hasHq) {
        int rows = hasHq ? 12 : 7;
        return PAD + HEADER_HEIGHT + rows * ROW_HEIGHT + PAD - 2;
    }

    public void render(GuiGraphics g, Font font, int screenWidth, ClickGuiTheme theme, Resources gained, Resources lost, String hqName, Resources hqCurrent) {
        boolean hasHq = hqName != null && !hqName.isBlank();
        int height = height(hasHq);
        UiRect panel = new UiRect(screenWidth - WIDTH - MARGIN, MARGIN, WIDTH, height);
        Render2D.shaderRoundedSurface(g, panel.x(), panel.y(), panel.width(), panel.height(), 4, theme.background(), Render2D.withAlpha(theme.accentColor(), 86));
        g.fill(panel.x() + 1, panel.y() + 1, panel.right() - 1, panel.y() + HEADER_HEIGHT + 2, Render2D.withAlpha(theme.accentColor(), 28));

        int x = panel.x() + PAD;
        int y = panel.y() + PAD - 1;
        int right = panel.right() - PAD;
        g.drawString(font, NiaClickGuiScreen.styled("Guild Resource Flow"), x, y, theme.textColor(), false);
        y += HEADER_HEIGHT;

        Resources safeGained = gained == null ? Resources.EMPTY : gained;
        Resources safeLost = lost == null ? Resources.EMPTY : lost;
        y = drawFlowRow(g, font, x, right, y, "Emeralds", safeGained.emeralds(), safeLost.emeralds(), TerritoryResourceColors.cityColor(), theme);
        y = drawFlowRow(g, font, x, right, y, "Ore", safeGained.ore(), safeLost.ore(), TerritoryResourceColors.configuredColor(ResourceKind.ORE), theme);
        y = drawFlowRow(g, font, x, right, y, "Crops", safeGained.crops(), safeLost.crops(), TerritoryResourceColors.configuredColor(ResourceKind.CROPS), theme);
        y = drawFlowRow(g, font, x, right, y, "Fish", safeGained.fish(), safeLost.fish(), TerritoryResourceColors.configuredColor(ResourceKind.FISH), theme);
        y = drawFlowRow(g, font, x, right, y, "Wood", safeGained.wood(), safeLost.wood(), TerritoryResourceColors.configuredColor(ResourceKind.WOOD), theme);

        g.fill(x, y + 5, right, y + 6, Render2D.withAlpha(theme.textColor(), 42));
        y += ROW_HEIGHT;

        String hqLabel = (hqName == null || hqName.isBlank()) ? "HQ: Not set" : "HQ: " + hqName;
        g.drawString(font, NiaClickGuiScreen.styled(hqLabel), x, y + 1, theme.secondaryText(), false);
        y += ROW_HEIGHT;
        Resources hq = hqCurrent == null ? Resources.EMPTY : hqCurrent;
        y = drawStockRow(g, font, x, right, y, "Emeralds", hq.emeralds(), TerritoryResourceColors.cityColor(), theme);
        y = drawStockRow(g, font, x, right, y, "Ore", hq.ore(), TerritoryResourceColors.configuredColor(ResourceKind.ORE), theme);
        y = drawStockRow(g, font, x, right, y, "Crops", hq.crops(), TerritoryResourceColors.configuredColor(ResourceKind.CROPS), theme);
        y = drawStockRow(g, font, x, right, y, "Fish", hq.fish(), TerritoryResourceColors.configuredColor(ResourceKind.FISH), theme);
        drawStockRow(g, font, x, right, y, "Wood", hq.wood(), TerritoryResourceColors.configuredColor(ResourceKind.WOOD), theme);
    }

    private int drawFlowRow(GuiGraphics g, Font font, int x, int right, int y, String label, int gained, int lost, int color, ClickGuiTheme theme) {
        g.fill(x, y + 4, x + 7, y + 9, color);
        g.drawString(font, NiaClickGuiScreen.styled(label), x + 11, y + 1, theme.secondaryText(), false);

        String value = "+" + format(gained) + "  -" + format(lost);
        int valueWidth = font.width(NiaClickGuiScreen.styled(value));
        g.drawString(font, NiaClickGuiScreen.styled(value), right - valueWidth, y + 1, theme.textColor(), false);
        return y + ROW_HEIGHT;
    }

    private int drawStockRow(GuiGraphics g, Font font, int x, int right, int y, String label, int current, int color, ClickGuiTheme theme) {
        g.fill(x, y + 4, x + 7, y + 9, color);
        g.drawString(font, NiaClickGuiScreen.styled(label), x + 11, y + 1, theme.secondaryText(), false);

        String value = format(current);
        int valueWidth = font.width(NiaClickGuiScreen.styled(value));
        g.drawString(font, NiaClickGuiScreen.styled(value), right - valueWidth, y + 1, theme.textColor(), false);
        return y + ROW_HEIGHT;
    }

    private String format(int value) {
        return String.format(Locale.ROOT, "%,d", value).replace(',', ' ');
    }
}
