package org.nia.niamod.models.gui.component;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
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
    private static final float DEFAULT_HEIGHT = 38;
    private static final int MODULE_WIDTH = 283;
    private static final int SETTING_START_X_OFFSET = 6;

    private final SettingSection section;
    private final List<Object> children = new ArrayList<>();
    private final Animation opening = new Animation(Easing.EASE_OUT_EXPO, 200);
    private final Animation settingOpacity = new Animation(Easing.LINEAR, 100);
    private final Animation hoverAnimation = new Animation(Easing.LINEAR, 50);
    private boolean expanded;
    private int x, y, width;
    private boolean mouseDown;

    public SectionComponent(SettingSection section) {
        this.section = section;
        opening.setValue(DEFAULT_HEIGHT);
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
        };
    }

    public List<EditBox> createEditBoxes(Font font, ClickGuiTheme theme) {
        List<EditBox> boxes = new ArrayList<>();
        for (Object child : children) {
            if (child instanceof StringInputComponent stringComp) {
                boxes.add(stringComp.createEditBox(font, theme));
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
        return (int) Math.round(opening.getValue());
    }

    public void render(GuiGraphics g, Font font, int mouseX, int mouseY, ClickGuiTheme theme) {
        float expandedHeight = calculateExpandedHeight();
        opening.setDuration(Math.min((long) expandedHeight * 3, 450));
        opening.setEasing(Easing.EASE_OUT_EXPO);
        opening.run(expanded ? expandedHeight : DEFAULT_HEIGHT);
        settingOpacity.setDuration(expanded ? opening.getDuration() / 2 : opening.getDuration() / 3);
        settingOpacity.run(expanded ? 255 : 0);

        int totalHeight = getHeight();
        Render2D.roundedRect(g, x, y, width, totalHeight, 8, theme.getOverlay());

        boolean headerHovered = mouseX >= x && mouseX <= x + width
                && mouseY >= y && mouseY <= y + DEFAULT_HEIGHT;
        hoverAnimation.run(headerHovered ? mouseDown ? 35 : 20 : 0);
        if (hoverAnimation.getValue() > 0.5) {
            Render2D.roundedRect(g, x, y, width, totalHeight, 8,
                    Render2D.withAlpha(0x000000, (int) hoverAnimation.getValue()));
        }

        boolean enabled = !section.hasToggle() || section.isEnabled();
        int nameColor = (enabled && section.hasToggle())
                ? (0xFF000000 | (theme.getAccentColor() & 0x00FFFFFF))
                : 0xC8FFFFFF;
        g.drawString(font, NiaClickGuiScreen.styled(section.title()), x + 6, y + 8, nameColor, false);
        g.drawString(font, NiaClickGuiScreen.styled(section.description()), x + 6, y + 25, 0x46FFFFFF, false);

        int clipTop = y + (int) DEFAULT_HEIGHT;
        int clipBottom = y + totalHeight;

        if (totalHeight > DEFAULT_HEIGHT + 1) {
            int opacity = (int) settingOpacity.getValue();
            g.enableScissor(x, clipTop, x + width, clipBottom);

            float childYOffset = DEFAULT_HEIGHT;
            for (Object child : children) {
                int childX = x + SETTING_START_X_OFFSET;
                int childY = y + (int) childYOffset + 1;
                int childW = width - SETTING_START_X_OFFSET * 2;
                setChildPosition(child, childX, childY, childW);
                renderChild(child, g, font, mouseX, mouseY, theme, opacity);
                updateChildClipVisibility(child, clipTop, clipBottom);
                childYOffset += getChildHeight(child);
            }

            g.disableScissor();
        } else {
            hideAllChildren();
        }
    }

    private float calculateExpandedHeight() {
        float totalChildHeight = DEFAULT_HEIGHT;
        for (Object child : children) totalChildHeight += getChildHeight(child);
        return totalChildHeight - 1;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + DEFAULT_HEIGHT) {
            mouseDown = true;
            if (button == 0) {
                if (section.hasToggle()) {
                    section.setEnabled(!section.isEnabled());
                } else if (!children.isEmpty()) {
                    expanded = !expanded;
                }
                return true;
            }
            if (button == 1 && !children.isEmpty()) {
                expanded = !expanded;
                return true;
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
        }
    }

    private void updateChildClipVisibility(Object child, int clipTop, int clipBottom) {
        if (child instanceof StringInputComponent s) {
            s.updateClipVisibility(clipTop, clipBottom);
        }
    }

    private void setChildPosition(Object child, int cx, int cy, int cw) {
        if (child instanceof BooleanComponent c) c.setPosition(cx, cy, cw);
        else if (child instanceof ChoiceComponent c) c.setPosition(cx, cy, cw);
        else if (child instanceof SliderComponent c) c.setPosition(cx, cy, cw);
        else if (child instanceof ColorPickerComponent c) c.setPosition(cx, cy, cw);
        else if (child instanceof StringInputComponent c) c.setPosition(cx, cy, cw);
    }

    private int getChildHeight(Object child) {
        if (child instanceof BooleanComponent) return BooleanComponent.HEIGHT;
        if (child instanceof ChoiceComponent) return ChoiceComponent.HEIGHT;
        if (child instanceof SliderComponent) return SliderComponent.HEIGHT;
        if (child instanceof ColorPickerComponent c) return c.getHeight();
        if (child instanceof StringInputComponent) return StringInputComponent.HEIGHT;
        return 0;
    }

    private void renderChild(Object child, GuiGraphics g, Font font, int mx, int my, ClickGuiTheme theme, int opacity) {
        if (child instanceof BooleanComponent c) c.render(g, font, mx, my, theme, opacity);
        else if (child instanceof ChoiceComponent c) c.render(g, font, mx, my, theme, opacity);
        else if (child instanceof SliderComponent c) c.render(g, font, mx, my, theme, opacity);
        else if (child instanceof ColorPickerComponent c) c.render(g, font, mx, my, theme, opacity);
        else if (child instanceof StringInputComponent c) c.render(g, font, mx, my, theme, opacity);
    }

    private boolean childMouseClicked(Object child, double mx, double my, int button) {
        if (child instanceof BooleanComponent c) return c.mouseClicked(mx, my, button);
        if (child instanceof ChoiceComponent c) return c.mouseClicked(mx, my, button);
        if (child instanceof SliderComponent c) return c.mouseClicked(mx, my, button);
        if (child instanceof ColorPickerComponent c) return c.mouseClicked(mx, my, button);
        if (child instanceof StringInputComponent c) return c.mouseClicked(mx, my, button);
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
        if (child instanceof StringInputComponent c) return c.mouseReleased(mx, my, button);
        return false;
    }
}
