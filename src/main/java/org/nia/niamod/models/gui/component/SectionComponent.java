package org.nia.niamod.models.gui.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.FormattedCharSequence;
import org.nia.niamod.config.setting.BooleanSetting;
import org.nia.niamod.config.setting.ChoiceSetting;
import org.nia.niamod.config.setting.ColorSetting;
import org.nia.niamod.config.setting.ConfigSetting;
import org.nia.niamod.config.setting.SettingSection;
import org.nia.niamod.config.setting.StringSetting;
import org.nia.niamod.models.gui.NiaClickGuiScreen;
import org.nia.niamod.models.gui.animation.Animation;
import org.nia.niamod.models.gui.animation.Easing;
import org.nia.niamod.models.gui.theme.ClickGuiTheme;
import org.nia.niamod.render.Render2D;

import java.util.ArrayList;
import java.util.List;

public class SectionComponent {
    private static final int MIN_HEADER_HEIGHT = 38;
    private static final int SETTING_START_X_OFFSET = 6;
    private static final int CONTENT_TOP_PADDING = 6;
    private static final int CONTENT_BOTTOM_PADDING = 6;
    private static final int CHILD_VERTICAL_GAP = 4;
    private static final int HEADER_TEXT_X = 6;
    private static final int HEADER_SWITCH_RIGHT_PADDING = 8;
    private static final int HEADER_SWITCH_TEXT_GAP = 8;
    private static final int HEADER_TOP_PADDING = 8;
    private static final int HEADER_TITLE_DESC_GAP = 7;
    private static final int HEADER_LINE_GAP = 2;
    private static final int HEADER_BOTTOM_PADDING = 6;

    private final SettingSection section;
    private final List<Object> children = new ArrayList<>();
    private final Animation opening = new Animation(Easing.EASE_OUT_EXPO, 200);
    private final Animation settingOpacity = new Animation(Easing.LINEAR, 100);
    private final Animation hoverAnimation = new Animation(Easing.LINEAR, 50);
    private float headerToggleAnim = -1;
    private boolean expanded;
    private int x, y, width;
    private int viewportClipTop = Integer.MIN_VALUE;
    private int viewportClipBottom = Integer.MAX_VALUE;
    private int headerHeight = MIN_HEADER_HEIGHT;
    private boolean mouseDown;

    public SectionComponent(SettingSection section) {
        this.section = section;
        opening.setValue(MIN_HEADER_HEIGHT);
        settingOpacity.setValue(0);

        for (ConfigSetting<?> setting : section.settings()) {
            children.add(createChild(setting));
        }
    }

    public SettingSection getSection() {
        return section;
    }

    private Object createChild(ConfigSetting<?> setting) {
        return switch (setting.getKind()) {
            case BOOLEAN -> new BooleanComponent((BooleanSetting) setting);
            case INTEGER, FLOAT -> new SliderComponent(setting);
            case CHOICE -> new ChoiceComponent((ChoiceSetting) setting);
            case COLOR -> new ColorPickerComponent((ColorSetting) setting);
            case STRING -> new StringInputComponent((StringSetting) setting);
            case BUTTON -> new ButtonComponent((org.nia.niamod.config.setting.ButtonSetting) setting);
        };
    }

    public List<EditBox> createEditBoxes(Font font, ClickGuiTheme theme) {
        List<EditBox> boxes = new ArrayList<>();
        for (Object child : children) {
            if (child instanceof StringInputComponent stringComp) {
                boxes.add(stringComp.createEditBox(font, theme));
            } else if (child instanceof SliderComponent sliderComp) {
                boxes.add(sliderComp.createEditBox(font, theme));
            } else if (child instanceof ColorPickerComponent colorComp) {
                boxes.add(colorComp.createEditBox(font, theme));
            }
        }
        return boxes;
    }

