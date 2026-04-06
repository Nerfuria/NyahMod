package org.nia.niamod.models.gui.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.nia.niamod.config.setting.BooleanSetting;
import org.nia.niamod.models.gui.NiaClickGuiScreen;
import org.nia.niamod.models.gui.render.UiRect;
import org.nia.niamod.models.gui.theme.ClickGuiTheme;
import org.nia.niamod.render.Render2D;

public class BooleanComponent {
    public static final int HEIGHT = 18;
    private static final int SWITCH_WIDTH = 18;
    private static final int SWITCH_HEIGHT = 10;

    private final BooleanSetting setting;
    private int x, y, width;

    public BooleanComponent(BooleanSetting setting) {
        this.setting = setting;
    }

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
        g.drawString(font, NiaClickGuiScreen.styled(setting.getTitle()), x, y + 5, textColor, false);

        int switchX = x + width - SWITCH_WIDTH;
        int switchY = y + (HEIGHT - SWITCH_HEIGHT) / 2;
        int trackColor = Render2D.withAlpha(theme.getSecondary(), Math.min(240, opacity));
        int onColor = Render2D.withAlpha(theme.getAccentColor(), Math.min(255, opacity));
        Render2D.toggle(
                g,
                new UiRect(switchX, switchY, SWITCH_WIDTH, SWITCH_HEIGHT),
                Boolean.TRUE.equals(setting.get()),
                onColor,
                trackColor,
                0xFFFFFFFF
        );
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
