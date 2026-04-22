package org.nia.niamod.models.gui.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;
import org.nia.niamod.models.gui.render.UiRect;
import org.nia.niamod.models.gui.screens.NiaClickGuiScreen;
import org.nia.niamod.models.territory.TerritoryResourceColors;
import org.nia.niamod.models.territory.TerritoryUpgrade;
import org.nia.niamod.models.gui.theme.ClickGuiTheme;
import org.nia.niamod.render.Render2D;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

public class TerritoryQuickMenu {
    private static final int TILE_W = 26;
    private static final int TILE_H = 26;
    private static final int SIDE_W = 58;
    private static final int SIDE_H = 18;
    private static final int TAX_H = 40;
    private static final int GAP = 2;
    private static final int WIDTH = TILE_W * 4 + GAP * 3 + SIDE_W + GAP;
    private static final int HEIGHT = Math.max(TILE_H * 4 + GAP * 3, TAX_H + GAP + SIDE_H + GAP + SIDE_H);
    private static final TerritoryUpgrade[] UPGRADES = new TerritoryUpgrade[]{
            TerritoryUpgrade.DAMAGE,
            TerritoryUpgrade.ATTACK,
            TerritoryUpgrade.HEALTH,
            TerritoryUpgrade.DEFENSE,
            TerritoryUpgrade.TOWER_AURA,
            TerritoryUpgrade.TOWER_VOLLEY,
            TerritoryUpgrade.STRONGER_MINIONS,
            TerritoryUpgrade.TOWER_MULTI_ATTACKS,
            TerritoryUpgrade.RESOURCE_STORAGE,
            TerritoryUpgrade.EMERALD_STORAGE,
            TerritoryUpgrade.EFFICIENT_RESOURCES,
            TerritoryUpgrade.RESOURCE_RATE,
            TerritoryUpgrade.EFFICIENT_EMERALDS,
            TerritoryUpgrade.EMERALD_RATE
    };
    private static final Map<TerritoryUpgrade, ItemStack> ICONS = createIcons();
    private static final ItemStack TAX_ICON = new ItemStack(Items.EMERALD);
    private static final String CROWN = "\uD83D\uDC51";

    private final List<Button> buttons = new ArrayList<>();
    private int x;
    private int y;
    private boolean visible;
    private boolean editingTax;
    private String taxInput = "";
    private int originalTax;

    private static Map<TerritoryUpgrade, ItemStack> createIcons() {
        EnumMap<TerritoryUpgrade, ItemStack> icons = new EnumMap<>(TerritoryUpgrade.class);
        for (TerritoryUpgrade upgrade : TerritoryUpgrade.values()) {
            icons.put(upgrade, new ItemStack(upgrade.iconItem()));
        }
        return icons;
    }

    public void showAt(int mouseX, int mouseY, int screenWidth, int screenHeight) {
        x = clamp(mouseX, 4, Math.max(4, screenWidth - WIDTH - 4));
        y = clamp(mouseY, 4, Math.max(4, screenHeight - HEIGHT - 4));
        visible = true;
    }

    public void hide() {
        visible = false;
        editingTax = false;
        taxInput = "";
        buttons.clear();
    }

    public boolean visible() {
        return visible;
    }

    public boolean contains(double mouseX, double mouseY) {
        return visible && bounds().contains(mouseX, mouseY);
    }

