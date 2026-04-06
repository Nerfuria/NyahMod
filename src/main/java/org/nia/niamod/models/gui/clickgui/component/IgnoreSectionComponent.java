package org.nia.niamod.models.gui.clickgui.component;

import lombok.Getter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.nia.niamod.config.setting.SettingSection;
import org.nia.niamod.features.IgnoreFeature;
import org.nia.niamod.models.records.State;
import org.nia.niamod.render.Render2D;
import org.nia.niamod.models.gui.clickgui.NiaClickGuiScreen;
import org.nia.niamod.models.gui.clickgui.animation.Animation;
import org.nia.niamod.models.gui.clickgui.animation.Easing;
import org.nia.niamod.models.gui.clickgui.theme.ClickGuiTheme;

import java.util.ArrayList;
import java.util.List;

public class IgnoreSectionComponent {
    private static final float DEFAULT_HEIGHT = 38;
    private static final int SUMMARY_HEIGHT = 18;
    private static final int ROW_HEIGHT = 18;
    private static final int ROW_GAP = 4;
    private static final int MAX_VISIBLE = 8;

    @Getter
    private final SettingSection section;

    private final Animation opening = new Animation(Easing.EASE_OUT_EXPO, 300);
    private final Animation settingOpacity = new Animation(Easing.LINEAR, 150);
    private final Animation hoverAnimation = new Animation(Easing.LINEAR, 50);

    private boolean expanded;
    private int x, y, width;
    private boolean mouseDown;

    private final List<PillAction> pillActions = new ArrayList<>();

    public IgnoreSectionComponent(SettingSection section) {
        this.section = section;
        opening.setValue(DEFAULT_HEIGHT);
        settingOpacity.setValue(0);
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
        int opacity = (int) settingOpacity.getValue();
        pillActions.clear();

        Render2D.roundedRect(g, x, y, width, totalHeight, 8, theme.getOverlay());

        boolean headerHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + DEFAULT_HEIGHT;
        hoverAnimation.run(headerHovered ? mouseDown ? 35 : 20 : 0);
        if (hoverAnimation.getValue() > 0.5) {
            Render2D.roundedRect(g, x, y, width, totalHeight, 8,
                    Render2D.withAlpha(0x000000, (int) hoverAnimation.getValue()));
        }

        boolean enabled = !section.hasToggle() || section.isEnabled();
        int nameColor = enabled && section.hasToggle()
                ? (0xFF000000 | (theme.getAccentColor() & 0x00FFFFFF))
                : 0xC8FFFFFF;
        g.drawString(font, NiaClickGuiScreen.styled(section.getTitle()), x + 6, y + 8, nameColor, false);
        g.drawString(font, NiaClickGuiScreen.styled(section.getDescription()), x + 6, y + 25, 0x46FFFFFF, false);

        if (totalHeight > DEFAULT_HEIGHT + 1) {
            IgnoreFeature ignoreFeature = section.getIgnoreFeature();
            int contentX = x + 6;
            int contentY = y + (int) DEFAULT_HEIGHT + 2;
            int contentWidth = width - 12;
            int clipTop = y + (int) DEFAULT_HEIGHT;
            int clipBottom = y + totalHeight;

            g.enableScissor(x, clipTop, x + width, clipBottom);

            if (ignoreFeature == null) {
                if (opacity > 0) {
                    g.drawString(font, NiaClickGuiScreen.styled("Unavailable"), contentX, contentY + 2,
                            Render2D.withAlpha(0xFFFFFF, Math.min(130, opacity)), false);
                }
            } else {
                List<String> members = ignoreFeature.getGuildMembers();
                if (members.isEmpty()) {
                    if (opacity > 0) {
                        g.drawString(font, NiaClickGuiScreen.styled("No members loaded"), contentX, contentY + 2,
                                Render2D.withAlpha(0xFFFFFF, Math.min(130, opacity)), false);
                    }
                } else {
                    renderSummary(g, font, ignoreFeature, theme, opacity, contentX, contentY, contentWidth);

                    int rows = Math.min(members.size(), MAX_VISIBLE);
                    int rowsStartY = contentY + SUMMARY_HEIGHT + 5;
                    for (int i = 0; i < rows; i++) {
                        String username = members.get(i);
                        int rowY = rowsStartY + i * (ROW_HEIGHT + ROW_GAP);

                        if (opacity > 0 && rowY < y + opening.getValue() + 15) {
                            int rowColor = Render2D.withAlpha(theme.getSecondary(), Math.min(210, opacity));
                            Render2D.roundedRect(g, contentX, rowY, contentWidth, ROW_HEIGHT, 6, rowColor);

                            g.drawString(font, NiaClickGuiScreen.styled(username), contentX + 6, rowY + 5,
                                    Render2D.withAlpha(0xFFFFFF, Math.min(220, opacity)), false);

                            State state = ignoreFeature.getState(username);
                            int stateColor = switch (state) {
                                case FAVOURITE -> 0xFFC95050;
                                case AVOID -> 0xFF5A6573;
                                case NORMAL -> 0xFF373B3D;
                            };

                            int stateX = contentX + contentWidth - 96;
                            int buttonY = rowY + 2;
                            Render2D.roundedRect(g, stateX, buttonY, 42, 14, 5, stateColor);
                            String stateLabel = switch (state) {
                                case FAVOURITE -> "FAV";
                                case AVOID -> "AVOID";
                                case NORMAL -> "NORMAL";
                            };
                            int stateTextW = NiaClickGuiScreen.styledWidth(font, stateLabel);
                            g.drawString(font, NiaClickGuiScreen.styled(stateLabel), stateX + (42 - stateTextW) / 2, buttonY + 3, 0xFFFFFFFF, false);

                            int ignX = contentX + contentWidth - 48;
                            boolean ignored = ignoreFeature.isIgnored(username);
                            int ignColor = ignored ? (0xFF000000 | (theme.getAccentColor() & 0x00FFFFFF)) : 0xFF373B3D;
                            Render2D.roundedRect(g, ignX, buttonY, 42, 14, 5, ignColor);
                            String ignLabel = ignored ? "REMOVE" : "IGNORE";
                            int ignTextW = NiaClickGuiScreen.styledWidth(font, ignLabel);
                            g.drawString(font, NiaClickGuiScreen.styled(ignLabel), ignX + (42 - ignTextW) / 2, buttonY + 3, 0xFFFFFFFF, false);

                            pillActions.add(new PillAction(username, PillType.STATE, contentX, rowY, contentWidth - 54, ROW_HEIGHT));
                            pillActions.add(new PillAction(username, PillType.IGNORE, ignX, buttonY, 42, 14));
                        }
                    }
                }
            }
            g.disableScissor();
        }
    }

