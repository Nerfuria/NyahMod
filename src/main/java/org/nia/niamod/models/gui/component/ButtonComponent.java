package org.nia.niamod.models.gui.component;

import lombok.RequiredArgsConstructor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.nia.niamod.config.setting.ButtonSetting;
import org.nia.niamod.models.gui.NiaClickGuiScreen;
import org.nia.niamod.models.gui.theme.ClickGuiTheme;
import org.nia.niamod.render.Render2D;

@RequiredArgsConstructor
public class ButtonComponent {
    public static final int HEIGHT = 18;
    private static final int BUTTON_WIDTH = 60;
    private static final int BUTTON_HEIGHT = 14;

    private final ButtonSetting setting;
    private int x, y, width;

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

        int buttonX = x + width - BUTTON_WIDTH - 4;
        int buttonY = centerY - BUTTON_HEIGHT / 2;
        boolean hovered = mouseX >= buttonX && mouseX <= buttonX + BUTTON_WIDTH && mouseY >= buttonY && mouseY <= buttonY + BUTTON_HEIGHT;

        int buttonFill = hovered ? Render2D.withAlpha(theme.getSecondary(), Math.min(242, opacity)) : Render2D.withAlpha(theme.getSecondary(), Math.min(224, opacity));
        int buttonBorder = hovered ? Render2D.withAlpha(0xFFFFFF, Math.min(56, opacity)) : Render2D.withAlpha(0xFFFFFF, Math.min(26, opacity));

        Render2D.shaderRoundedSurface(
                g,
                buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT,
                4, buttonFill, buttonBorder
        );

        String btnText = setting.getButtonText();
        if (btnText != null) {
            int tw = font.width(NiaClickGuiScreen.styled(btnText));
            g.drawString(font, NiaClickGuiScreen.styled(btnText), buttonX + (BUTTON_WIDTH - tw) / 2, buttonY + (BUTTON_HEIGHT - font.lineHeight) / 2 + 1, textColor, false);
        }
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int centerY = y + HEIGHT / 2;
            int buttonX = x + width - BUTTON_WIDTH - 4;
            int buttonY = centerY - BUTTON_HEIGHT / 2;
            if (mx >= buttonX && mx <= buttonX + BUTTON_WIDTH && my >= buttonY && my <= buttonY + BUTTON_HEIGHT) {
                Runnable action = setting.get();
                if (action != null) {
                    action.run();
                }
                return true;
            }
        }
        return false;
    }
}