    public void render(
            GuiGraphics g,
            Font font,
            int mouseX,
            int mouseY,
            ClickGuiTheme theme,
            Map<TerritoryUpgrade, Integer> stats,
            int taxPercent,
            boolean headquarters,
            boolean bordersOpen,
            BiConsumer<TerritoryUpgrade, Integer> statAdjusted,
            IntConsumer taxSet,
            Runnable headquartersSet,
            Runnable bordersToggled
    ) {
        buttons.clear();
        if (!visible) {
            return;
        }

        UiRect panel = bounds();
        Render2D.shaderRoundedSurface(g, panel.x(), panel.y(), panel.width(), panel.height(), 4, theme.background(), Render2D.withAlpha(theme.accentColor(), 92));

        for (int i = 0; i < UPGRADES.length; i++) {
            TerritoryUpgrade upgrade = UPGRADES[i];
            int col = i % 4;
            int row = i / 4;
            UiRect tile = new UiRect(x + col * (TILE_W + GAP), y + row * (TILE_H + GAP), TILE_W, TILE_H);
            drawUpgradeTile(g, font, tile, mouseX, mouseY, theme, upgrade, stats);
            buttons.add(new Button(tile, button -> statAdjusted.accept(upgrade, button == GLFW.GLFW_MOUSE_BUTTON_RIGHT ? -1 : 1)));
        }

        int sideX = x + TILE_W * 4 + GAP * 4;
        UiRect tax = new UiRect(sideX, y, SIDE_W, TAX_H);
        UiRect hq = new UiRect(sideX, y + TAX_H + GAP, SIDE_W, SIDE_H);
        UiRect borders = new UiRect(sideX, y + TAX_H + GAP + SIDE_H + GAP, SIDE_W, SIDE_H);
        drawTaxControl(g, font, tax, mouseX, mouseY, theme, taxPercent);
        drawCrownTile(g, font, hq, mouseX, mouseY, theme, headquarters);
        drawBordersTile(g, font, borders, mouseX, mouseY, theme, bordersOpen);
        buttons.add(new Button(tax, ignored -> startTaxEdit(taxPercent)));
        buttons.add(new Button(hq, ignored -> headquartersSet.run()));
        buttons.add(new Button(borders, ignored -> bordersToggled.run()));
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !bounds().contains(mouseX, mouseY)) {
            return false;
        }
        for (Button quickButton : buttons) {
            if (quickButton.bounds().contains(mouseX, mouseY)) {
                quickButton.onClick().accept(button);
                return true;
            }
        }
        return true;
    }

    public boolean keyPressed(KeyEvent event, IntConsumer taxSet) {
        if (!visible || !editingTax) {
            return false;
        }

        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            commitTax(taxSet);
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            cancelTaxEdit();
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_BACKSPACE) {
            if (!taxInput.isEmpty()) {
                taxInput = taxInput.substring(0, taxInput.length() - 1);
            }
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_DELETE) {
            taxInput = "";
            return true;
        }
        return false;
    }

    public boolean charTyped(CharacterEvent event) {
        if (!visible || !editingTax || !event.isAllowedChatCharacter()) {
            return false;
        }

        String typed = event.codepointAsString();
        if (typed.length() == 1 && Character.isDigit(typed.charAt(0)) && taxInput.length() < 2) {
            taxInput += typed;
            return true;
        }
        return false;
    }

    private void drawUpgradeTile(GuiGraphics g, Font font, UiRect tile, int mouseX, int mouseY, ClickGuiTheme theme, TerritoryUpgrade upgrade, Map<TerritoryUpgrade, Integer> stats) {
        boolean hovered = tile.contains(mouseX, mouseY);
        int color = TerritoryResourceColors.configuredColor(upgrade.costResource());
        g.fill(tile.x(), tile.y(), tile.right(), tile.bottom(), hovered ? Render2D.withAlpha(theme.accentColor(), 34) : Render2D.withAlpha(theme.secondary(), 72));
        Render2D.outline(g, tile, Render2D.withAlpha(color, hovered ? 180 : 72));

        int itemX = tile.x() + (tile.width() - 16) / 2;
        int itemY = tile.y() + (tile.height() - 16) / 2;
        ItemStack stack = ICONS.getOrDefault(upgrade, ItemStack.EMPTY);
        g.renderItem(stack, itemX, itemY);
        g.renderItemDecorations(font, stack, itemX, itemY, Integer.toString(level(stats, upgrade)));
    }

    private void drawTaxControl(GuiGraphics g, Font font, UiRect tile, int mouseX, int mouseY, ClickGuiTheme theme, int taxPercent) {
        boolean hovered = tile.contains(mouseX, mouseY);
        int border = editingTax
                ? Render2D.withAlpha(theme.accentColor(), 210)
                : Render2D.withAlpha(theme.accentColor(), hovered ? 180 : 90);
        g.fill(tile.x(), tile.y(), tile.right(), tile.bottom(), hovered || editingTax ? Render2D.withAlpha(theme.accentColor(), 34) : Render2D.withAlpha(theme.secondary(), 72));
        Render2D.outline(g, tile, border);

        int iconX = tile.x() + (tile.width() - 16) / 2;
        int iconY = tile.y() + 4;
        g.renderItem(TAX_ICON, iconX, iconY);

        String value = editingTax ? taxInput + cursorText() : taxPercent + "%";
        drawCentered(g, font, value, tile.x() + 1, tile.y() + 25, tile.width() - 2, theme.textColor());
    }

    private void drawCrownTile(GuiGraphics g, Font font, UiRect tile, int mouseX, int mouseY, ClickGuiTheme theme, boolean headquarters) {
        boolean hovered = tile.contains(mouseX, mouseY);
        g.fill(tile.x(), tile.y(), tile.right(), tile.bottom(), hovered ? Render2D.withAlpha(theme.accentColor(), 34) : Render2D.withAlpha(theme.secondary(), 72));
        Render2D.outline(g, tile, Render2D.withAlpha(theme.accentColor(), hovered ? 180 : 90));
        drawCentered(g, font, CROWN, tile.x(), tile.y() + 5, tile.width(), headquarters ? 0xFFFFD75A : Render2D.withAlpha(0xFFFFFF, 92));
    }

    private void drawBordersTile(GuiGraphics g, Font font, UiRect tile, int mouseX, int mouseY, ClickGuiTheme theme, boolean bordersOpen) {
        boolean hovered = tile.contains(mouseX, mouseY);
        g.fill(tile.x(), tile.y(), tile.right(), tile.bottom(), hovered ? Render2D.withAlpha(theme.accentColor(), 34) : Render2D.withAlpha(theme.secondary(), 72));
        Render2D.outline(g, tile, Render2D.withAlpha(theme.accentColor(), hovered ? 180 : 90));
        drawCentered(g, font, bordersOpen ? "Open" : "Closed", tile.x() + 1, tile.y() + 5, tile.width() - 2, bordersOpen ? theme.textColor() : theme.secondaryText());
    }

    private void drawCentered(GuiGraphics g, Font font, String value, int x, int y, int width, int color) {
        String fitted = fit(font, value, width);
        Component text = NiaClickGuiScreen.styled(fitted);
        g.drawString(font, text, x + (width - font.width(text)) / 2, y, color, false);
    }

    private int level(Map<TerritoryUpgrade, Integer> stats, TerritoryUpgrade upgrade) {
        return stats == null ? 0 : stats.getOrDefault(upgrade, 0);
    }

    private void startTaxEdit(int taxPercent) {
        originalTax = taxPercent;
        taxInput = "";
        editingTax = true;
    }

    private void commitTax(IntConsumer taxSet) {
        int value = originalTax;
        if (!taxInput.isBlank()) {
            try {
                value = Integer.parseInt(taxInput);
            } catch (NumberFormatException ignored) {
                value = originalTax;
            }
        }
        taxSet.accept(Math.max(0, Math.min(70, value)));
        editingTax = false;
    }

    private void cancelTaxEdit() {
        editingTax = false;
        taxInput = "";
    }

    private String cursorText() {
        return (System.currentTimeMillis() / 450L) % 2L == 0L ? "|" : "";
    }

    private String fit(Font font, String value, int width) {
        if (value == null) {
            return "";
        }
        if (font.width(NiaClickGuiScreen.styled(value)) <= width) {
            return value;
        }
        String trimmed = value;
        while (!trimmed.isEmpty() && font.width(NiaClickGuiScreen.styled(trimmed + "...")) > width) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed.isEmpty() ? "" : trimmed + "...";
    }

    private UiRect bounds() {
        return new UiRect(x, y, WIDTH, HEIGHT);
    }

    private int clamp(int value, int min, int max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private record Button(UiRect bounds, java.util.function.IntConsumer onClick) {
    }
}
