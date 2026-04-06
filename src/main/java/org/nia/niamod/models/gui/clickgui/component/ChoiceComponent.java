package org.nia.niamod.models.gui.clickgui.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.nia.niamod.config.setting.ChoiceSetting;
import org.nia.niamod.render.Render2D;
import org.nia.niamod.models.gui.clickgui.NiaClickGuiScreen;
import org.nia.niamod.models.gui.clickgui.render.UiRect;
import org.nia.niamod.models.gui.clickgui.theme.ClickGuiTheme;

public class ChoiceComponent {
    public static final int HEIGHT = 16;
    private static final int CONTROL_WIDTH = 118;
    private static final int ARROW_WIDTH = 16;

    private final ChoiceSetting setting;
    private int x;
    private int y;
    private int width;

    public ChoiceComponent(ChoiceSetting setting) {
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
        g.drawString(font, NiaClickGuiScreen.styled(setting.getTitle()), x, y + 3, textColor, false);

        UiRect control = controlRect();
        boolean hovered = mouseX >= control.x() && mouseX <= control.right()
                && mouseY >= control.y() && mouseY <= control.bottom();

        int fillColor = hovered
                ? Render2D.withAlpha(theme.getSecondary(), Math.min(245, opacity + 24))
                : Render2D.withAlpha(theme.getSecondary(), Math.min(225, opacity));
        Render2D.roundedRect(g, control.x(), control.y(), control.width(), control.height(), 0, fillColor);

        int dividerColor = Render2D.withAlpha(0xFFFFFF, Math.min(36, textAlpha));
        g.fill(control.x() + ARROW_WIDTH, control.y() + 2, control.x() + ARROW_WIDTH + 1, control.bottom() - 2, dividerColor);
        g.fill(control.right() - ARROW_WIDTH - 1, control.y() + 2, control.right() - ARROW_WIDTH, control.bottom() - 2, dividerColor);

        int arrowColor = hovered ? 0xFFFFFFFF : 0xC8FFFFFF;
        g.drawString(font, NiaClickGuiScreen.styled("<"), control.x() + 5, control.y() + 3, arrowColor, false);
        g.drawString(font, NiaClickGuiScreen.styled(">"), control.right() - 9, control.y() + 3, arrowColor, false);

        String label = setting.displayValue(setting.get());
        int labelWidth = NiaClickGuiScreen.styledWidth(font, label);
        int labelX = control.x() + (control.width() - labelWidth) / 2;
        g.drawString(font, NiaClickGuiScreen.styled(label), labelX, control.y() + 3, textColor, false);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }

        UiRect control = controlRect();
        if (mouseX < control.x() || mouseX > control.right() || mouseY < control.y() || mouseY > control.bottom()) {
            return false;
        }

        if (mouseX <= control.x() + ARROW_WIDTH) {
            setting.previous();
        } else {
            setting.next();
        }
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    private UiRect controlRect() {
        int controlX = x + Math.max(0, width - CONTROL_WIDTH);
        return new UiRect(controlX, y, CONTROL_WIDTH, 14);
    }
}
