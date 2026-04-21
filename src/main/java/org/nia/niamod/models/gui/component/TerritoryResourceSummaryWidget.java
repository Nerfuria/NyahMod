package org.nia.niamod.models.gui.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.nia.niamod.models.gui.territory.ResourceFlowState;
import org.nia.niamod.models.gui.render.UiRect;
import org.nia.niamod.models.gui.screens.NiaClickGuiScreen;
import org.nia.niamod.models.gui.theme.ClickGuiTheme;
import org.nia.niamod.render.Render2D;

import java.util.Locale;

public class TerritoryResourceSummaryWidget {
    private static final int MARGIN = 8;
    private static final int WIDTH = 244;
    private static final int HEADER_HEIGHT = 18;
    private static final int ROW_HEIGHT = 14;
    private static final int PAD = 8;

    public int height() {
        int rows = 7;
        return PAD + HEADER_HEIGHT + rows * ROW_HEIGHT + PAD - 2;
    }

    public void render(GuiGraphics g, Font font, ClickGuiTheme theme, ResourceFlowState state) {
        int height = height();
        UiRect panel = new UiRect(MARGIN, MARGIN, WIDTH, height);
        Render2D.shaderRoundedSurface(g, panel.x(), panel.y(), panel.width(), panel.height(), 4, theme.background(), Render2D.withAlpha(theme.accentColor(), 86));
        g.fill(panel.x() + 1, panel.y() + 1, panel.right() - 1, panel.y() + HEADER_HEIGHT + 2, Render2D.withAlpha(theme.accentColor(), 28));

        int x = panel.x() + PAD;
        int y = panel.y() + PAD - 1;
        int right = panel.right() - PAD;
        g.drawString(font, NiaClickGuiScreen.styled("Guild Economy"), x, y, theme.textColor(), false);
        y += HEADER_HEIGHT;

        String hqLabel = state == null || !state.hasHeadquarters() ? "HQ: Not set" : "HQ: " + state.headquarterName();
        g.drawString(font, NiaClickGuiScreen.styled(hqLabel), x, y + 1, theme.secondaryText(), false);
        y += ROW_HEIGHT;

        long current = state == null ? 0L : state.headquarterResources();
        long max = state == null ? 0L : state.headquarterCapacity();
        y = drawRow(g, font, x, right, y, "HQ Resources", format(current) + (max > 0 ? " / " + format(max) : ""), theme);
        y = drawRow(g, font, x, right, y, "Resources / hr", format(state == null ? 0.0 : state.resourceGainPerHour()), theme);
        y = drawRow(g, font, x, right, y, "Used / hr", format(state == null ? 0L : state.resourceLossPerHour()), theme);
        y = drawRow(g, font, x, right, y, "Usage", formatPercent(state == null ? 0.0 : state.resourceUsagePercent()), theme);
        y = drawRow(g, font, x, right, y, "Emeralds / hr", format(state == null ? 0.0 : state.emeraldGainPerHour()), theme);
        drawRow(g, font, x, right, y, "Net / hr", format(state == null ? 0.0 : state.netResourcePerHour()), theme);
    }

    private int drawRow(GuiGraphics g, Font font, int x, int right, int y, String label, String value, ClickGuiTheme theme) {
        g.drawString(font, NiaClickGuiScreen.styled(label), x, y + 1, theme.secondaryText(), false);
        int valueWidth = font.width(NiaClickGuiScreen.styled(value));
        g.drawString(font, NiaClickGuiScreen.styled(value), right - valueWidth, y + 1, theme.textColor(), false);
        return y + ROW_HEIGHT;
    }

    private String format(long value) {
        return String.format(Locale.ROOT, "%,d", value).replace(',', ' ');
    }

    private String format(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001) {
            return format(Math.round(value));
        }
        return String.format(Locale.ROOT, "%,.2f", value).replace(',', ' ');
    }

    private String formatPercent(double value) {
        return String.format(Locale.ROOT, "%.1f%%", value);
    }
}
