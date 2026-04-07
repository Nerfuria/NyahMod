package org.nia.niamod.models.gui.component;

import lombok.RequiredArgsConstructor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.nia.niamod.config.setting.BooleanSetting;
import org.nia.niamod.models.gui.NiaClickGuiScreen;
import org.nia.niamod.models.gui.render.UiRect;
import org.nia.niamod.models.gui.theme.ClickGuiTheme;
import org.nia.niamod.render.Render2D;

@RequiredArgsConstructor
public class BooleanComponent {
    public static final int HEIGHT = 18;
    private static final int SWITCH_WIDTH = 24;
    private static final int SWITCH_HEIGHT = 12;

    private final BooleanSetting setting;
    private int x, y, width;
    private float animAmount = -1;

    public void setPosition(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    public int getHeight() {
        return HEIGHT;
    }

    public void render(GuiGraphics g, Font font, int mouseX, int mouseY, ClickGuiTheme theme, int opacity) {
        int textAlpha = Math.min(220, opacity);
        int textColor = (textAlpha << 24) | 0xFFFFFF;
        int centerY = y + HEIGHT / 2;

        g.drawString(font, NiaClickGuiScreen.styled(setting.getTitle()), x, centerY - font.lineHeight / 2 + 1, textColor, false);

        int switchX = x + width - SWITCH_WIDTH - 4; // keep some padding
        int switchY = centerY - SWITCH_HEIGHT / 2;
        boolean active = Boolean.TRUE.equals(setting.get());
        if (animAmount < 0) animAmount = active ? 1 : 0;
        animAmount = (animAmount * 12f + (active ? 1f : 0f)) / 13f;

        int trackColor = Render2D.withAlpha(theme.getSecondary(), Math.min(240, opacity));
        int onColor = Render2D.withAlpha(theme.getAccentColor(), Math.min(255, opacity));
        int currentColor = Render2D.lerpColor(trackColor, onColor, animAmount);

        Render2D.shaderRoundedSurface(
                g,
                switchX, switchY, SWITCH_WIDTH, SWITCH_HEIGHT,
                SWITCH_HEIGHT / 2,
                currentColor,
                Render2D.withAlpha(0xFFFFFF, Math.min(40, opacity))
        );

        int knobSize = SWITCH_HEIGHT - 2;
        int knobY = switchY + 1;
        float knobXStart = switchX + 1;
        float knobXEnd = switchX + SWITCH_WIDTH - 1 - knobSize;
        int knobX = Math.round(knobXStart + animAmount * (knobXEnd - knobXStart));

        Render2D.shaderRoundedRect(g, knobX, knobY, knobSize, knobSize, knobSize / 2, Render2D.withAlpha(0xFFFFFF, Math.min(255, opacity + 50)));
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + HEIGHT) {
            setting.set(!Boolean.TRUE.equals(setting.get()));
            return true;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }
}
