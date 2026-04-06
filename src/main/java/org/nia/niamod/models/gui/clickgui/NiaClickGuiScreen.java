package org.nia.niamod.models.gui.clickgui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.lwjgl.glfw.GLFW;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.config.setting.SettingCategory;
import org.nia.niamod.config.setting.SettingSection;
import org.nia.niamod.features.IgnoreFeature;
import org.nia.niamod.render.Render2D;
import org.nia.niamod.models.gui.clickgui.animation.Animation;
import org.nia.niamod.models.gui.clickgui.animation.Easing;
import org.nia.niamod.models.gui.clickgui.component.IgnoreSectionComponent;
import org.nia.niamod.models.gui.clickgui.component.SectionComponent;
import org.nia.niamod.models.gui.clickgui.render.UiRect;
import org.nia.niamod.models.gui.clickgui.theme.ClickGuiTheme;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class NiaClickGuiScreen extends Screen {
    private static final ClickGuiTheme THEME = ClickGuiTheme.defaultTheme();
    private static final int PANEL_W = 400, PANEL_H = 300;
    private static final int SIDEBAR_W = 100, ROUND = 12;
    private static final int MODULE_GAP = 7, MODULE_W = 283;
    private static final int SEARCH_BAR_HEIGHT = 24;

    private final Screen parent;
    private final Animation scaleAnim = new Animation(Easing.EASE_OUT_EXPO, 300);
    private final Animation opacityAnim = new Animation(Easing.EASE_OUT_EXPO, 300);
    private boolean closing;
    private double animTime;

    private float panelX, panelY;
    private boolean dragging;
    private double dragOffX, dragOffY;

    private enum Tab { SEARCH, GENERAL, WAR, SOCIAL }
    private Tab selectedTab = Tab.GENERAL;
    private final Map<SettingCategory, List<Object>> catComps = new EnumMap<>(SettingCategory.class);
    private List<Object> activeComps = new ArrayList<>();
    private final List<EditBox> textInputs = new ArrayList<>();
    private final List<TabBtn> tabBtns = new ArrayList<>();
    private final Animation[] tabAnims = new Animation[Tab.values().length];

    private boolean searchMode;
    private EditBox searchBox;
    private String searchQuery = "";
    private List<Object> allComps = new ArrayList<>();
    private List<Object> searchResults = new ArrayList<>();
    private long tabChangeTime;

    private double scrollTarget, scroll;
    private long lastFrameTime;

    public NiaClickGuiScreen(Screen parent) {
        super(Component.literal("NiaMod"));
        this.parent = parent;
    }

    public static Component styled(String text) {
        return Component.literal(text).withStyle(currentFontStyle());
    }

    public static int styledWidth(Font font, String text) {
        return font.width(styled(text));
    }

    private static Style currentFontStyle() {
        return Style.EMPTY;
    }

    public static void applyClickGuiFont(EditBox editBox, String hintText) {
        editBox.addFormatter((text, cursor) -> styled(text).getVisualOrderText());
        if (hintText != null) {
            editBox.setHint(styled(hintText));
        }
    }

    @Override
    protected void init() {
        panelX = (width - PANEL_W) / 2.0f;
        panelY = (height - PANEL_H) / 2.0f;

        textInputs.clear();

        searchBox = new EditBox(font, 0, 0, MODULE_W, 14, styled("Search"));
        searchBox.setBordered(false);
        searchBox.setTextColor(0xFFFFFFFF);
        applyClickGuiFont(searchBox, "Search...");
        searchBox.setResponder(q -> { searchQuery = q; updateSearch(); });
        searchBox.setCanLoseFocus(true);
        searchBox.visible = false;
        addRenderableWidget(searchBox);
        textInputs.add(searchBox);

        buildAll();
        selectTab(selectedTab);

        closing = false;
        scaleAnim.setEasing(Easing.EASE_OUT_EXPO);
        scaleAnim.setDuration(300);
        scaleAnim.setValue(0);
        scaleAnim.run(1);
        opacityAnim.setEasing(Easing.EASE_OUT_EXPO);
        opacityAnim.setDuration(300);
        opacityAnim.setValue(0);
        opacityAnim.run(1);

        lastFrameTime = System.currentTimeMillis();
        for (int i = 0; i < tabAnims.length; i++) {
            tabAnims[i] = new Animation(Easing.LINEAR, 200);
            tabAnims[i].setValue(selectedTab.ordinal() == i ? 255 : 0);
        }
    }

    private void buildAll() {
        catComps.clear();
        allComps.clear();
        for (SettingCategory cat : SettingCategory.values()) {
            List<Object> list = new ArrayList<>();
            for (SettingSection sec : NyahConfig.getSections(cat)) {
                Object comp = sec.getType() == SettingSection.SectionType.IGNORE_MANAGER
                        ? new IgnoreSectionComponent(sec)
                        : createSection(sec);
                list.add(comp);
                allComps.add(comp);
            }
            catComps.put(cat, list);
        }
    }

    private SectionComponent createSection(SettingSection sec) {
        SectionComponent sc = new SectionComponent(sec);
        for (EditBox box : sc.createEditBoxes(font, THEME)) {
            addRenderableWidget(box);
            textInputs.add(box);
        }
        return sc;
    }

    private void selectTab(Tab tab) {
        hideAllTextInputs();
        selectedTab = tab;
        searchMode = tab == Tab.SEARCH;
        scroll = scrollTarget = 0;
        tabChangeTime = System.currentTimeMillis();
        if (searchMode) {
            searchBox.visible = true;
            searchBox.active = true;
            searchBox.setEditable(true);
            searchBox.setFocused(true);
            updateSearch();
            activeComps = searchResults;
        } else {
            searchBox.visible = false;
            searchBox.active = false;
            searchBox.setEditable(false);
            searchBox.setFocused(false);
            SettingCategory cat = switch (tab) {
                case WAR -> SettingCategory.WAR;
                case SOCIAL -> SettingCategory.SOCIAL;
                default -> SettingCategory.GENERAL;
            };
            activeComps = catComps.getOrDefault(cat, List.of());
        }
    }

    private void updateSearch() {
        searchResults.clear();
        if (searchQuery.isEmpty()) { searchResults.addAll(allComps); }
        else {
            String q = searchQuery.toLowerCase();
            for (Object c : allComps) {
                if (matchesSearch(c, q)) searchResults.add(c);
            }
        }
        if (searchMode) activeComps = searchResults;
    }

    private String compTitle(Object c) {
        return c instanceof SectionComponent s ? s.getSection().getTitle() : c instanceof IgnoreSectionComponent ? "Ignore Manager" : "";
    }

    private String compDesc(Object c) {
        return c instanceof SectionComponent s ? s.getSection().getDescription() : c instanceof IgnoreSectionComponent ? "Guild-member tools" : "";
    }

    private boolean matchesSearch(Object component, String query) {
        if (containsIgnoreCase(compTitle(component), query) || containsIgnoreCase(compDesc(component), query)) {
            return true;
        }

        if (component instanceof SectionComponent sectionComponent) {
            return sectionComponent.getSection().getSettings().stream().anyMatch(setting ->
                    containsIgnoreCase(setting.getTitle(), query)
                            || containsIgnoreCase(setting.getDescription(), query)
                            || containsIgnoreCase(setting.getId(), query)
            );
        }

        if (component instanceof IgnoreSectionComponent ignoreSectionComponent) {
            SettingSection section = ignoreSectionComponent.getSection();
            IgnoreFeature ignoreFeature = section.getIgnoreFeature();
            return ignoreFeature != null && ignoreFeature.getGuildMembers().stream().anyMatch(member -> containsIgnoreCase(member, query));
        }

        return false;
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private boolean hasFocusedTextInput() {
        return textInputs.stream().anyMatch(EditBox::isFocused);
    }

    private void hideAllTextInputs() {
        for (EditBox editBox : textInputs) {
            editBox.setFocused(false);
            editBox.visible = false;
            editBox.active = false;
            editBox.setEditable(false);
            editBox.setX(-300);
            editBox.setY(-300);
        }
    }


    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float dt) {
        long now = System.currentTimeMillis();
        long frameDt = now - lastFrameTime;
        lastFrameTime = now;

        double lerpFactor = 1.0 - Math.pow(0.005, frameDt / 1000.0);
        scroll += (scrollTarget - scroll) * Math.min(lerpFactor, 0.6);
        if (Math.abs(scrollTarget - scroll) < 0.3) scroll = scrollTarget;

        if (closing) {
            scaleAnim.setEasing(Easing.LINEAR);
            scaleAnim.setDuration(100);
            scaleAnim.run(0);
            opacityAnim.setEasing(Easing.LINEAR);
            opacityAnim.setDuration(100);
            opacityAnim.run(0);
        }
        animTime = scaleAnim.getValue();
        if (closing && scaleAnim.isFinished()) { if (minecraft != null) minecraft.setScreen(parent); return; }
        if (animTime <= 0) return;

        if (dragging) {
            panelX = Math.max(-PANEL_W + 50, Math.min(width - 50, (float) (mouseX + dragOffX)));
            panelY = Math.max(0, Math.min(height - 30, (float) (mouseY + dragOffY)));
        }

        int px = Math.round(panelX), py = Math.round(panelY);
        double cx = px + PANEL_W / 2.0, cy = py + PANEL_H / 2.0;

        g.pose().pushMatrix();
        if (animTime != 1) {
            g.pose().translate((float) (cx * (1 - animTime)), (float) (cy * (1 - animTime)));
            g.pose().scale((float) animTime, (float) animTime);
        }

        if (animTime > 0.993) {
            Render2D.dropShadow(g, new UiRect(px, py, PANEL_W, PANEL_H), 6, 0x26000000, ROUND);
            Render2D.softGlow(g, new UiRect(px, py, PANEL_W, PANEL_H), 3, Render2D.withAlpha(THEME.getAccentColor(), 22), ROUND);
        }
        Render2D.roundedRect(g, px - 1, py - 1, PANEL_W + 2, PANEL_H + 2, ROUND + 1, 0x20FFFFFF);
        Render2D.roundedRect(g, px, py, PANEL_W, PANEL_H, ROUND, THEME.getBackground());

        g.enableScissor(px + 1, py + 1, px + PANEL_W - 1, py + PANEL_H - 1);

        int cx0 = px + SIDEBAR_W + 8;
        int cw = PANEL_W - SIDEBAR_W - 12;
        int viewH = PANEL_H - 14;
        int maxScroll = maxScroll(viewH);
        scrollTarget = Math.max(-maxScroll, Math.min(0, scrollTarget));
        scroll = Math.max(-maxScroll, Math.min(0, scroll));

        g.enableScissor(cx0, py + 1, px + PANEL_W - 4, py + PANEL_H - 1);

        if (searchMode) {
            renderSearchBar(g, cx0, py + 10, mouseX, mouseY);
            renderModules(g, cx0, py + 42, cw, mouseX, mouseY);
        } else {
            renderModules(g, cx0, py + 7, cw, mouseX, mouseY);
        }

        super.render(g, mouseX, mouseY, dt);
        g.disableScissor();

        renderSidebar(g, px, py, mouseX, mouseY);
        renderScrollbar(g, px + PANEL_W - 4, py + 7 + (searchMode ? 35 : 0), viewH - (searchMode ? 35 : 0), maxScroll);

        g.disableScissor();
        g.pose().popMatrix();
    }

    private void renderSidebar(GuiGraphics g, int px, int py, int mx, int my) {
        Render2D.roundedRect(g, px, py, SIDEBAR_W, PANEL_H, ROUND, THEME.getSecondary());
        g.fill(px + SIDEBAR_W - ROUND, py, px + SIDEBAR_W, py + PANEL_H, THEME.getSecondary());
        g.fill(px + SIDEBAR_W - 1, py + 10, px + SIDEBAR_W, py + PANEL_H - 10, 0x18FFFFFF);

        tabBtns.clear();
        int buttonX = px + 10;
        int buttonW = SIDEBAR_W - 20;
        double catY = py + 18;
        Tab[] tabs = Tab.values();
        String[] labels = {"Search", "General", "War", "Social"};

        for (int i = 0; i < tabs.length; i++) {
            int by = (int) Math.round(catY);
            int buttonH = 24;
            boolean sel = tabs[i] == selectedTab;
            boolean hovered = mx >= buttonX && mx <= buttonX + buttonW && my >= by && my <= by + buttonH;

            tabAnims[i].setDuration(200);
            tabAnims[i].run(sel ? 255 : 0);
            double sa = tabAnims[i].getValue();

            int buttonColor = hovered ? Render2D.withAlpha(0xFFFFFF, sel ? 24 : 10) : 0x00000000;
            if (buttonColor != 0) {
                Render2D.roundedRect(g, buttonX, by, buttonW, buttonH, 7, buttonColor);
            }

            if (sel) {
                Render2D.softGlow(g, new UiRect(buttonX, by, buttonW, buttonH), 2, Render2D.withAlpha(THEME.getAccentColor(), 24), 7);
                Render2D.roundedRect(g, buttonX, by, buttonW, buttonH, 7, Render2D.withAlpha(THEME.getAccentColor(), 32));
                Render2D.roundedRect(g, buttonX + 7, by + 10, 4, 4, 2, THEME.getAccentColor());
            }

            int textColor = sel ? 0xFFFFFFFF : 0xC8FFFFFF;
            float xOff = sel ? (float) (sa / 140.0) : 0.0f;
            g.drawString(font, styled(labels[i]), (int) (buttonX + 16 + xOff), by + 8, textColor, false);
            tabBtns.add(new TabBtn(tabs[i], buttonX, by, buttonW, buttonH));
            catY += 30;
        }
    }

    private void renderSearchBar(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        UiRect rect = new UiRect(x, y, MODULE_W, SEARCH_BAR_HEIGHT);
        boolean focused = searchBox.isFocused();
        boolean hovered = mouseX >= rect.x() && mouseX <= rect.right() && mouseY >= rect.y() && mouseY <= rect.bottom();
        int fill = focused
                ? Render2D.withAlpha(THEME.getSecondary(), 245)
                : hovered ? Render2D.withAlpha(THEME.getSecondary(), 228) : Render2D.withAlpha(THEME.getSecondary(), 214);
        int border = focused ? Render2D.withAlpha(THEME.getAccentColor(), 150) : 0x24FFFFFF;

        Render2D.softGlow(g, rect, focused ? 2 : 1, Render2D.withAlpha(THEME.getAccentColor(), focused ? 26 : 12), 8);
        Render2D.roundedRect(g, rect.x(), rect.y(), rect.width(), rect.height(), 8, fill);
        Render2D.outline(g, rect, border);
        Render2D.circle(g, rect.x() + 12, rect.y() + rect.height() / 2, 5, focused ? THEME.getAccentColor() : 0x78FFFFFF);

        searchBox.setX(rect.x() + 22);
        searchBox.setY(rect.y() + 7);
        searchBox.setWidth(rect.width() - 58);
        searchBox.visible = true;
        searchBox.active = true;
        searchBox.setEditable(true);

        String count = searchResults.isEmpty() ? "0" : Integer.toString(searchResults.size());
        int countWidth = font.width(count);
        g.drawString(font, styled(count), rect.right() - countWidth - 10, rect.y() + 8, 0x96FFFFFF, false);
    }

    private void renderModules(GuiGraphics g, int x, int y, int w, int mx, int my) {
        double moduleY = y + scroll;
        int mw = Math.min(MODULE_W, w);
        for (Object c : activeComps) {
            setPos(c, x, (int) Math.round(moduleY), mw);
            renderComp(c, g, font, mx, my);
            moduleY += getH(c) + MODULE_GAP;
        }
    }

    private void renderScrollbar(GuiGraphics g, int sx, int sy, int sh, int maxScroll) {
        if (maxScroll <= 0) return;
        int thumbH = Math.max(15, Math.round(sh * (float) sh / (sh + maxScroll)));
        float progress = (float) (-scroll / maxScroll);
        int thumbY = sy + Math.round((sh - thumbH) * progress);
        g.fill(sx, thumbY, sx + 1, thumbY + thumbH, THEME.getScrollbarColor());
    }

    private int totalHeight() {
        int t = 0;
        for (int i = 0; i < activeComps.size(); i++) {
            t += getH(activeComps.get(i));
            if (i < activeComps.size() - 1) t += MODULE_GAP;
        }
        return t;
    }

    private int maxScroll(int viewH) {
        return Math.max(0, totalHeight() - viewH + (searchMode ? 42 : 7));
    }


    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean outside) {
        double mx = click.x(), my = click.y();
        int btn = click.button();
        int px = Math.round(panelX), py = Math.round(panelY);

        for (TabBtn tb : tabBtns) if (mx >= tb.x && mx <= tb.x + tb.w && my >= tb.y && my <= tb.y + tb.h) { selectTab(tb.tab); return true; }
        if (btn == 0 && isDragZone(mx, my, px, py)) {
            dragging = true;
            dragOffX = panelX - mx;
            dragOffY = panelY - my;
            return true;
        }
        for (Object c : activeComps) if (compClick(c, mx, my, btn)) return true;
        return super.mouseClicked(click, outside);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging) {
            panelX = Math.max(-PANEL_W + 50, Math.min(width - 50, (float) (event.x() + dragOffX)));
            panelY = Math.max(0, Math.min(height - 30, (float) (event.y() + dragOffY)));
            return true;
        }
        for (Object c : activeComps) if (compDrag(c, event.x(), event.y(), event.button(), dx, dy)) return true;
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging) { dragging = false; return true; }
        for (Object c : activeComps) if (compRelease(c, event.x(), event.y(), event.button())) return true;
        return super.mouseReleased(event);
    }   

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        int px = Math.round(panelX), py = Math.round(panelY);
        if (mx >= px + SIDEBAR_W && mx <= px + PANEL_W && my >= py && my <= py + PANEL_H) { scrollTarget += v * 30; return true; }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (searchMode && !searchQuery.isEmpty()) { searchBox.setValue(""); return true; }
            onClose();
            return true;
        }
        if (!searchMode
                && !closing
                && !hasFocusedTextInput()
                && event.key() >= GLFW.GLFW_KEY_A
                && event.key() <= GLFW.GLFW_KEY_Z) {
            selectTab(Tab.SEARCH);
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        if (!closing) {
            hideAllTextInputs();
            closing = true;
            scaleAnim.setEasing(Easing.LINEAR); scaleAnim.setDuration(100); scaleAnim.run(0);
            opacityAnim.setEasing(Easing.LINEAR); opacityAnim.setDuration(100); opacityAnim.run(0);
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private boolean isDragZone(double mx, double my, int px, int py) {
        if (mx >= px && mx <= px + PANEL_W && my >= py && my <= py + 8) {
            return true;
        }

        if (mx < px || mx > px + SIDEBAR_W || my < py || my > py + PANEL_H) {
            return false;
        }

        for (TabBtn tabBtn : tabBtns) {
            if (mx >= tabBtn.x && mx <= tabBtn.x + tabBtn.w && my >= tabBtn.y && my <= tabBtn.y + tabBtn.h) {
                return false;
            }
        }

        return true;
    }


    private int getH(Object c) { return c instanceof SectionComponent s ? s.getHeight() : c instanceof IgnoreSectionComponent i ? i.getHeight() : 0; }
    private void setPos(Object c, int x, int y, int w) { if (c instanceof SectionComponent s) s.setPosition(x, y, w); else if (c instanceof IgnoreSectionComponent i) i.setPosition(x, y, w); }
    private void renderComp(Object c, GuiGraphics g, Font f, int mx, int my) { if (c instanceof SectionComponent s) s.render(g, f, mx, my, THEME); else if (c instanceof IgnoreSectionComponent i) i.render(g, f, mx, my, THEME); }
    private boolean compClick(Object c, double mx, double my, int btn) { return c instanceof SectionComponent s ? s.mouseClicked(mx, my, btn) : c instanceof IgnoreSectionComponent i && i.mouseClicked(mx, my, btn); }
    private boolean compDrag(Object c, double mx, double my, int btn, double dx, double dy) { return c instanceof SectionComponent s ? s.mouseDragged(mx, my, btn, dx, dy) : c instanceof IgnoreSectionComponent i && i.mouseDragged(mx, my, btn, dx, dy); }
    private boolean compRelease(Object c, double mx, double my, int btn) { return c instanceof SectionComponent s ? s.mouseReleased(mx, my, btn) : c instanceof IgnoreSectionComponent i && i.mouseReleased(mx, my, btn); }

    private record TabBtn(Tab tab, int x, int y, int w, int h) {}
}
