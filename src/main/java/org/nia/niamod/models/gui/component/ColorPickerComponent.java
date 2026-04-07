package org.nia.niamod.models.gui.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import org.nia.niamod.config.setting.ColorSetting;
import org.nia.niamod.models.gui.NiaClickGuiScreen;
import org.nia.niamod.models.gui.render.GradientQuad;
import org.nia.niamod.models.gui.render.UiRect;
import org.nia.niamod.models.gui.theme.ClickGuiTheme;
import org.nia.niamod.render.Render2D;

public class ColorPickerComponent {
    public static final int CLOSED_HEIGHT = 18;
    public static final int OPENED_HEIGHT = 116;

    private final ColorSetting setting;
    private boolean expanded;
    private float hue, saturation, value;
    private boolean draggingSV;
    private boolean draggingHue;

    private int x, y, width;
    private EditBox hexInput;

    public ColorPickerComponent(ColorSetting setting) {
        this.setting = setting;
        float[] hsv = Render2D.rgbToHsv(setting.get() & 0xFFFFFF);
        this.hue = hsv[0];
        this.saturation = hsv[1];
        this.value = hsv[2];
    }

    public EditBox createEditBox(Font font, ClickGuiTheme theme) {
        int inputHeight = Math.max(16, font.lineHeight + 4);
        hexInput = new EditBox(font, 0, 0, 46, inputHeight, NiaClickGuiScreen.styled("Hex"));
        hexInput.setBordered(false);
        hexInput.setHeight(inputHeight);
        hexInput.setTextColor(0xFFFFFFFF);
        NiaClickGuiScreen.applyClickGuiFont(hexInput, "Hex");
        hexInput.setValue(getHex());
        hexInput.setResponder(this::onHexChanged);
        return hexInput;
    }

    private String getHex() {
        int currentColor = Render2D.hsvToRgb(hue, saturation, value);
        return String.format("#%06X", currentColor);
    }

    private void onHexChanged(String text) {
        if (draggingHue || draggingSV) return;
        if (text.startsWith("#")) text = text.substring(1);
        if (text.length() == 6) {
            try {
                int rgb = Integer.parseInt(text, 16);
                setting.set(rgb);
                float[] hsv = Render2D.rgbToHsv(rgb);
                this.hue = hsv[0];
                this.saturation = hsv[1];
                this.value = hsv[2];
            } catch (NumberFormatException ignored) {}
        }
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
        int centerY = y + CLOSED_HEIGHT / 2;

        g.drawString(font, NiaClickGuiScreen.styled(setting.getTitle()), x, centerY - font.lineHeight / 2 + 1, textColor, false);

        int swatchSize = 12;
        int swatchX = x + width - swatchSize - 4;
        int swatchY = centerY - swatchSize / 2;
        int currentColor = 0xFF000000 | Render2D.hsvToRgb(hue, saturation, value);

        Render2D.shaderRoundedSurface(
                g,
                swatchX - 1,
                swatchY - 1,
                swatchSize + 2,
                swatchSize + 2,
                4,
                Render2D.withAlpha(currentColor, opacity),
                Render2D.withAlpha(0xFFFFFF, Math.min(72, opacity))
        );

        int hexWidth = 50;
        int hexX = swatchX - hexWidth - 8;
        int hexHeight = hexInput != null ? hexInput.getHeight() : Math.max(18, font.lineHeight + 6);
        int hexY = centerY - hexHeight / 2;

        if (hexInput != null) {
            int hexBorder = hexInput.isFocused()
                    ? Render2D.withAlpha(theme.getAccentColor(), Math.min(120, opacity + 30))
                    : Render2D.withAlpha(0xFFFFFF, Math.min(34, opacity / 6 + 18));
            Render2D.shaderRoundedSurface(
                    g,
                    hexX,
                    hexY,
                    hexWidth,
                    hexHeight,
                    5,
                    Render2D.withAlpha(theme.getSecondary(), Math.min(200, opacity)),
                    hexBorder
            );

            boolean active = opacity > 10;
            hexInput.visible = active;
            hexInput.active = active;
            hexInput.setEditable(active);
            if (!hexInput.isFocused()) {
                hexInput.setValue(getHex());
                hexInput.moveCursorToStart(false);
            }
            NiaClickGuiScreen.layoutBorderlessEditBox(hexInput, font, hexX + 6, hexY, hexWidth - 12, hexHeight);
        }

        if (!expanded) {
            return;
        }

        int panelX = x + 4;
        int panelY = y + 20;
        int panelW = width - 8;
        int panelH = OPENED_HEIGHT - 24;

        Render2D.shaderRoundedSurface(
                g,
                panelX,
                panelY,
                panelW,
                panelH,
                6,
                Render2D.withAlpha(theme.getSecondary(), opacity),
                Render2D.withAlpha(0xFFFFFF, Math.min(28, opacity / 6 + 16))
        );

        int svX = panelX + 4;
        int svY = panelY + 4;
        int svW = panelW - 8;
        int svH = 70;
        renderSVField(g, svX, svY, svW, svH, opacity);
        Render2D.outline(g, new UiRect(svX - 1, svY - 1, svW + 2, svH + 2), Render2D.withAlpha(0xFFFFFF, Math.min(40, opacity)));

        int crossX = svX + Math.round(saturation * svW);
        int crossY = svY + Math.round((1.0f - value) * svH);
        Render2D.circle(g, crossX, crossY, 6, Render2D.withAlpha(0xFFFFFF, opacity));
        Render2D.circle(g, crossX, crossY, 4, Render2D.withAlpha(currentColor, opacity));

        int hueX = panelX + 4;
        int hueY = svY + svH + 6;
        int hueW = panelW - 8;
        int hueH = 8;
        renderHueBar(g, hueX, hueY, hueW, hueH, opacity);
        Render2D.outline(g, new UiRect(hueX - 1, hueY - 1, hueW + 2, hueH + 2), Render2D.withAlpha(0xFFFFFF, Math.min(40, opacity)));

        float huePointer = hue / 360.0f * hueW;
        int markerX = hueX + Math.round(huePointer);
        Render2D.shaderRoundedRect(g, markerX - 2, hueY - 1, 4, hueH + 2, 2, Render2D.withAlpha(0xFFFFFF, opacity));
    }