    private float calculateExpandedHeight() {
        IgnoreFeature ignoreFeature = section.getIgnoreFeature();
        if (ignoreFeature == null || ignoreFeature.getGuildMembers().isEmpty()) {
            return DEFAULT_HEIGHT + ROW_HEIGHT + 3;
        }

        int rows = Math.min(ignoreFeature.getGuildMembers().size(), MAX_VISIBLE);
        return DEFAULT_HEIGHT + SUMMARY_HEIGHT + 5 + rows * (ROW_HEIGHT + ROW_GAP) + 3;
    }

    private void renderSummary(
            GuiGraphics g,
            Font font,
            IgnoreFeature ignoreFeature,
            ClickGuiTheme theme,
            int opacity,
            int contentX,
            int contentY,
            int contentWidth
    ) {
        int favourites = 0;
        int avoided = 0;
        for (String member : ignoreFeature.getGuildMembers()) {
            State state = ignoreFeature.getState(member);
            if (state == State.FAVOURITE) {
                favourites++;
            } else if (state == State.AVOID) {
                avoided++;
            }
        }

        Render2D.roundedRect(g, contentX, contentY, contentWidth, SUMMARY_HEIGHT, 6,
                Render2D.withAlpha(theme.getSecondary(), Math.min(205, opacity)));
        String summary = "Guild " + ignoreFeature.getGuildMembers().size() + "  Fav " + favourites + "  Avoid " + avoided;
        g.drawString(font, NiaClickGuiScreen.styled(summary), contentX + 6, contentY + 5,
                Render2D.withAlpha(0xFFFFFF, Math.min(180, opacity)), false);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + DEFAULT_HEIGHT) {
            mouseDown = true;
            if (button == 0 && section.hasToggle()) {
                section.setEnabled(!section.isEnabled());
                return true;
            }
            if (button == 1) {
                expanded = !expanded;
                return true;
            }
            return true;
        }

        if (!expanded) return false;

        IgnoreFeature ignoreFeature = section.getIgnoreFeature();
        if (ignoreFeature == null) return false;

        for (PillAction action : pillActions) {
            if (mouseX >= action.x && mouseX <= action.x + action.w
                    && mouseY >= action.y && mouseY <= action.y + action.h) {
                if (action.type == PillType.STATE) {
                    ignoreFeature.cycleState(action.username);
                } else {
                    ignoreFeature.ignore(action.username, !ignoreFeature.isIgnored(action.username));
                }
                return true;
            }
        }

        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        mouseDown = false;
        return false;
    }

    private record PillAction(String username, PillType type, int x, int y, int w, int h) {}

    private enum PillType {STATE, IGNORE}
}
