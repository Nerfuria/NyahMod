package org.nia.niamod.models.gui.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.nia.niamod.config.setting.ColorSetting;
import org.nia.niamod.models.gui.NiaClickGuiScreen;
import org.nia.niamod.models.gui.theme.ClickGuiTheme;
import org.nia.niamod.render.Render2D;

public class ColorPickerComponent {
    public static final int CLOSED_HEIGHT = 15;
    public static final int OPENED_HEIGHT = 110;
    private static final int PICKER_WIDTH = 105;
    private static final int PICKER_ROUND = 8;

    private final ColorSetting setting;
    private boolean expanded;
    private float hue, saturation, value;
    private boolean draggingSV;
    private boolean draggingHue;

    private int x, y, width;
    private int cachedLabelWidth;

    public ColorPickerComponent(ColorSetting setting) {
        this.setting = setting;
        float[] hsv = Render2D.rgbToHsv(setting.get() & 0xFFFFFF);
        this.hue = hsv[0];
        this.saturation = hsv[1];
        this.value = hsv[2];
    }

    public void setPosition(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    public int getHeight() {
        return expanded ? OPENED_HEIGHT : CLOSED_HEIGHT;
    }

    public void render(GuiGraphics g, Font font, int mouseX, int mouseY, ClickGuiTheme theme, int opacity) {
        int textAlpha = Math.min(220, opacity);
        int textColor = (textAlpha << 24) | 0xFFFFFF;

        g.drawString(font, NiaClickGuiScreen.styled(setting.getTitle()), x, y + 2, textColor, false);

        cachedLabelWidth = NiaClickGuiScreen.styledWidth(font, setting.getTitle());

        int swatchX = x + cachedLabelWidth + 4;
        int swatchY = y + 2;
        int currentColor = 0xFF000000 | Render2D.hsvToRgb(hue, saturation, value);
        Render2D.roundedRect(g, swatchX, swatchY, 15, 7, 3, currentColor);

        if (!expanded) return;

        int pickerX = x + 10 + cachedLabelWidth + 8;
        int pickerY = y;
        int pickerH = OPENED_HEIGHT - 15;

        Render2D.dropShadow(g, pickerX, pickerY, PICKER_WIDTH, pickerH, 10, 40, PICKER_ROUND);
        Render2D.roundedRect(g, pickerX - 1, pickerY, PICKER_WIDTH + 1, pickerH, PICKER_ROUND, theme.getSecondary());
        Render2D.roundedRect(g, pickerX, pickerY + 1, PICKER_WIDTH - 1, pickerH - 2, PICKER_ROUND, theme.getBackground());

        int gradX = pickerX;
        int gradY = pickerY;
        int gradW = PICKER_WIDTH - 1;
        int gradH = (int) (pickerH * 0.55f);
        renderSVField(g, gradX, gradY, gradW, gradH);

        int crossX = gradX + Math.round(saturation * gradW);
        int crossY = gradY + Math.round((1.0f - value) * gradH);
        Render2D.roundedRect(g, crossX - 3, crossY - 3, 7, 7, 3, 0xFFFFFFFF);
        Render2D.roundedRect(g, crossX - 3, crossY - 3, 6, 6, 3, 0xFF000000);
        Render2D.roundedRect(g, crossX - 2, crossY - 2, 5, 5, 2, currentColor);

        double padding = 8.5;
        int hueBarX = (int) (pickerX + padding);
        int hueBarY = (int) (gradY + padding + gradH - 5 + 2.5);
        int hueBarW = (int) (PICKER_WIDTH - padding * 2);
        int hueBarH = 7;
        renderHueBar(g, hueBarX, hueBarY, hueBarW, hueBarH);

        float huePointer = hue / 360.0f * hueBarW;
        int markerSize = 11;
        int markerX = hueBarX + Math.round(huePointer) - markerSize / 2;
        Render2D.roundedRect(g, markerX - 1, hueBarY - 1, markerSize + 2, hueBarH + 2, 4, 0xFF000000);
        Render2D.roundedRect(g, markerX, hueBarY, markerSize, hueBarH, 3,
                0xFF000000 | Render2D.hsvToRgb(hue, 1.0f, 1.0f));

        int previewY = (int) (gradY + gradH + padding + padding + 7 - 11);
        Render2D.dropShadow(g, (int) (pickerX + padding), previewY, 15, 15, 10, 40, 6);
        Render2D.roundedRect(g, (int) (pickerX + padding), previewY, 15, 15, 3, currentColor);

        int r = (currentColor >> 16) & 0xFF;
        int gr = (currentColor >> 8) & 0xFF;
        int b = currentColor & 0xFF;
        int textX = (int) (pickerX + padding * 2 + 15);
        g.drawString(font, NiaClickGuiScreen.styled(String.valueOf(r)), textX + 9, previewY + 3, 0xDCFFFFFF, false);
        g.drawString(font, NiaClickGuiScreen.styled(String.valueOf(gr)), textX + 30, previewY + 3, 0xDCFFFFFF, false);
        g.drawString(font, NiaClickGuiScreen.styled(String.valueOf(b)), textX + 51, previewY + 3, 0xDCFFFFFF, false);

        String hexText = String.format("#%02X%02X%02X", r, gr, b);
        g.drawString(font, NiaClickGuiScreen.styled(hexText), textX, previewY + 16, 0xFF373B3D, false);
    }

    private void renderSVField(GuiGraphics g, int fx, int fy, int fw, int fh) {
        int step = 2;
        for (int gx = 0; gx < fw; gx += step) {
            for (int gy = 0; gy < fh; gy += step) {
                float s = (float) gx / fw;
                float v = 1.0f - (float) gy / fh;
                int color = 0xFF000000 | Render2D.hsvToRgb(hue, s, v);
                g.fill(fx + gx, fy + gy, fx + gx + step, fy + gy + step, color);
            }
        }
    }

    private void renderHueBar(GuiGraphics g, int bx, int by, int bw, int bh) {
        for (int i = 0; i < bw; i++) {
            float h = (float) i / bw * 360.0f;
            int color = 0xFF000000 | Render2D.hsvToRgb(h, 1.0f, 1.0f);
            g.fill(bx + i, by, bx + i + 1, by + bh, color);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + CLOSED_HEIGHT) {
            expanded = !expanded;
            return true;
        }

        if (!expanded) return false;

        int pickerX = x + 10 + cachedLabelWidth + 8;
        int pickerY = y;
        int pickerH = OPENED_HEIGHT - 15;
        int gradW = PICKER_WIDTH - 1;
        int gradH = (int) (pickerH * 0.55f);

        if (mouseX >= pickerX && mouseX <= pickerX + gradW && mouseY >= pickerY && mouseY <= pickerY + gradH) {
            draggingSV = true;
            updateSV(mouseX, mouseY, pickerX, pickerY, gradW, gradH);
            return true;
        }

        double padding = 8.5;
        int hueBarX = (int) (pickerX + padding);
        int hueBarY = (int) (pickerY + padding + gradH - 5 + 2.5);
        int hueBarW = (int) (PICKER_WIDTH - padding * 2);
        int hueBarH = 7;
        if (mouseX >= hueBarX && mouseX <= hueBarX + hueBarW && mouseY >= hueBarY - 2 && mouseY <= hueBarY + hueBarH + 2) {
            draggingHue = true;
            updateHue(mouseX, hueBarX, hueBarW);
            return true;
        }

        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingSV) {
            int pickerX = x + 10 + cachedLabelWidth + 8;
            int pickerY = y;
            int pickerH = OPENED_HEIGHT - 15;
            int gradW = PICKER_WIDTH - 1;
            int gradH = (int) (pickerH * 0.55f);
            updateSV(mouseX, mouseY, pickerX, pickerY, gradW, gradH);
            return true;
        }
        if (draggingHue) {
            double padding = 8.5;
            int pickerX = x + 10 + cachedLabelWidth + 8;
            int hueBarX = (int) (pickerX + padding);
            int hueBarW = (int) (PICKER_WIDTH - padding * 2);
            updateHue(mouseX, hueBarX, hueBarW);
            return true;
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingSV || draggingHue) {
            draggingSV = false;
            draggingHue = false;
            return true;
        }
        return false;
    }

    private void updateSV(double mouseX, double mouseY, int gradX, int gradY, int gradW, int gradH) {
        saturation = Math.max(0, Math.min(1, (float) (mouseX - gradX) / gradW));
        value = Math.max(0, Math.min(1, 1.0f - (float) (mouseY - gradY) / gradH));
        setting.set(Render2D.hsvToRgb(hue, saturation, value));
    }

    private void updateHue(double mouseX, int hueBarX, int hueBarW) {
        hue = Math.max(0, Math.min(359.9f, (float) (mouseX - hueBarX) / hueBarW * 360.0f));
        setting.set(Render2D.hsvToRgb(hue, saturation, value));
    }
}
