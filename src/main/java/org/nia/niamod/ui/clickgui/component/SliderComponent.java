package org.nia.niamod.ui.clickgui.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.nia.niamod.config.setting.ConfigSetting;
import org.nia.niamod.config.setting.FloatSetting;
import org.nia.niamod.config.setting.IntSetting;
import org.nia.niamod.render.Render2D;
import org.nia.niamod.ui.clickgui.NiaClickGuiScreen;
import org.nia.niamod.ui.clickgui.theme.ClickGuiTheme;

import java.util.Locale;

public class SliderComponent {
    public static final int HEIGHT = 18;
    private static final int SLIDER_WIDTH = 92;
    private static final int TRACK_HEIGHT = 2;
    private static final int GRABBER_SIZE = 5;

    private final ConfigSetting<?> setting;
    private final float min;
    private final float max;
    private final boolean isInt;

    private int x, y, width;
    private int trackX, trackY;
    private boolean dragging;
    private float renderPercentage;
    private float dragSquish;

    public SliderComponent(ConfigSetting<?> setting) {
        this.setting = setting;
        if (setting instanceof IntSetting intSetting) {
            this.min = intSetting.getMin();
            this.max = intSetting.getMax();
            this.isInt = true;
        } else if (setting instanceof FloatSetting floatSetting) {
            this.min = floatSetting.getMin();
            this.max = floatSetting.getMax();
            this.isInt = false;
        } else {
            throw new IllegalArgumentException("SliderComponent requires IntSetting or FloatSetting");
        }
        this.renderPercentage = getProgress();
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

        float targetPercentage = getProgress();
        renderPercentage = (renderPercentage * 29 + targetPercentage) / 30;
        dragSquish *= 0.82f;

        String valueText = formatValue();
        int valueWidth = Math.max(22, NiaClickGuiScreen.styledWidth(font, valueText));
        int valueX = x + width - valueWidth;
        trackX = valueX - 8 - SLIDER_WIDTH;
        trackY = y + HEIGHT / 2 - TRACK_HEIGHT / 2;

        int bgColor = Render2D.withAlpha(theme.getBackground(), Math.min(254, opacity));
        int filledColor = Render2D.withAlpha(theme.getAccentColor(), Math.min(180, opacity));
        g.fill(trackX, trackY, trackX + SLIDER_WIDTH, trackY + TRACK_HEIGHT, bgColor);
        int fillWidth = Math.round(renderPercentage * SLIDER_WIDTH);
        g.fill(trackX, trackY, trackX + fillWidth, trackY + TRACK_HEIGHT, filledColor);

        float grabberCenterX = trackX + renderPercentage * SLIDER_WIDTH;
        int knobWidth = Math.max(2, GRABBER_SIZE - Math.round(dragSquish));
        int knobHeight = GRABBER_SIZE + Math.round(dragSquish * 0.65f);
        int gx = Math.round(grabberCenterX - knobWidth / 2.0f);
        int gy = y + HEIGHT / 2 - knobHeight / 2;
        int accent = Render2D.withAlpha(theme.getAccentColor(), Math.min(255, opacity));
        g.fill(gx, gy, gx + knobWidth, gy + knobHeight, accent);
        g.drawString(font, NiaClickGuiScreen.styled(valueText), valueX, y + 5, textColor, false);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= trackX - 3 && mouseX <= trackX + SLIDER_WIDTH + 3
                && mouseY >= y && mouseY <= y + HEIGHT) {
            dragging = true;
            updateValueFromMouse(mouseX);
            return true;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging) {
            dragSquish = Math.min(3.0f, dragSquish + (float) Math.abs(deltaX) * 0.35f);
            updateValueFromMouse(mouseX);
            return true;
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) {
            dragging = false;
            return true;
        }
        return false;
    }

    private float getProgress() {
        if (max == min) return 0;
        Number value = (Number) setting.get();
        return Math.max(0, Math.min(1, (value.floatValue() - min) / (max - min)));
    }

    private String formatValue() {
        if (isInt) return Integer.toString(((Number) setting.get()).intValue());
        return String.format(Locale.ROOT, "%.2f", ((Number) setting.get()).floatValue());
    }

    @SuppressWarnings("unchecked")
    private void updateValueFromMouse(double mouseX) {
        float progress = Math.max(0, Math.min(1, (float) (mouseX - trackX) / SLIDER_WIDTH));
        if (isInt) {
            int value = Math.round(min + progress * (max - min));
            ((ConfigSetting<Integer>) setting).set(value);
        } else {
            float value = min + progress * (max - min);
            ((ConfigSetting<Float>) setting).set(value);
        }
    }
}