    public void setPosition(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    public int getHeight() {
        return Math.max(headerHeight, (int) Math.round(opening.getValue()));
    }

    public void setViewportClip(int top, int bottom) {
        this.viewportClipTop = top;
        this.viewportClipBottom = bottom;
    }

    public void syncStateImmediately() {
        opening.setValue(expanded ? calculateExpandedHeight() : headerHeight);
        settingOpacity.setValue(expanded ? 255 : 0);
        hoverAnimation.setValue(0);
        headerToggleAnim = section.hasToggle() && section.isEnabled() ? 1 : 0;
    }

    public void render(GuiGraphics g, Font font, int mouseX, int mouseY, ClickGuiTheme theme) {
        headerHeight = calculateHeaderHeight(font);
        updateChildLayouts(font);
        float expandedHeight = calculateExpandedHeight();
        opening.setDuration(Math.min((long) expandedHeight * 3, 450));
        opening.setEasing(Easing.EASE_OUT_EXPO);
        opening.run(expanded ? expandedHeight : headerHeight);
        settingOpacity.setDuration(expanded ? opening.getDuration() / 2 : opening.getDuration() / 3);
        settingOpacity.run(expanded ? 255 : 0);

        int totalHeight = getHeight();
        Render2D.shaderRoundedRect(g, x, y, width, totalHeight, 8, theme.getOverlay());

        boolean headerHovered = mouseX >= x && mouseX <= x + width
                && mouseY >= y && mouseY <= y + headerHeight;
        hoverAnimation.run(headerHovered ? mouseDown ? 35 : 20 : 0);
        if (hoverAnimation.getValue() > 0.5) {
            Render2D.shaderRoundedRect(g, x, y, width, headerHeight, 8,
                    Render2D.withAlpha(0x000000, (int) hoverAnimation.getValue()));
        }

        boolean enabled = !section.hasToggle() || section.isEnabled();
        int nameColor = enabled
                ? (0xFF000000 | (theme.getAccentColor() & 0x00FFFFFF))
                : 0xC8FFFFFF;
        renderHeaderText(g, font, nameColor);

        if (section.hasToggle()) {
            headerToggleAnim = animateHeaderToggle(enabled);
            BooleanComponent.renderToggle(
                    g,
                    theme,
                    headerSwitchX(),
                    headerSwitchY(),
                    headerToggleAnim,
                    255
            );
        }

        int clipTop = y + headerHeight;
        int clipBottom = y + totalHeight;

        if (totalHeight > headerHeight + 1) {
            int opacity = (int) settingOpacity.getValue();
            g.enableScissor(x, clipTop, x + width, clipBottom);

            float childYOffset = headerHeight + CONTENT_TOP_PADDING;
            for (Object child : children) {
                int childX = x + SETTING_START_X_OFFSET;
                int childY = y + Math.round(childYOffset);
                int childW = width - SETTING_START_X_OFFSET * 2;
                setChildPosition(child, childX, childY, childW);
                renderChild(child, g, font, mouseX, mouseY, theme, opacity);
                updateChildClipVisibility(child, clipTop, clipBottom);
                childYOffset += getChildHeight(child) + CHILD_VERTICAL_GAP;
            }

            g.disableScissor();
        } else {
            hideAllChildren();
        }
    }

    private void renderHeaderText(GuiGraphics g, Font font, int nameColor) {
        int textX = x + HEADER_TEXT_X;
        int textWidth = headerTextWidth();
        int textY = y + HEADER_TOP_PADDING;

        List<FormattedCharSequence> titleLines = wrappedText(font, section.title(), textWidth);
        for (FormattedCharSequence line : titleLines) {
            g.drawString(font, line, textX, textY, nameColor, false);
            textY += font.lineHeight + HEADER_LINE_GAP;
        }

        List<FormattedCharSequence> descriptionLines = wrappedText(font, section.description(), textWidth);
        if (!titleLines.isEmpty() && !descriptionLines.isEmpty()) {
            textY += HEADER_TITLE_DESC_GAP - HEADER_LINE_GAP;
        }
        for (FormattedCharSequence line : descriptionLines) {
            g.drawString(font, line, textX, textY, 0x46FFFFFF, false);
            textY += font.lineHeight + HEADER_LINE_GAP;
        }
    }

    private int calculateHeaderHeight(Font font) {
        int textWidth = headerTextWidth();
        int titleHeight = textHeight(font, wrappedText(font, section.title(), textWidth).size());
        int descriptionHeight = textHeight(font, wrappedText(font, section.description(), textWidth).size());
        int titleDescriptionGap = titleHeight > 0 && descriptionHeight > 0 ? HEADER_TITLE_DESC_GAP : 0;
        int textHeight = titleHeight + titleDescriptionGap + descriptionHeight;
        return Math.max(MIN_HEADER_HEIGHT, HEADER_TOP_PADDING + textHeight + HEADER_BOTTOM_PADDING);
    }

    private int textHeight(Font font, int lineCount) {
        if (lineCount <= 0) {
            return 0;
        }
        return lineCount * font.lineHeight + (lineCount - 1) * HEADER_LINE_GAP;
    }

    private List<FormattedCharSequence> wrappedText(Font font, String text, int maxWidth) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return font.split(NiaClickGuiScreen.styled(text), Math.max(1, maxWidth));
    }

