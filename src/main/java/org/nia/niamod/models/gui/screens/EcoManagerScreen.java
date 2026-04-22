package org.nia.niamod.models.gui.screens;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import org.nia.niamod.NiamodClient;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.features.EcoMenuFeature;
import org.nia.niamod.managers.FeatureManager;
import org.nia.niamod.models.api.TerritoryResponse;
import org.nia.niamod.models.gui.component.EcoMenu;
import org.nia.niamod.models.gui.component.EcoMenu.DamageRange;
import org.nia.niamod.models.gui.component.EcoMenu.DetailData;
import org.nia.niamod.models.gui.component.TerritoryQuickMenu;
import org.nia.niamod.models.gui.component.TerritoryResourceSummaryWidget;
import org.nia.niamod.models.gui.component.TerritoryResourceSummaryWidget.ResourceFlow;
import org.nia.niamod.models.gui.component.TerritoryWidget;
import org.nia.niamod.models.gui.render.UiRect;
import org.nia.niamod.models.territory.ResourceAmounts;
import org.nia.niamod.models.territory.Resources;
import org.nia.niamod.models.territory.TerritoryNode;
import org.nia.niamod.models.territory.TerritoryResourceColors;
import org.nia.niamod.models.territory.TerritoryResourceStore;
import org.nia.niamod.models.territory.TerritoryLoadout;
import org.nia.niamod.models.territory.TerritoryUpgrade;
import org.nia.niamod.models.gui.theme.ClickGuiTheme;
import org.nia.niamod.render.Render2D;
import org.nia.niamod.util.FileUtils;
import org.nia.niamod.util.WynncraftAPI;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class EcoManagerScreen extends Screen {
    private static final Gson GSON = new Gson();
    private static final Type STATIC_TERRITORY_DATA_TYPE = new TypeToken<Map<String, StaticTerritoryData>>() {
    }.getType();

    private static final int MAP_PADDING = 28;
    private static final int CONNECTION_DASH = 11;
    private static final int CONNECTION_GAP = 7;
    private static final int MAX_CONNECTION_DASHES_PER_EDGE = 6;
    private static final int LINE_CLIP_PADDING = 18;
    private static final int LOADOUT_BUTTON_W = 86;
    private static final int LOADOUT_BUTTON_H = 22;
    private static final int LOADOUT_BUTTON_GAP = 6;
    private static final double MIN_ZOOM = 0.35;
    private static final double MAX_ZOOM = 6.0;
    private static final double ZOOM_STEP = 1.16;
    private static final StaticTerritoryData EMPTY_STATIC_DATA = new StaticTerritoryData(Resources.EMPTY, List.of());

    private final Screen parent;
    private final EcoMenuFeature ecoFeature;
    private final Map<String, StaticTerritoryData> staticTerritoryData;
    private final List<TerritoryWidget> territoryWidgets = new ArrayList<>();
    private final List<TerritoryWidget> visibleTerritoryWidgets = new ArrayList<>();
    private final List<TerritoryLink> territoryLinks = new ArrayList<>();
    private final List<Line> visibleConnections = new ArrayList<>();
    private final Map<String, TerritoryWidget> widgetsByName = new HashMap<>();
    private final Map<String, TerritoryWidget> widgetsByNormalizedName = new HashMap<>();
    private final EcoMenu detailPanel = new EcoMenu();
    private final TerritoryQuickMenu quickMenu = new TerritoryQuickMenu();
    private final TerritoryResourceSummaryWidget resourceSummaryWidget = new TerritoryResourceSummaryWidget();
    private final LoadoutManager loadoutManager = new LoadoutManager();

    private boolean loadRequested;
    private boolean loading = true;
    private boolean draggingMap;
    private boolean layoutDirty = true;
    private int lastLayoutWidth = -1;
    private int lastLayoutHeight = -1;
    private double lastLayoutPanX = Double.NaN;
    private double lastLayoutPanY = Double.NaN;
    private double lastLayoutZoom = Double.NaN;
    private double panX;
    private double panY;
    private double zoom = 1.0;
    private final String guildName;
    private String status = "Loading territory data...";
    private MapBounds mapBounds = MapBounds.EMPTY;
    private TerritoryWidget selectedTerritory;

    public EcoManagerScreen(Screen parent) {
        this(parent, FeatureManager.getEcoMenuFeature());
    }

    public EcoManagerScreen(Screen parent, EcoMenuFeature ecoFeature) {
        super(Component.literal("Eco Manager"));
        this.parent = parent;
        this.ecoFeature = ecoFeature == null ? new EcoMenuFeature() : ecoFeature;
        this.guildName = configuredGuildName();
        this.staticTerritoryData = loadStaticTerritoryData();
    }

    @Override
    protected void init() {
        if (!loadRequested) {
            loadRequested = true;
            loadTerritoriesAsync();
        }
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float delta) {
        ClickGuiTheme theme = NiaClickGuiScreen.configuredTheme();
        layoutTerritories();
        drawMap(g, mouseX, mouseY, theme);
        drawResourceOverlay(g, theme);
        drawLoadoutButton(g, mouseX, mouseY, theme);
        super.render(g, mouseX, mouseY, delta);
        drawDetailPanel(g, mouseX, mouseY, theme);
        drawQuickMenu(g, mouseX, mouseY, theme);
        drawTerritoryHover(g, mouseX, mouseY, theme);
        loadoutManager.render(g, font, mouseX, mouseY, theme);
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent click, boolean doubleClick) {
        if (super.mouseClicked(click, doubleClick)) {
            return true;
        }

        layoutTerritories();

        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.input();

        if (loadoutManager.visible()) {
            if (loadoutManager.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && loadoutManager.selectingTerritories()) {
                TerritoryWidget widget = hoveredTerritory(mouseX, mouseY);
                if (widget != null && widget.owned()) {
                    loadoutManager.toggleTerritory(widget);
                    return true;
                }
            }
            if (loadoutManager.contains(mouseX, mouseY)) {
                return true;
            }
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                    || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT
                    || button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                draggingMap = true;
                return true;
            }
            return false;
        }

        if (loadoutButtonClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (quickMenu.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        DetailData details = selectedDetails();
        if (details != null && detailPanel.mouseClicked(
                mouseX,
                mouseY,
                button,
                width,
                height,
                details.producedResources()
        )) {
            return true;
        }

        for (int i = visibleTerritoryWidgets.size() - 1; i >= 0; i--) {
            TerritoryWidget widget = visibleTerritoryWidgets.get(i);
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && widget.owned() && widget.contains(mouseX, mouseY)) {
                selectTerritory(widget, (int) Math.round(mouseX), (int) Math.round(mouseY));
                return true;
            }
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && selectedTerritory != null) {
            selectedTerritory = null;
            quickMenu.hide();
            return true;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT
                || button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            draggingMap = true;
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(@NotNull MouseButtonEvent event, double deltaX, double deltaY) {
        if (loadoutManager.mouseDragged(event.x(), event.y(), width, height)) {
            return true;
        }
        if (!loadoutManager.visible()) {
            DetailData details = selectedDetails();
            if (detailPanel.mouseDragged(
                    event.x(),
                    event.y(),
                    width,
                    height,
                    details == null ? Resources.EMPTY : details.producedResources()
            )) {
                return true;
            }
        }
        if (draggingMap) {
            panX += deltaX;
            panY += deltaY;
            layoutDirty = true;
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(@NotNull MouseButtonEvent event) {
        if (loadoutManager.mouseReleased()) {
            return true;
        }
        if (!loadoutManager.visible() && detailPanel.mouseReleased()) {
            return true;
        }
        if (draggingMap) {
            draggingMap = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (loadoutManager.visible() && loadoutManager.contains(mouseX, mouseY)) {
            return true;
        }
        DetailData details = selectedDetails();
        if (details != null) {
            if (detailPanel.contains(mouseX, mouseY, width, height, details.producedResources())) {
                detailPanel.mouseScrolled(mouseX, mouseY, verticalAmount, width, height, details.producedResources());
                return true;
            }
        }

        if (verticalAmount == 0.0 || territoryWidgets.isEmpty()) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        UiRect canvas = canvasBounds();
        double oldZoom = zoom;
        double nextZoom = clamp(zoom * Math.pow(ZOOM_STEP, verticalAmount), MIN_ZOOM, MAX_ZOOM);
        if (Math.abs(nextZoom - oldZoom) < 0.0001) {
            return true;
        }

        double ratio = nextZoom / oldZoom;
        double canvasCenterX = canvas.x() + canvas.width() / 2.0;
        double canvasCenterY = canvas.y() + canvas.height() / 2.0;
        panX = mouseX - canvasCenterX - (mouseX - canvasCenterX - panX) * ratio;
        panY = mouseY - canvasCenterY - (mouseY - canvasCenterY - panY) * ratio;
        zoom = nextZoom;
        layoutDirty = true;
        return true;
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent event) {
        if (loadoutManager.visible() && event.key() == GLFW.GLFW_KEY_ESCAPE) {
            loadoutManager.close();
            return true;
        }
        if (selectedTerritory != null && quickMenu.keyPressed(
                event,
                tax -> ecoFeature.setTax(selectedTerritory.territory().name(), tax)
        )) {
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(@NotNull CharacterEvent event) {
        if (loadoutManager.visible()) {
            return true;
        }
        if (quickMenu.charTyped(event)) {
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public void onClose() {
        draggingMap = false;
        detailPanel.stopDragging();
        quickMenu.hide();
        loadoutManager.close();
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void loadTerritoriesAsync() {
        loading = territoryWidgets.isEmpty();
        if (loading) {
            status = "Loading territory data...";
        }

        try {
            WynncraftAPI.territoryResponseAsync().whenComplete((response, throwable) -> {
                Minecraft minecraft = Minecraft.getInstance();
                minecraft.execute(() -> {
                    if (throwable != null) {
                        failLoad(throwable);
                        return;
                    }
                    replaceTerritories(response);
                });
            });
        } catch (Throwable throwable) {
            failLoad(throwable);
        }
    }

    private void failLoad(Throwable throwable) {
        loading = false;
        clearTerritories();
        status = "Failed to load territories";
        NiamodClient.LOGGER.warn("Failed to load eco territory data", throwable);
    }

    private void replaceTerritories(Map<String, TerritoryResponse> response) {
        loading = false;

        String selectedName = selectedTerritory == null ? null : selectedTerritory.territory().name();
        clearTerritories();

        if (response == null || response.isEmpty()) {
            status = "No territory data";
            return;
        }

        List<TerritoryWidget> loadedWidgets = new ArrayList<>();
        for (Map.Entry<String, TerritoryResponse> entry : response.entrySet()) {
            TerritoryWidget widget = toWidget(entry.getKey(), entry.getValue());
            if (widget != null) {
                loadedWidgets.add(widget);
            }
        }
        loadedWidgets.sort(Comparator.comparing(widget -> widget.territory().name().toLowerCase(Locale.ROOT)));

        Map<String, TerritoryWidget> loadedByNormalizedName = new HashMap<>();
        for (TerritoryWidget widget : loadedWidgets) {
            loadedByNormalizedName.put(normalizeName(widget.territory().name()), widget);
        }

        for (TerritoryWidget widget : loadedWidgets) {
            if (widget.owned() || touchesOwnedTerritory(widget, loadedByNormalizedName)) {
                territoryWidgets.add(widget);
            }
        }
        for (TerritoryWidget widget : territoryWidgets) {
            widgetsByName.put(widget.territory().name(), widget);
            widgetsByNormalizedName.put(normalizeName(widget.territory().name()), widget);
        }
        buildTerritoryLinks();
        mapBounds = preferredMapBounds();
        panX = 0.0;
        panY = 0.0;
        zoom = 1.0;
        layoutDirty = true;
        requestCurrentStats();

        if (selectedName != null) {
            TerritoryWidget restored = widgetsByNormalizedName.get(normalizeName(selectedName));
            selectedTerritory = restored != null && restored.owned() ? restored : null;
        }

        long ownedCount = territoryWidgets.stream().filter(TerritoryWidget::owned).count();
        if (ownedCount == 0L) {
            status = "No territories for " + guildName;
        } else {
            status = ownedCount + (ownedCount == 1L ? " territory" : " territories");
        }
    }

    private TerritoryWidget toWidget(String name, TerritoryResponse response) {
        if (name == null || name.isBlank() || response == null) {
            return null;
        }
        if (response.location() == null || response.location().start() == null || response.location().end() == null) {
            return null;
        }

        int[] start = response.location().start();
        int[] end = response.location().end();
        if (start.length < 2 || end.length < 2) {
            return null;
        }

        boolean owned = isOwnedBySelectedGuild(response);
        StaticTerritoryData staticData = staticTerritoryData.getOrDefault(name, EMPTY_STATIC_DATA);
        List<String> connections = staticData.connections().stream()
                .filter(connection -> connection != null && !connection.isBlank())
                .toList();
        TerritoryNode territory = new TerritoryNode(
                name,
                Math.min(start[0], end[0]),
                Math.min(start[1], end[1]),
                Math.max(start[0], end[0]),
                Math.max(start[1], end[1]),
                parseAcquiredMillis(response.acquired()),
                staticData.resources(),
                connections
        );
        return new TerritoryWidget(territory, owned, this::selectTerritory);
    }

    private boolean touchesOwnedTerritory(TerritoryWidget widget, Map<String, TerritoryWidget> allWidgetsByNormalizedName) {
        if (widget == null || widget.owned()) {
            return widget != null;
        }

        for (String connection : widget.territory().connections()) {
            TerritoryWidget target = allWidgetsByNormalizedName.get(normalizeName(connection));
            if (target != null && target.owned()) {
                return true;
            }
        }

        String territoryKey = normalizeName(widget.territory().name());
        for (TerritoryWidget candidate : allWidgetsByNormalizedName.values()) {
            if (candidate.owned() && connectsTo(candidate, territoryKey)) {
                return true;
            }
        }
        return false;
    }

    private boolean connectsTo(TerritoryWidget widget, String normalizedTargetName) {
        if (widget == null || normalizedTargetName == null || normalizedTargetName.isBlank()) {
            return false;
        }
        for (String connection : widget.territory().connections()) {
            if (normalizeName(connection).equals(normalizedTargetName)) {
                return true;
            }
        }
        return false;
    }

    private void clearTerritories() {
        territoryWidgets.clear();
        visibleTerritoryWidgets.clear();
        territoryLinks.clear();
        visibleConnections.clear();
        widgetsByName.clear();
        widgetsByNormalizedName.clear();
        mapBounds = MapBounds.EMPTY;
        selectedTerritory = null;
        quickMenu.hide();
        layoutDirty = true;
    }

    private void buildTerritoryLinks() {
        Set<String> drawnEdges = new HashSet<>();
        for (TerritoryWidget widget : territoryWidgets) {
            for (String connection : widget.territory().connections()) {
                TerritoryWidget target = widgetsByName.get(connection);
                if (target == null) {
                    target = widgetsByNormalizedName.get(normalizeName(connection));
                }
                if (target == null || target == widget) {
                    continue;
                }

                String edgeKey = edgeKey(widget.territory().name(), target.territory().name());
                if (drawnEdges.add(edgeKey)) {
                    territoryLinks.add(new TerritoryLink(widget, target));
                }
            }
        }
    }

    private MapBounds preferredMapBounds() {
        for (TerritoryWidget widget : territoryWidgets) {
            if (widget.owned() && ecoFeature.isHeadquarters(widget.territory().name())) {
                return MapBounds.from(List.of(widget));
            }
        }

        List<TerritoryWidget> ownedWidgets = territoryWidgets.stream()
                .filter(TerritoryWidget::owned)
                .toList();
        return MapBounds.from(ownedWidgets.isEmpty() ? territoryWidgets : ownedWidgets);
    }

    private boolean isOwnedBySelectedGuild(TerritoryResponse response) {
        return response.guild() != null
                && response.guild().name() != null
                && normalizeName(response.guild().name()).equals(normalizeName(guildName));
    }

    private void layoutTerritories() {
        UiRect canvas = canvasBounds();
        if (!layoutDirty
                && width == lastLayoutWidth
                && height == lastLayoutHeight
                && panX == lastLayoutPanX
                && panY == lastLayoutPanY
                && zoom == lastLayoutZoom) {
            return;
        }

        visibleTerritoryWidgets.clear();
        visibleConnections.clear();
        lastLayoutWidth = width;
        lastLayoutHeight = height;
        lastLayoutPanX = panX;
        lastLayoutPanY = panY;
        lastLayoutZoom = zoom;
        layoutDirty = false;

        if (territoryWidgets.isEmpty() || !mapBounds.valid()) {
            return;
        }

        double availableW = Math.max(1.0, canvas.width() - MAP_PADDING * 2.0);
        double availableH = Math.max(1.0, canvas.height() - MAP_PADDING * 2.0);
        double worldW = Math.max(1.0, mapBounds.width());
        double worldH = Math.max(1.0, mapBounds.height());
        double baseScale = Math.min(availableW / worldW, availableH / worldH);
        if (!Double.isFinite(baseScale) || baseScale <= 0) {
            baseScale = 1.0;
        }
        double scale = baseScale * zoom;
        double worldCenterX = (mapBounds.minX() + mapBounds.maxX()) / 2.0;
        double worldCenterZ = (mapBounds.minZ() + mapBounds.maxZ()) / 2.0;
        double screenCenterX = canvas.x() + canvas.width() / 2.0 + panX;
        double screenCenterY = canvas.y() + canvas.height() / 2.0 + panY;

        for (TerritoryWidget widget : territoryWidgets) {
            TerritoryNode territory = widget.territory();
            double centerX = screenCenterX + (territory.centerX() - worldCenterX) * scale;
            double centerY = screenCenterY + (territory.centerZ() - worldCenterZ) * scale;

            int boxW = Math.max(1, (int) Math.round(territory.worldWidth() * scale));
            int boxH = Math.max(1, (int) Math.round(territory.worldHeight() * scale));

            int x = (int) Math.round(centerX - boxW / 2.0);
            int y = (int) Math.round(centerY - boxH / 2.0);
            UiRect bounds = new UiRect(x, y, boxW, boxH);
            widget.setBounds(bounds);
            if (TerritoryWidget.intersects(bounds, canvas, TerritoryWidget.CLIP_PADDING)) {
                visibleTerritoryWidgets.add(widget);
            }
        }

        for (TerritoryLink edge : territoryLinks) {
            TerritoryWidget source = edge.source();
            TerritoryWidget target = edge.target();
            double[] clippedLine = Render2D.clipLine(
                    source.centerX(),
                    source.centerY(),
                    target.centerX(),
                    target.centerY(),
                    canvas,
                    LINE_CLIP_PADDING
            );
            if (clippedLine != null) {
                visibleConnections.add(new Line(clippedLine[0], clippedLine[1], clippedLine[2], clippedLine[3], edge.foreignRoute()));
            }
        }
    }

    private void drawMap(GuiGraphics g, int mouseX, int mouseY, ClickGuiTheme theme) {
        UiRect canvas = canvasBounds();
        if (territoryWidgets.isEmpty()) {
            drawCenteredStatus(g, canvas, theme);
        } else {
            drawConnections(g);
            long now = System.currentTimeMillis();
            for (TerritoryWidget widget : visibleTerritoryWidgets) {
                widget.render(
                        g,
                        font,
                        mouseX,
                        mouseY,
                        theme,
                        now,
                        canvas,
                        widget.owned() && ecoFeature.isHeadquarters(widget.territory().name()),
                        widget == selectedTerritory || loadoutManager.isSelected(widget)
                );
            }
        }
    }

    private void drawCenteredStatus(GuiGraphics g, UiRect canvas, ClickGuiTheme theme) {
        String text = loading ? "Loading territory data..." : status;
        Component component = NiaClickGuiScreen.styled(text);
        int textW = font.width(component);
        int x = canvas.x() + (canvas.width() - textW) / 2;
        int y = canvas.y() + (canvas.height() - font.lineHeight) / 2;
        g.drawString(font, component, x, y, theme.secondaryText(), false);
    }

    private DetailData selectedDetails() {
        return detailDataFor(selectedTerritory);
    }

    private DetailData detailDataFor(TerritoryWidget widget) {
        if (widget == null || !widget.owned()) {
            return null;
        }

        TerritoryNode territory = widget.territory();
        Map<TerritoryUpgrade, Integer> stats = ecoFeature.statsFor(territory.name());
        int ownedConnections = ownedConnectionCount(territory);
        int externalConnections = externalTerritoryCount(territory);
        boolean headquarters = ecoFeature.isHeadquarters(territory.name());

        return new DetailData(
                widget,
                guildName,
                stats,
                resourceStoreFor(territory.name(), stats),
                calculateProducedResourcesPerHour(territory, stats),
                calculateDamage(territory, ownedConnections, externalConnections, headquarters, stats),
                calculateAttackSpeed(stats),
                calculateHealth(territory, ownedConnections, externalConnections, headquarters, stats),
                calculateDefense(stats),
                ownedConnections,
                totalConnectionCount(territory),
                externalConnections,
                ecoFeature.tax(territory.name()),
                headquarters,
                ecoFeature.bordersOpen(territory.name()),
                ecoFeature.territoryRoute(territory.name()).label(),
                ecoFeature.loadoutFor(territory.name())
        );
    }

    private void drawDetailPanel(GuiGraphics g, int mouseX, int mouseY, ClickGuiTheme theme) {
        DetailData details = selectedDetails();
        if (details == null) {
            return;
        }

        TerritoryNode territory = details.selectedTerritory().territory();
        detailPanel.render(
                g,
                font,
                mouseX,
                mouseY,
                theme,
                details,
                width,
                height,
                (upgrade, delta) -> onTerritoryUpgradeAdjusted(territory, upgrade, delta),
                delta -> ecoFeature.setTax(territory.name(), ecoFeature.tax(territory.name()) + delta),
                () -> ecoFeature.setHeadquarters(territory.name()),
                () -> ecoFeature.setBordersOpen(territory.name(), !ecoFeature.bordersOpen(territory.name())),
                () -> ecoFeature.toggleTerritoryRoute(territory.name()),
                () -> {
                    quickMenu.hide();
                    loadoutManager.open();
                },
                () -> selectedTerritory = null
        );
    }

    private void drawQuickMenu(GuiGraphics g, int mouseX, int mouseY, ClickGuiTheme theme) {
        if (selectedTerritory == null || !quickMenu.visible()) {
            return;
        }

        TerritoryNode territory = selectedTerritory.territory();
        Map<TerritoryUpgrade, Integer> stats = ecoFeature.statsFor(territory.name());
        quickMenu.render(
                g,
                font,
                mouseX,
                mouseY,
                theme,
                stats,
                ecoFeature.tax(territory.name()),
                ecoFeature.isHeadquarters(territory.name()),
                ecoFeature.bordersOpen(territory.name()),
                (upgrade, delta) -> onTerritoryUpgradeAdjusted(territory, upgrade, delta),
                tax -> ecoFeature.setTax(territory.name(), tax),
                () -> ecoFeature.setHeadquarters(territory.name()),
                () -> ecoFeature.setBordersOpen(territory.name(), !ecoFeature.bordersOpen(territory.name()))
        );
    }

    private void drawTerritoryHover(GuiGraphics g, int mouseX, int mouseY, ClickGuiTheme theme) {
        if (loadoutManager.visible()) {
            return;
        }
        DetailData selectedData = selectedDetails();
        if (selectedData != null
                && detailPanel.contains(mouseX, mouseY, width, height, selectedData.producedResources())) {
            return;
        }
        if (showLoadoutButton() && loadoutButtonBounds().contains(mouseX, mouseY)) {
            return;
        }
        TerritoryWidget hovered = hoveredTerritory(mouseX, mouseY);
        if (hovered == null || !hovered.owned() || quickMenu.contains(mouseX, mouseY)) {
            return;
        }
        if (hovered == selectedTerritory) {
            return;
        }

        TerritoryNode territory = hovered.territory();
        Map<TerritoryUpgrade, Integer> stats = ecoFeature.statsFor(territory.name());
        Resources produced = calculateProducedResourcesPerHour(territory, stats);
        List<String> lines = List.of(
                territory.name(),
                "Prod E/O/C/F/W: " + formatCompact(produced.emeralds()) + " / " + formatCompact(produced.ore()) + " / " + formatCompact(produced.crops()) + " / " + formatCompact(produced.fish()) + " / " + formatCompact(produced.wood()),
                "Tower D/A/H/F: " + statLevel(stats, TerritoryUpgrade.DAMAGE) + " / " + statLevel(stats, TerritoryUpgrade.ATTACK) + " / " + statLevel(stats, TerritoryUpgrade.HEALTH) + " / " + statLevel(stats, TerritoryUpgrade.DEFENSE),
                "Res ER/RR/EE/EmR: " + statLevel(stats, TerritoryUpgrade.EFFICIENT_RESOURCES) + " / " + statLevel(stats, TerritoryUpgrade.RESOURCE_RATE) + " / " + statLevel(stats, TerritoryUpgrade.EFFICIENT_EMERALDS) + " / " + statLevel(stats, TerritoryUpgrade.EMERALD_RATE)
        );

        int tooltipW = 0;
        for (String line : lines) {
            tooltipW = Math.max(tooltipW, font.width(NiaClickGuiScreen.styled(line)));
        }
        tooltipW += 14;
        int tooltipH = 10 + lines.size() * 12;
        int tooltipX = clampInt(mouseX + 12, 4, Math.max(4, width - tooltipW - 4));
        int tooltipY = clampInt(mouseY + 12, 4, Math.max(4, height - tooltipH - 4));
        Render2D.shaderRoundedSurface(g, tooltipX, tooltipY, tooltipW, tooltipH, 4, theme.background(), Render2D.withAlpha(theme.accentColor(), 90));
        int y = tooltipY + 6;
        for (int i = 0; i < lines.size(); i++) {
            g.drawString(font, NiaClickGuiScreen.styled(lines.get(i)), tooltipX + 7, y, i == 0 ? theme.textColor() : theme.secondaryText(), false);
            y += 12;
        }
    }

    private TerritoryWidget hoveredTerritory(double mouseX, double mouseY) {
        for (int i = visibleTerritoryWidgets.size() - 1; i >= 0; i--) {
            TerritoryWidget widget = visibleTerritoryWidgets.get(i);
            if (widget.contains(mouseX, mouseY)) {
                return widget;
            }
        }
        return null;
    }

    private void drawResourceOverlay(GuiGraphics g, ClickGuiTheme theme) {
        if (territoryWidgets.isEmpty()) {
            return;
        }
        ResourceFlow flowState = summarizeResourceFlow();
        resourceSummaryWidget.render(g, font, theme, flowState);
    }

    private void drawLoadoutButton(GuiGraphics g, int mouseX, int mouseY, ClickGuiTheme theme) {
        if (!showLoadoutButton()) {
            return;
        }

        UiRect button = loadoutButtonBounds();
        boolean hovered = button.contains(mouseX, mouseY);
        int fill = hovered ? Render2D.withAlpha(theme.accentColor(), 54) : Render2D.withAlpha(theme.background(), 210);
        Render2D.shaderRoundedSurface(g, button.x(), button.y(), button.width(), button.height(), 4, fill, Render2D.withAlpha(theme.accentColor(), hovered ? 170 : 95));

        Component text = NiaClickGuiScreen.styled("Loadouts");
        g.drawString(
                font,
                text,
                button.x() + (button.width() - font.width(text)) / 2,
                button.y() + (button.height() - font.lineHeight) / 2 + 1,
                hovered ? theme.textColor() : theme.secondaryText(),
                false
        );
    }

    private boolean loadoutButtonClicked(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !showLoadoutButton() || !loadoutButtonBounds().contains(mouseX, mouseY)) {
            return false;
        }

        quickMenu.hide();
        loadoutManager.open();
        return true;
    }

    private boolean showLoadoutButton() {
        return !loadoutManager.visible()
                && !ecoFeature.loadouts().isEmpty()
                && territoryWidgets.stream().anyMatch(TerritoryWidget::owned);
    }

    private UiRect loadoutButtonBounds() {
        return new UiRect(8, 8 + resourceSummaryWidget.height() + LOADOUT_BUTTON_GAP, LOADOUT_BUTTON_W, LOADOUT_BUTTON_H);
    }

    private void drawConnections(GuiGraphics g) {
        double phase = System.currentTimeMillis() / 70.0;

        for (Line edge : visibleConnections) {
            int color = edge.foreignRoute()
                    ? Render2D.withAlpha(0xFF3B30, 190)
                    : Render2D.withAlpha(TerritoryResourceColors.connectionColor(), 155);
            Render2D.dashedLine(
                    g,
                    edge.x1(),
                    edge.y1(),
                    edge.x2(),
                    edge.y2(),
                    phase,
                    CONNECTION_DASH,
                    CONNECTION_GAP,
                    MAX_CONNECTION_DASHES_PER_EDGE,
                    (distance, length) -> color
            );
        }
    }

    private void selectTerritory(TerritoryWidget widget) {
        selectTerritory(widget, (int) Math.round(widget.centerX()), (int) Math.round(widget.centerY()));
    }

    private void selectTerritory(TerritoryWidget widget, int mouseX, int mouseY) {
        if (widget == null || !widget.owned()) {
            return;
        }
        selectedTerritory = widget;
        detailPanel.resetScroll();
        DetailData details = detailDataFor(widget);
        detailPanel.ensurePosition(width, height, details == null ? Resources.EMPTY : details.producedResources());
        quickMenu.showAt(mouseX, mouseY, width, height);
    }

    private void onTerritoryUpgradeAdjusted(TerritoryNode territory, TerritoryUpgrade upgrade, int delta) {
        ecoFeature.adjustStat(territory.name(), upgrade, delta);
    }

    private void requestCurrentStats() {
        ecoFeature.requestStats(territoryWidgets.stream()
                .filter(TerritoryWidget::owned)
                .map(widget -> widget.territory().name())
                .toList());
    }

    private ResourceFlow summarizeResourceFlow() {
        ResourceAmounts stored = ResourceAmounts.EMPTY;
        ResourceAmounts capacity = ResourceAmounts.EMPTY;
        ResourceAmounts gained = ResourceAmounts.EMPTY;
        ResourceAmounts used = ResourceAmounts.EMPTY;
        for (TerritoryWidget widget : territoryWidgets) {
            if (!widget.owned()) {
                continue;
            }
            TerritoryNode territory = widget.territory();
            Map<TerritoryUpgrade, Integer> stats = ecoFeature.statsFor(territory.name());
            if (ecoFeature.isHeadquarters(territory.name())) {
                TerritoryResourceStore store = resourceStoreFor(territory.name(), stats);
                stored = store.current();
                capacity = store.max();
            }
            gained = gained.plus(ResourceAmounts.fromResources(calculateProducedResourcesPerHour(territory, stats)));
            used = used.plus(calculateResourceCostPerHour(stats));
        }
        double usagePercent = gained.materialTotal() <= 0L ? 0.0 : used.materialTotal() / (double) gained.materialTotal() * 100.0;
        return new ResourceFlow(stored, capacity, gained, used, usagePercent);
    }

    private TerritoryResourceStore resourceStoreFor(String territoryName, Map<TerritoryUpgrade, Integer> stats) {
        double materialStorageBase = ecoFeature.isHeadquarters(territoryName) ? 1500.0 : 300.0;
        long materialStorage = Math.round(materialStorageBase * storageMultiplier(stats, TerritoryUpgrade.RESOURCE_STORAGE));
        long emeraldStorage = Math.round(1500.0 * storageMultiplier(stats, TerritoryUpgrade.EMERALD_STORAGE));
        return new TerritoryResourceStore(
                ResourceAmounts.EMPTY,
                new ResourceAmounts(emeraldStorage, materialStorage, materialStorage, materialStorage, materialStorage)
        );
    }

    private ResourceAmounts calculateResourceCostPerHour(Map<TerritoryUpgrade, Integer> stats) {
        ResourceAmounts total = ResourceAmounts.EMPTY;
        for (TerritoryUpgrade upgrade : TerritoryUpgrade.values()) {
            total = total.plus(ResourceAmounts.of(upgrade.costResource(), upgrade.cost(statLevel(stats, upgrade))));
        }
        return total;
    }

    private DamageRange calculateDamage(TerritoryNode territory, int directConnections, int externalConnections, boolean headquarters, Map<TerritoryUpgrade, Integer> stats) {
        if (territory == null) {
            return DamageRange.EMPTY;
        }

        double multiplier = territoryMultiplier(directConnections, externalConnections, headquarters);
        double damageMultiplier = percentMultiplier(stats, TerritoryUpgrade.DAMAGE);
        return new DamageRange(1_000.0 * damageMultiplier * multiplier, 1_500.0 * damageMultiplier * multiplier);
    }

    private double calculateAttackSpeed(Map<TerritoryUpgrade, Integer> stats) {
        return 0.5 * percentMultiplier(stats, TerritoryUpgrade.ATTACK);
    }

    private double calculateHealth(TerritoryNode territory, int directConnections, int externalConnections, boolean headquarters, Map<TerritoryUpgrade, Integer> stats) {
        if (territory == null) {
            return 0.0;
        }
        return 300_000.0 * percentMultiplier(stats, TerritoryUpgrade.HEALTH) * territoryMultiplier(directConnections, externalConnections, headquarters);
    }

    private double calculateDefense(Map<TerritoryUpgrade, Integer> stats) {
        return 0.1 * percentMultiplier(stats, TerritoryUpgrade.DEFENSE);
    }

    private Resources calculateProducedResourcesPerHour(TerritoryNode territory, Map<TerritoryUpgrade, Integer> stats) {
        if (territory == null) {
            return Resources.EMPTY;
        }

        Resources base = territory.resources();
        return new Resources(
                roundProductionPerHour(base.emeralds(), stats, TerritoryUpgrade.EMERALD_RATE, TerritoryUpgrade.EFFICIENT_EMERALDS),
                roundProductionPerHour(base.ore(), stats, TerritoryUpgrade.RESOURCE_RATE, TerritoryUpgrade.EFFICIENT_RESOURCES),
                roundProductionPerHour(base.crops(), stats, TerritoryUpgrade.RESOURCE_RATE, TerritoryUpgrade.EFFICIENT_RESOURCES),
                roundProductionPerHour(base.fish(), stats, TerritoryUpgrade.RESOURCE_RATE, TerritoryUpgrade.EFFICIENT_RESOURCES),
                roundProductionPerHour(base.wood(), stats, TerritoryUpgrade.RESOURCE_RATE, TerritoryUpgrade.EFFICIENT_RESOURCES)
        );
    }

    private double territoryMultiplier(int directConnections, int externalConnections, boolean headquarters) {
        double connectionMultiplier = 1.0 + (0.3 * directConnections);
        if (!headquarters) {
            return connectionMultiplier;
        }
        return (1.5 + (0.25 * externalConnections)) * connectionMultiplier;
    }

    private double upgradeBonus(Map<TerritoryUpgrade, Integer> stats, TerritoryUpgrade upgrade) {
        return upgrade.bonus(statLevel(stats, upgrade));
    }

    private double storageMultiplier(Map<TerritoryUpgrade, Integer> stats, TerritoryUpgrade upgrade) {
        return 1.0 + (upgradeBonus(stats, upgrade) / 100.0);
    }

    private double percentMultiplier(Map<TerritoryUpgrade, Integer> stats, TerritoryUpgrade upgrade) {
        return 1.0 + (upgradeBonus(stats, upgrade) / 100.0);
    }

    private int statLevel(Map<TerritoryUpgrade, Integer> stats, TerritoryUpgrade upgrade) {
        return stats == null ? 0 : stats.getOrDefault(upgrade, 0);
    }

    private double calculateProductionPerHour(int basePerHour, Map<TerritoryUpgrade, Integer> stats, TerritoryUpgrade rateUpgrade, TerritoryUpgrade efficientUpgrade) {
        if (basePerHour <= 0) {
            return 0.0;
        }

        double basePerProduction = basePerHour / 900.0;
        double secondsPerProduction = Math.max(1.0, upgradeBonus(stats, rateUpgrade));
        double efficientMultiplier = 1.0 + (upgradeBonus(stats, efficientUpgrade) / 100.0);
        return basePerProduction * (3600.0 / secondsPerProduction) * efficientMultiplier;
    }

    private int roundProductionPerHour(int basePerHour, Map<TerritoryUpgrade, Integer> stats, TerritoryUpgrade rateUpgrade, TerritoryUpgrade efficientUpgrade) {
        return (int) Math.round(calculateProductionPerHour(basePerHour, stats, rateUpgrade, efficientUpgrade));
    }

    private int ownedConnectionCount(TerritoryNode territory) {
        int count = 0;
        for (String connection : territory.connections()) {
            TerritoryWidget target = connectedOwnedTerritory(connection);
            if (target != null) {
                count++;
            }
        }
        return count;
    }

    private int externalTerritoryCount(TerritoryNode territory) {
        if (territory == null) {
            return 0;
        }

        Set<String> visited = new HashSet<>();
        ArrayDeque<TerritoryNodeDepth> queue = new ArrayDeque<>();
        visited.add(normalizeName(territory.name()));
        queue.addLast(new TerritoryNodeDepth(territory, 0));

        int count = 0;
        while (!queue.isEmpty()) {
            TerritoryNodeDepth current = queue.removeFirst();
            if (current.depth() >= 3) {
                continue;
            }

            for (String connection : current.territory().connections()) {
                TerritoryWidget target = connectedOwnedTerritory(connection);
                if (target == null) {
                    continue;
                }

                String key = normalizeName(target.territory().name());
                if (visited.add(key)) {
                    count++;
                    queue.addLast(new TerritoryNodeDepth(target.territory(), current.depth() + 1));
                }
            }
        }

        return count;
    }

    private int totalConnectionCount(TerritoryNode territory) {
        return (int) territory.connections().stream()
                .filter(connection -> connection != null && !connection.isBlank())
                .count();
    }

    private TerritoryWidget connectedOwnedTerritory(String connection) {
        TerritoryWidget target = widgetsByName.get(connection);
        if (target == null) {
            target = widgetsByNormalizedName.get(normalizeName(connection));
        }
        return target != null && target.owned() ? target : null;
    }

    private Map<String, StaticTerritoryData> loadStaticTerritoryData() {
        try {
            Map<String, StaticTerritoryData> parsed = GSON.fromJson(FileUtils.readFile("terr_data.json"), STATIC_TERRITORY_DATA_TYPE);
            return parsed == null ? Map.of() : parsed;
        } catch (Exception e) {
            NiamodClient.LOGGER.warn("Failed to load static territory resource data", e);
            return Map.of();
        }
    }

    private long parseAcquiredMillis(String acquired) {
        if (acquired == null || acquired.isBlank()) {
            return -1L;
        }

        try {
            return Instant.parse(acquired).toEpochMilli();
        } catch (DateTimeParseException ignored) {
        }

        try {
            return OffsetDateTime.parse(acquired).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored) {
            return -1L;
        }
    }

    private String configuredGuildName() {
        String configured = NyahConfig.getData().getGuildName();
        return configured == null || configured.isBlank() ? "Guild" : configured.trim();
    }

    private String edgeKey(String a, String b) {
        String left = normalizeName(a);
        String right = normalizeName(b);
        return left.compareTo(right) <= 0 ? left + '\0' + right : right + '\0' + left;
    }

    private String normalizeName(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private int clampInt(int value, int min, int max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private String formatCompact(long value) {
        long abs = Math.abs(value);
        if (abs >= 1_000_000L) {
            return String.format(Locale.ROOT, "%.1fm", value / 1_000_000.0);
        }
        if (abs >= 1_000L) {
            return String.format(Locale.ROOT, "%.1fk", value / 1_000.0);
        }
        return Long.toString(value);
    }

    private UiRect canvasBounds() {
        return new UiRect(0, 0, Math.max(1, width), Math.max(1, height));
    }

    private record LoadoutButton(UiRect bounds, Runnable onClick) {
    }

    private record StaticTerritoryData(Resources resources, List<String> connections) {
        public StaticTerritoryData {
            resources = resources == null ? Resources.EMPTY : resources;
            connections = connections == null ? List.of() : connections;
        }
    }

    private record MapBounds(double minX, double minZ, double maxX, double maxZ, boolean valid) {
        private static final MapBounds EMPTY = new MapBounds(0.0, 0.0, 0.0, 0.0, false);
        private static final double MIN_SPAN = 900.0;

        private static MapBounds from(List<TerritoryWidget> widgets) {
            if (widgets.isEmpty()) {
                return EMPTY;
            }

            double minX = Double.MAX_VALUE;
            double minZ = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE;
            double maxZ = -Double.MAX_VALUE;
            for (TerritoryWidget widget : widgets) {
                TerritoryNode territory = widget.territory();
                minX = Math.min(minX, territory.minX());
                minZ = Math.min(minZ, territory.minZ());
                maxX = Math.max(maxX, territory.maxX());
                maxZ = Math.max(maxZ, territory.maxZ());
            }

            if (maxX - minX < MIN_SPAN) {
                double center = (minX + maxX) / 2.0;
                minX = center - MIN_SPAN / 2.0;
                maxX = center + MIN_SPAN / 2.0;
            }
            if (maxZ - minZ < MIN_SPAN) {
                double center = (minZ + maxZ) / 2.0;
                minZ = center - MIN_SPAN / 2.0;
                maxZ = center + MIN_SPAN / 2.0;
            }
            return new MapBounds(minX, minZ, maxX, maxZ, true);
        }

        private double width() {
            return maxX - minX;
        }

        private double height() {
            return maxZ - minZ;
        }
    }

    private record TerritoryLink(TerritoryWidget source, TerritoryWidget target) {
        private boolean foreignRoute() {
            return !source.owned() || !target.owned();
        }
    }

    private record Line(double x1, double y1, double x2, double y2, boolean foreignRoute) {
    }

    private record TerritoryNodeDepth(TerritoryNode territory, int depth) {
    }

    private class LoadoutManager {
        private static final int PANEL_W = 230;
        private static final int PAD = 8;
        private static final int ROW_H = 20;

        private final List<LoadoutButton> buttons = new ArrayList<>();
        private final Set<String> selectedTerritories = new HashSet<>();
        private TerritoryLoadout selectedLoadout;
        private boolean visible;
        private boolean positioned;
        private boolean dragging;
        private int x;
        private int y;
        private double dragOffsetX;
        private double dragOffsetY;

        private void open() {
            visible = true;
            selectedLoadout = null;
            selectedTerritories.clear();
            selectedTerritory = null;
        }

        private void close() {
            visible = false;
            dragging = false;
            selectedLoadout = null;
            selectedTerritories.clear();
            buttons.clear();
        }

        private boolean visible() {
            return visible;
        }

        private boolean contains(double mouseX, double mouseY) {
            return visible && panelBounds().contains(mouseX, mouseY);
        }

        private boolean selectingTerritories() {
            return visible && selectedLoadout != null;
        }

        private boolean isSelected(TerritoryWidget widget) {
            return selectingTerritories() && widget != null && containsTerritory(widget.territory().name());
        }

        private void toggleTerritory(TerritoryWidget widget) {
            if (!selectingTerritories() || widget == null || !widget.owned()) {
                return;
            }

            String existing = selectedTerritoryName(widget.territory().name());
            if (existing == null) {
                selectedTerritories.add(widget.territory().name());
            } else {
                selectedTerritories.remove(existing);
            }
        }

        private boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!visible || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                return false;
            }

            for (LoadoutButton loadoutButton : buttons) {
                if (loadoutButton.bounds().contains(mouseX, mouseY)) {
                    loadoutButton.onClick().run();
                    return true;
                }
            }

            UiRect panel = panelBounds();
            if (headerBounds(panel).contains(mouseX, mouseY)) {
                dragging = true;
                dragOffsetX = mouseX - panel.x();
                dragOffsetY = mouseY - panel.y();
                return true;
            }
            return panel.contains(mouseX, mouseY);
        }

        private boolean mouseDragged(double mouseX, double mouseY, int screenWidth, int screenHeight) {
            if (!dragging) {
                return false;
            }

            int panelH = panelHeight();
            x = clampInt((int) Math.round(mouseX - dragOffsetX), 8, Math.max(8, screenWidth - PANEL_W - 8));
            y = clampInt((int) Math.round(mouseY - dragOffsetY), 8, Math.max(8, screenHeight - panelH - 8));
            return true;
        }

        private boolean mouseReleased() {
            if (!dragging) {
                return false;
            }
            dragging = false;
            return true;
        }

        private void render(GuiGraphics g, net.minecraft.client.gui.Font font, int mouseX, int mouseY, ClickGuiTheme theme) {
            buttons.clear();
            if (!visible) {
                return;
            }

            UiRect panel = panelBounds();
            Render2D.shaderRoundedSurface(g, panel.x(), panel.y(), panel.width(), panel.height(), 4, theme.background(), Render2D.withAlpha(theme.accentColor(), 96));
            g.fill(panel.x() + 1, panel.y() + 1, panel.right() - 1, panel.y() + 24, Render2D.withAlpha(theme.accentColor(), 30));

            int x = panel.x() + PAD;
            int y = panel.y() + 8;
            int right = panel.right() - PAD;
            drawText(g, font, selectedLoadout == null ? "Loadouts" : selectedLoadout.name(), x, y, right - x, theme.textColor());
            y += 24;

            if (selectedLoadout == null) {
                for (TerritoryLoadout loadout : ecoFeature.loadouts()) {
                    UiRect row = new UiRect(x, y, right - x, ROW_H);
                    drawButton(g, font, row, loadout.name(), row.contains(mouseX, mouseY), theme);
                    buttons.add(new LoadoutButton(row, () -> {
                        selectedLoadout = loadout;
                        selectedTerritories.clear();
                    }));
                    y += ROW_H + 4;
                }
                return;
            }

            drawText(g, font, "Selected: " + selectedTerritories.size(), x, y, right - x, theme.secondaryText());
            y += 24;

            int gap = 6;
            int buttonW = (right - x - gap) / 2;
            UiRect apply = new UiRect(x, y, buttonW, ROW_H);
            UiRect cancel = new UiRect(x + buttonW + gap, y, right - x - buttonW - gap, ROW_H);
            drawButton(g, font, apply, "Apply", apply.contains(mouseX, mouseY), theme);
            drawButton(g, font, cancel, "Cancel", cancel.contains(mouseX, mouseY), theme);
            buttons.add(new LoadoutButton(apply, () -> {
                if (!selectedTerritories.isEmpty()) {
                    ecoFeature.applyLoadout(selectedLoadout.name(), selectedTerritories);
                }
                close();
            }));
            buttons.add(new LoadoutButton(cancel, this::close));
        }

        private UiRect panelBounds() {
            int panelH = panelHeight();
            ensurePosition(panelH);
            return new UiRect(x, y, PANEL_W, panelH);
        }

        private int panelHeight() {
            int rows = selectedLoadout == null ? ecoFeature.loadouts().size() : 2;
            int preferred = 40 + rows * (ROW_H + 4) + PAD;
            return Math.min(preferred, Math.max(1, height - 16));
        }

        private void ensurePosition(int panelH) {
            if (!positioned) {
                x = clampInt((width - PANEL_W) / 2, 8, Math.max(8, width - PANEL_W - 8));
                y = clampInt((height - panelH) / 2, 8, Math.max(8, height - panelH - 8));
                positioned = true;
                return;
            }

            x = clampInt(x, 8, Math.max(8, width - PANEL_W - 8));
            y = clampInt(y, 8, Math.max(8, height - panelH - 8));
        }

        private UiRect headerBounds(UiRect panel) {
            return new UiRect(panel.x(), panel.y(), panel.width(), 24);
        }

        private void drawButton(GuiGraphics g, net.minecraft.client.gui.Font font, UiRect rect, String label, boolean hovered, ClickGuiTheme theme) {
            int fill = hovered ? Render2D.withAlpha(theme.accentColor(), 48) : Render2D.withAlpha(theme.secondary(), 180);
            g.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), fill);
            Render2D.outline(g, rect, Render2D.withAlpha(theme.accentColor(), hovered ? 160 : 80));
            drawText(g, font, label, rect.x() + 5, rect.y() + 6, rect.width() - 10, hovered ? theme.textColor() : theme.secondaryText());
        }

        private void drawText(GuiGraphics g, net.minecraft.client.gui.Font font, String text, int x, int y, int maxWidth, int color) {
            g.drawString(font, NiaClickGuiScreen.styled(fit(font, text, maxWidth)), x, y, color, false);
        }

        private String fit(net.minecraft.client.gui.Font font, String text, int maxWidth) {
            if (text == null || maxWidth <= 0) {
                return "";
            }
            if (font.width(NiaClickGuiScreen.styled(text)) <= maxWidth) {
                return text;
            }

            String trimmed = text;
            while (!trimmed.isEmpty() && font.width(NiaClickGuiScreen.styled(trimmed + "...")) > maxWidth) {
                trimmed = trimmed.substring(0, trimmed.length() - 1);
            }
            return trimmed.isEmpty() ? "" : trimmed + "...";
        }

        private boolean containsTerritory(String territoryName) {
            return selectedTerritoryName(territoryName) != null;
        }

        private String selectedTerritoryName(String territoryName) {
            String key = normalizeName(territoryName);
            for (String selected : selectedTerritories) {
                if (normalizeName(selected).equals(key)) {
                    return selected;
                }
            }
            return null;
        }
    }
}