    private void renderSVField(GuiGraphics g, int fx, int fy, int fw, int fh, int opacity) {
        g.guiRenderState.submitGuiElement(new GradientQuad(
                g.pose(),
                fx,
                fx + fw,
                fy,
                fy + fh,
                g.scissorStack.peek(),
                Render2D.withAlpha(Render2D.hsvToRgb(hue, 1.0f, 1.0f), opacity)
        ));
    }

    private void renderHueBar(GuiGraphics g, int bx, int by, int bw, int bh, int opacity) {
        for (int i = 0; i < bw; i++) {
            float h = (float) i / bw * 360.0f;
            int color = Render2D.withAlpha(Render2D.hsvToRgb(h, 1.0f, 1.0f), opacity);
            g.fill(bx + i, by, bx + i + 1, by + bh, color);
        }
    }

    public void updateClipVisibility(int clipTop, int clipBottom) {
        if (hexInput == null) return;
        int centerY = y + CLOSED_HEIGHT / 2;
        int editY = centerY - hexInput.getHeight() / 2;
        int editBottom = editY + hexInput.getHeight();
        boolean visible = editY >= clipTop && editBottom <= clipBottom;
        if (!visible) {
            hide();
        }
    }

    public void hide() {
        if (hexInput != null) {
            hexInput.setX(-300);
            hexInput.setY(-300);
            hexInput.setFocused(false);
            hexInput.visible = false;
            hexInput.active = false;
            hexInput.setEditable(false);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + CLOSED_HEIGHT) {
            int swatchSize = 12;
            int swatchX = x + width - swatchSize - 4;
            int hexWidth = 50;
            int hexX = swatchX - hexWidth - 6;

            if (mouseX >= hexX && mouseX <= hexX + hexWidth) {
                return false;
            }

            expanded = !expanded;
            return true;
        }

        if (!expanded) return false;

        int panelX = x + 4;
        int panelY = y + 20;
        int panelW = width - 8;

        int svX = panelX + 4;
        int svY = panelY + 4;
        int svW = panelW - 8;
        int svH = 70;

        if (mouseX >= svX && mouseX <= svX + svW && mouseY >= svY && mouseY <= svY + svH) {
            draggingSV = true;
            updateSV(mouseX, mouseY, svX, svY, svW, svH);
            return true;
        }

        int hueX = panelX + 4;
        int hueY = svY + svH + 6;
        int hueW = panelW - 8;
        int hueH = 8;

        if (mouseX >= hueX && mouseX <= hueX + hueW && mouseY >= hueY - 2 && mouseY <= hueY + hueH + 2) {
            draggingHue = true;
            updateHue(mouseX, hueX, hueW);
            return true;
        }

        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingSV) {
            int panelX = x + 4;
            int panelY = y + 20;
            int panelW = width - 8;
            int svX = panelX + 4;
            int svY = panelY + 4;
            int svW = panelW - 8;
            int svH = 70;
            updateSV(mouseX, mouseY, svX, svY, svW, svH);
            return true;
        }
        if (draggingHue) {
            int panelX = x + 4;
            int panelW = width - 8;
            int hueX = panelX + 4;
            int hueW = panelW - 8;
            updateHue(mouseX, hueX, hueW);
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