    private int headerTextWidth() {
        int textX = x + HEADER_TEXT_X;
        int textRight = section.hasToggle()
                ? headerSwitchX() - HEADER_SWITCH_TEXT_GAP
                : x + width - HEADER_TEXT_X;
        return Math.max(1, textRight - textX);
    }

    private float calculateExpandedHeight() {
        if (children.isEmpty()) {
            return headerHeight;
        }

        float totalChildHeight = headerHeight + CONTENT_TOP_PADDING + CONTENT_BOTTOM_PADDING;
        for (int i = 0; i < children.size(); i++) {
            totalChildHeight += getChildHeight(children.get(i));
            if (i < children.size() - 1) {
                totalChildHeight += CHILD_VERTICAL_GAP;
            }
        }
        return totalChildHeight;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + headerHeight) {
            mouseDown = true;
            if (button == 0 && section.hasToggle() && BooleanComponent.isOverSwitch(mouseX, mouseY, headerSwitchX(), headerSwitchY())) {
                section.setEnabled(!section.isEnabled());
                return true;
            }
            if (!children.isEmpty()) {
                expanded = !expanded;
            }
            return true;
        }

        if (expanded) {
            for (Object child : children) {
                if (childMouseClicked(child, mouseX, mouseY, button)) return true;
            }
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        for (Object child : children) {
            if (childMouseDragged(child, mouseX, mouseY, button, deltaX, deltaY)) return true;
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        mouseDown = false;
        if (expanded) {
            for (Object child : children) {
                if (childMouseReleased(child, mouseX, mouseY, button)) return true;
            }
        }
        return false;
    }

    private void hideAllChildren() {
        for (Object child : children) {
            if (child instanceof StringInputComponent s) s.hide();
            else if (child instanceof SliderComponent s) s.hide();
            else if (child instanceof ColorPickerComponent c) c.hide();
        }
    }

    private void updateChildClipVisibility(Object child, int clipTop, int clipBottom) {
        int effectiveClipTop = Math.max(clipTop, viewportClipTop);
        int effectiveClipBottom = Math.min(clipBottom, viewportClipBottom);
        if (child instanceof StringInputComponent s) {
            s.updateClipVisibility(effectiveClipTop, effectiveClipBottom);
        } else if (child instanceof SliderComponent s) {
            s.updateClipVisibility(effectiveClipTop, effectiveClipBottom);
        } else if (child instanceof ColorPickerComponent c) {
            c.updateClipVisibility(effectiveClipTop, effectiveClipBottom);
        }
    }

    private void updateChildLayouts(Font font) {
        int childW = Math.max(1, width - SETTING_START_X_OFFSET * 2);
        for (Object child : children) {
            updateChildLayout(child, font, childW);
        }
    }

    private void setChildPosition(Object child, int cx, int cy, int cw) {
        if (child instanceof BooleanComponent c) c.setPosition(cx, cy, cw);
        else if (child instanceof ChoiceComponent c) c.setPosition(cx, cy, cw);
        else if (child instanceof SliderComponent c) c.setPosition(cx, cy, cw);
        else if (child instanceof ColorPickerComponent c) c.setPosition(cx, cy, cw);
        else if (child instanceof StringInputComponent c) c.setPosition(cx, cy, cw);
        else if (child instanceof ButtonComponent c) c.setPosition(cx, cy, cw);
    }

    private void updateChildLayout(Object child, Font font, int cw) {
        if (child instanceof BooleanComponent c) c.updateLabelLayout(font, cw);
        else if (child instanceof ChoiceComponent c) c.updateLabelLayout(font, cw);
        else if (child instanceof SliderComponent c) c.updateLabelLayout(font, cw);
        else if (child instanceof ColorPickerComponent c) c.updateLabelLayout(font, cw);
        else if (child instanceof StringInputComponent c) c.updateLabelLayout(font, cw);
        else if (child instanceof ButtonComponent c) c.updateLabelLayout(font, cw);
    }

    private int getChildHeight(Object child) {
        if (child instanceof BooleanComponent c) return c.getHeight();
        if (child instanceof ChoiceComponent c) return c.getHeight();
        if (child instanceof SliderComponent c) return c.getHeight();
        if (child instanceof ColorPickerComponent c) return c.getHeight();
        if (child instanceof StringInputComponent c) return c.getHeight();
        if (child instanceof ButtonComponent c) return c.getHeight();
        return 0;
    }

    private void renderChild(Object child, GuiGraphics g, Font font, int mx, int my, ClickGuiTheme theme, int opacity) {
        if (child instanceof BooleanComponent c) c.render(g, font, mx, my, theme, opacity);
        else if (child instanceof ChoiceComponent c) c.render(g, font, mx, my, theme, opacity);
        else if (child instanceof SliderComponent c) c.render(g, font, mx, my, theme, opacity);
        else if (child instanceof ColorPickerComponent c) c.render(g, font, mx, my, theme, opacity);
        else if (child instanceof StringInputComponent c) c.render(g, font, mx, my, theme, opacity);
        else if (child instanceof ButtonComponent c) c.render(g, font, mx, my, theme, opacity);
    }

    private boolean childMouseClicked(Object child, double mx, double my, int button) {
        if (child instanceof BooleanComponent c) return c.mouseClicked(mx, my, button);
        if (child instanceof ChoiceComponent c) return c.mouseClicked(mx, my, button);
        if (child instanceof SliderComponent c) return c.mouseClicked(mx, my, button);
        if (child instanceof ColorPickerComponent c) return c.mouseClicked(mx, my, button);
        if (child instanceof StringInputComponent c) return c.mouseClicked(mx, my, button);
        if (child instanceof ButtonComponent c) return c.mouseClicked(mx, my, button);
        return false;
    }

    private boolean childMouseDragged(Object child, double mx, double my, int button, double dx, double dy) {
        if (child instanceof BooleanComponent c) return c.mouseDragged(mx, my, button, dx, dy);
        if (child instanceof ChoiceComponent c) return c.mouseDragged(mx, my, button, dx, dy);
        if (child instanceof SliderComponent c) return c.mouseDragged(mx, my, button, dx, dy);
        if (child instanceof ColorPickerComponent c) return c.mouseDragged(mx, my, button, dx, dy);
        if (child instanceof StringInputComponent c) return c.mouseDragged(mx, my, button, dx, dy);
        return false;
    }

    private boolean childMouseReleased(Object child, double mx, double my, int button) {
        if (child instanceof BooleanComponent c) return c.mouseReleased(mx, my, button);
        if (child instanceof ChoiceComponent c) return c.mouseReleased(mx, my, button);
        if (child instanceof SliderComponent c) return c.mouseReleased(mx, my, button);
        if (child instanceof ColorPickerComponent c) return c.mouseReleased(mx, my, button);
        return false;
    }

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    private int headerSwitchX() {
        return x + width - BooleanComponent.switchWidth() - HEADER_SWITCH_RIGHT_PADDING;
    }

    private int headerSwitchY() {
        return y + Math.round(headerHeight / 2f) - BooleanComponent.switchHeight() / 2;
    }

    private float animateHeaderToggle(boolean enabled) {
        if (headerToggleAnim < 0) {
            headerToggleAnim = enabled ? 1 : 0;
        }
        return headerToggleAnim = (headerToggleAnim * 4f + (enabled ? 1f : 0f)) / 5f;
    }

}
