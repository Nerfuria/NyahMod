package org.nia.niamod.models.gui.clickgui.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import org.nia.niamod.config.setting.StringSetting;
import org.nia.niamod.models.gui.clickgui.NiaClickGuiScreen;
import org.nia.niamod.models.gui.clickgui.theme.ClickGuiTheme;

public class StringInputComponent {
    public static final int HEIGHT = 28;

    private final StringSetting setting;
    private EditBox editBox;
    private int x, y, width;

    public StringInputComponent(StringSetting setting) {
        this.setting = setting;
    }

    public EditBox createEditBox(Font font, ClickGuiTheme theme) {
        editBox = new EditBox(font, 0, 0, 230, 14, NiaClickGuiScreen.styled(setting.getTitle()));
        editBox.setBordered(false);
        editBox.setTextColor(0xFFFFFFFF);
        editBox.setTextColorUneditable(0x82FFFFFF);
        NiaClickGuiScreen.applyClickGuiFont(editBox, setting.getTitle());
        editBox.setValue(setting.format());
        editBox.setResponder(setting::tryParseAndSet);
        return editBox;
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
        g.drawString(font, NiaClickGuiScreen.styled(setting.getTitle()), x, y + 2, textColor, false);

        if (editBox != null) {
            editBox.setX(x);
            editBox.setY(y + 14);
            editBox.setWidth(Math.min(230, width - 12));
            boolean active = opacity > 10;
            editBox.visible = active;
            editBox.active = active;
            editBox.setEditable(active);
            if (!active && editBox.isFocused()) {
                editBox.setFocused(false);
            }
            if (!editBox.isFocused()) {
                editBox.setValue(setting.format());
            }
        }
    }

    public void updateClipVisibility(int clipTop, int clipBottom) {
        if (editBox == null) return;
        int editY = y + 14;
        int editBottom = editY + 14;
        boolean visible = editBottom > clipTop && editY < clipBottom;
        if (!visible) {
            hide();
        }
    }

    public void hide() {
        if (editBox != null) {
            editBox.setX(-300);
            editBox.setY(-300);
            editBox.setFocused(false);
            editBox.visible = false;
            editBox.active = false;
            editBox.setEditable(false);
        }
    }

    public EditBox getEditBox() {
        return editBox;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }
}
