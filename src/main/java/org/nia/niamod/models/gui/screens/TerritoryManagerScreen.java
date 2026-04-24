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
import org.nia.niamod.features.TerritoryManagerFeature;
import org.nia.niamod.features.eco.GuiActions;
import org.nia.niamod.managers.FeatureManager;
import org.nia.niamod.models.api.TerritoryResponse;
import org.nia.niamod.models.eco.ResourceFlow;
import org.nia.niamod.models.eco.Resources;
import org.nia.niamod.models.eco.StatsCalculator;
import org.nia.niamod.models.eco.TerritoryDetails;
import org.nia.niamod.models.eco.TerritoryLoadout;
import org.nia.niamod.models.eco.TerritoryNode;
import org.nia.niamod.models.eco.TerritoryResourceColors;
import org.nia.niamod.models.eco.TerritoryUpgrade;
import org.nia.niamod.models.gui.component.TerritoryDetailPanel;
import org.nia.niamod.models.gui.component.TerritoryQuickMenu;
import org.nia.niamod.models.gui.component.TerritoryResourceSummaryWidget;
import org.nia.niamod.models.gui.component.TerritoryWidget;
import org.nia.niamod.models.gui.render.UiRect;
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

public class TerritoryManagerScreen extends Screen {
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
    private static final double MAP_CLICK_DRAG_SLOP = 3.0;
    private static final double MAP_CLICK_DRAG_SLOP_SQUARED = MAP_CLICK_DRAG_SLOP * MAP_CLICK_DRAG_SLOP;
    private static final StaticTerritoryData EMPTY_STATIC_DATA = new StaticTerritoryData(Resources.EMPTY, List.of());

    private final Screen parent;
    private final TerritoryManagerFeature feature;
    private final StatsCalculator statsCalculator;
    private final Map<String, StaticTerritoryData> staticTerritoryData;
    private final List<TerritoryWidget> territoryWidgets = new ArrayList<>();
    private final List<TerritoryWidget> visibleTerritoryWidgets = new ArrayList<>();
    private final List<TerritoryLink> territoryLinks = new ArrayList<>();
    private final List<Line> visibleConnections = new ArrayList<>();
    private final Set<String> territoriesConnectedToHeadquarters = new HashSet<>();
    private final Map<String, TerritoryWidget> widgetsByName = new HashMap<>();
    private final Map<String, TerritoryWidget> widgetsByNormalizedName = new HashMap<>();
    private final TerritoryDetailPanel detailPanel = new TerritoryDetailPanel();
    private final TerritoryQuickMenu quickMenu = new TerritoryQuickMenu();
    private final TerritoryResourceSummaryWidget resourceSummaryWidget = new TerritoryResourceSummaryWidget();
    private final LoadoutManager loadoutManager = new LoadoutManager();
    private final String guildName;
    private boolean loadRequested;
    private boolean loading = true;
    private boolean territoriesLoaded;
    private boolean territoryRefreshInFlight;
    private boolean draggingMap;
    private boolean closeSelectionOnMapClick;
    private boolean mapDragExceededClickSlop;
    private boolean layoutDirty = true;
    private long lastTerritoryRefreshMillis;
    private int lastLayoutWidth = -1;
    private int lastLayoutHeight = -1;
    private double lastLayoutPanX = Double.NaN;
    private double lastLayoutPanY = Double.NaN;
    private double lastLayoutZoom = Double.NaN;
    private double panX;
    private double panY;
    private double zoom = 1.0;
    private double mapDragStartX;
    private double mapDragStartY;
    private String status = "Loading territory data...";
    private MapBounds mapBounds = MapBounds.EMPTY;
    private TerritoryWidget selectedTerritory;
    private String headquartersConnectivitySignature = "";

    public TerritoryManagerScreen(Screen parent) {
        this(parent, FeatureManager.getTerritoryManagerFeature());
    }

    public TerritoryManagerScreen(Screen parent, TerritoryManagerFeature feature) {
        super(Component.literal("Territory Manager"));
        this.parent = parent;
        this.feature = feature == null ? new TerritoryManagerFeature() : feature;
        this.statsCalculator = new StatsCalculator(this.feature, this::connectedOwnedTerritoryNode);
        this.guildName = configuredGuildName();
        this.staticTerritoryData = loadStaticTerritoryData();
    }

    @Override
    protected void init() {
        if (!loadRequested) {
            loadRequested = true;
            loadTerritoriesAsync(true);
        }
    }

    @Override
    public void tick() {
        super.tick();
        refreshOwnedTerritoriesIfDue();
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
                startMapDrag(mouseX, mouseY, false);
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
        TerritoryDetails details = selectedDetails();
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
            startMapDrag(mouseX, mouseY, true);
            return true;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT
                || button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            startMapDrag(mouseX, mouseY, false);
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
            TerritoryDetails details = selectedDetails();
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
            updateMapDragDistance(event.x(), event.y());
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
            updateMapDragDistance(event.x(), event.y());
            if (closeSelectionOnMapClick && !mapDragExceededClickSlop) {
                selectedTerritory = null;
                quickMenu.hide();
            }
            draggingMap = false;
            closeSelectionOnMapClick = false;
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (loadoutManager.visible() && loadoutManager.contains(mouseX, mouseY)) {
            return true;
        }
        TerritoryDetails details = selectedDetails();
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
                feature.actionsFor(selectedTerritory.territory().name())::setTax,
                percent -> feature.setGlobalTax(ownedTerritoryNames(), percent)
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
        closeSelectionOnMapClick = false;
        detailPanel.stopDragging();
        quickMenu.hide();
        loadoutManager.close();
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void startMapDrag(double mouseX, double mouseY, boolean closeSelectionOnClick) {
        draggingMap = true;
        closeSelectionOnMapClick = closeSelectionOnClick;
        mapDragExceededClickSlop = false;
        mapDragStartX = mouseX;
        mapDragStartY = mouseY;
    }

    private void updateMapDragDistance(double mouseX, double mouseY) {
        if (mapDragExceededClickSlop) {
            return;
        }

        double offsetX = mouseX - mapDragStartX;
        double offsetY = mouseY - mapDragStartY;
        mapDragExceededClickSlop = offsetX * offsetX + offsetY * offsetY > MAP_CLICK_DRAG_SLOP_SQUARED;
    }

    private void refreshOwnedTerritoriesIfDue() {
        if (!loadRequested || territoryRefreshInFlight) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastTerritoryRefreshMillis >= territoryRefreshIntervalMillis()) {
            loadTerritoriesAsync(!territoriesLoaded);
        }
    }

    private long territoryRefreshIntervalMillis() {
        return Math.max(1, NyahConfig.getData().getEcoTerritoryRefreshSeconds()) * 1000L;
    }

    private void loadTerritoriesAsync(boolean initialLoad) {
        if (territoryRefreshInFlight) {
            return;
        }

        territoryRefreshInFlight = true;
        lastTerritoryRefreshMillis = System.currentTimeMillis();
        loading = initialLoad && territoryWidgets.isEmpty();
        if (loading) {
            status = "Loading territory data...";
        }

        try {
            WynncraftAPI.territoryResponseAsync().whenComplete((response, throwable) -> {
                Minecraft minecraft = Minecraft.getInstance();
                minecraft.execute(() -> {
                    territoryRefreshInFlight = false;
                    if (throwable != null) {
                        failLoad(throwable, initialLoad);
                        return;
                    }
                    replaceTerritories(response, initialLoad);
                });
            });
        } catch (Throwable throwable) {
            territoryRefreshInFlight = false;
            failLoad(throwable, initialLoad);
        }
    }

    private void failLoad(Throwable throwable, boolean clearExisting) {
        loading = false;
        if (clearExisting) {
            clearTerritories();
            status = "Failed to load territories";
        } else {
            status = "Failed to refresh territories";
        }
        NiamodClient.LOGGER.warn("Failed to load eco territory data", throwable);
    }

    private void replaceTerritories(Map<String, TerritoryResponse> response, boolean resetViewport) {
        loading = false;
        territoriesLoaded = true;

        String selectedName = selectedTerritory == null ? null : selectedTerritory.territory().name();
        clearTerritories(false);

        if (response == null) {
            clearHeadquartersConnectivityCache();
            status = "No territory data";
            selectedTerritory = null;
            quickMenu.hide();
            return;
        }
        if (response.isEmpty()) {
            clearHeadquartersConnectivityCache();
            status = "No territories for " + guildName;
            selectedTerritory = null;
            quickMenu.hide();
            return;
        }

        List<TerritoryWidget> allWidgets = new ArrayList<>();
        Map<String, TerritoryWidget> allWidgetsByName = new HashMap<>();
        Map<String, TerritoryWidget> allWidgetsByNormalizedName = new HashMap<>();
        for (Map.Entry<String, TerritoryResponse> entry : response.entrySet()) {
            TerritoryWidget widget = toWidget(entry.getKey(), entry.getValue());
            if (widget != null) {
                allWidgets.add(widget);
                indexWidget(widget, allWidgetsByName, allWidgetsByNormalizedName);
            }
        }

        Set<String> visibleTerritoryKeys = visibleTerritoryKeys(allWidgets, allWidgetsByName, allWidgetsByNormalizedName);
        List<TerritoryWidget> loadedWidgets = allWidgets.stream()
                .filter(widget -> visibleTerritoryKeys.contains(normalizeName(widget.territory().name())))
                .sorted(Comparator.comparing(widget -> widget.territory().name().toLowerCase(Locale.ROOT)))
                .toList();

        for (TerritoryWidget widget : loadedWidgets) {
            territoryWidgets.add(widget);
        }
        for (TerritoryWidget widget : territoryWidgets) {
            indexWidget(widget, widgetsByName, widgetsByNormalizedName);
        }
        buildTerritoryLinks();
        recomputeHeadquartersConnectivityIfNeeded();
        mapBounds = preferredMapBounds();
        if (resetViewport) {
            panX = 0.0;
            panY = 0.0;
            zoom = 1.0;
        }
        layoutDirty = true;
        requestCurrentStats();

        if (selectedName != null) {
            TerritoryWidget restored = widgetsByNormalizedName.get(normalizeName(selectedName));
            selectedTerritory = restored != null && restored.owned() ? restored : null;
            if (selectedTerritory == null) {
                quickMenu.hide();
            }
        } else {
            quickMenu.hide();
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

    private Set<String> visibleTerritoryKeys(
            List<TerritoryWidget> allWidgets,
            Map<String, TerritoryWidget> allWidgetsByName,
            Map<String, TerritoryWidget> allWidgetsByNormalizedName
    ) {
        Set<String> visibleKeys = new HashSet<>();
        for (TerritoryWidget widget : allWidgets) {
            if (!widget.owned()) {
                continue;
            }

            visibleKeys.add(normalizeName(widget.territory().name()));
            for (String connection : widget.territory().connections()) {
                TerritoryWidget target = findWidget(allWidgetsByName, allWidgetsByNormalizedName, connection);
                if (target != null && !target.owned()) {
                    visibleKeys.add(normalizeName(target.territory().name()));
                }
            }
        }
        return visibleKeys;
    }

    private void indexWidget(TerritoryWidget widget, Map<String, TerritoryWidget> byName, Map<String, TerritoryWidget> byNormalizedName) {
        byName.put(widget.territory().name(), widget);
        byNormalizedName.put(normalizeName(widget.territory().name()), widget);
    }

    private void clearTerritories() {
        clearTerritories(true);
    }

    private void clearTerritories(boolean clearSelection) {
        territoryWidgets.clear();
        visibleTerritoryWidgets.clear();
        territoryLinks.clear();
        visibleConnections.clear();
        widgetsByName.clear();
        widgetsByNormalizedName.clear();
        mapBounds = MapBounds.EMPTY;
        if (clearSelection) {
            selectedTerritory = null;
            quickMenu.hide();
            clearHeadquartersConnectivityCache();
        }
        layoutDirty = true;
    }

    private void buildTerritoryLinks() {
        Set<String> drawnEdges = new HashSet<>();
        for (TerritoryWidget widget : territoryWidgets) {
            for (String connection : widget.territory().connections()) {
                TerritoryWidget target = findWidget(widgetsByName, widgetsByNormalizedName, connection);
                if (target == null || target == widget) {
                    continue;
                }
                if (!widget.owned() && !target.owned()) {
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
            if (widget.owned() && feature.isHeadquarters(widget.territory().name())) {
                return MapBounds.from(withDirectForeignNeighbors(List.of(widget)));
            }
        }

        List<TerritoryWidget> ownedWidgets = territoryWidgets.stream()
                .filter(TerritoryWidget::owned)
                .toList();
        return MapBounds.from(ownedWidgets.isEmpty() ? territoryWidgets : withDirectForeignNeighbors(ownedWidgets));
    }

    private List<TerritoryWidget> withDirectForeignNeighbors(List<TerritoryWidget> baseWidgets) {
        if (baseWidgets.isEmpty()) {
            return baseWidgets;
        }

        List<TerritoryWidget> result = new ArrayList<>(baseWidgets);
        Set<String> resultKeys = new HashSet<>();
        for (TerritoryWidget widget : baseWidgets) {
            resultKeys.add(normalizeName(widget.territory().name()));
        }

        for (TerritoryWidget widget : baseWidgets) {
            for (String connection : widget.territory().connections()) {
                TerritoryWidget target = findWidget(widgetsByName, widgetsByNormalizedName, connection);
                if (target != null && !target.owned() && resultKeys.add(normalizeName(target.territory().name()))) {
                    result.add(target);
                }
            }
        }
        return result;
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
                        widget.owned() && feature.isHeadquarters(widget.territory().name()),
                        widget == selectedTerritory || loadoutManager.isSelected(widget),
                        disconnectedFromHeadquarters(widget)
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

    private TerritoryDetails selectedDetails() {
        return detailDataFor(selectedTerritory);
    }

    private TerritoryDetails detailDataFor(TerritoryWidget widget) {
        if (widget == null || !widget.owned()) {
            return null;
        }

        return statsCalculator.details(widget.territory(), guildName);
    }

    private void drawDetailPanel(GuiGraphics g, int mouseX, int mouseY, ClickGuiTheme theme) {
        TerritoryDetails details = selectedDetails();
        if (details == null) {
            return;
        }

        TerritoryNode territory = details.territory();
        GuiActions actions = feature.actionsFor(territory.name());
        detailPanel.render(
                g,
                font,
                mouseX,
                mouseY,
                theme,
                details,
                width,
                height,
                new TerritoryDetailPanel.Actions(
                        actions::adjustUpgrade,
                        actions::adjustTax,
                        percent -> feature.setGlobalTax(ownedTerritoryNames(), percent),
                        () -> setHeadquarters(territory.name()),
                        actions::toggleBorders,
                        () -> feature.setGlobalBordersOpen(ownedTerritoryNames(), !feature.bordersOpen(territory.name())),
                        actions::toggleRoute,
                        () -> feature.setGlobalTerritoryRoute(ownedTerritoryNames(), feature.territoryRoute(territory.name()).toggled()),
                        () -> {
                            quickMenu.hide();
                            loadoutManager.open();
                        },
                        () -> selectedTerritory = null
                )
        );
    }

    private void drawQuickMenu(GuiGraphics g, int mouseX, int mouseY, ClickGuiTheme theme) {
        if (selectedTerritory == null || !quickMenu.visible()) {
            return;
        }

        TerritoryNode territory = selectedTerritory.territory();
        Map<TerritoryUpgrade, Integer> stats = feature.statsFor(territory.name());
        GuiActions actions = feature.actionsFor(territory.name());
        quickMenu.render(
                g,
                font,
                mouseX,
                mouseY,
                theme,
                stats,
                feature.tax(territory.name()),
                feature.isHeadquarters(territory.name()),
                feature.bordersOpen(territory.name()),
                feature.territoryRoute(territory.name()).label(),
                actions::adjustUpgrade,
                () -> setHeadquarters(territory.name()),
                actions::toggleBorders,
                () -> feature.setGlobalBordersOpen(ownedTerritoryNames(), !feature.bordersOpen(territory.name())),
                actions::toggleRoute,
                () -> feature.setGlobalTerritoryRoute(ownedTerritoryNames(), feature.territoryRoute(territory.name()).toggled())
        );
    }

    private void drawTerritoryHover(GuiGraphics g, int mouseX, int mouseY, ClickGuiTheme theme) {
        if (loadoutManager.visible()) {
            return;
        }
        TerritoryDetails selectedData = selectedDetails();
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
        TerritoryDetails details = statsCalculator.details(territory, guildName);
        Map<TerritoryUpgrade, Integer> stats = details.upgradeLevels();
        Resources produced = details.producedResources();
        List<String> lines = List.of(
                territory.name(),
                "Prod E/O/C/F/W: " + formatCompact(produced.emeralds()) + " / " + formatCompact(produced.ore()) + " / " + formatCompact(produced.crops()) + " / " + formatCompact(produced.fish()) + " / " + formatCompact(produced.wood()),
                "Tower D/A/H/F: " + StatsCalculator.statLevel(stats, TerritoryUpgrade.DAMAGE) + " / " + StatsCalculator.statLevel(stats, TerritoryUpgrade.ATTACK) + " / " + StatsCalculator.statLevel(stats, TerritoryUpgrade.HEALTH) + " / " + StatsCalculator.statLevel(stats, TerritoryUpgrade.DEFENSE),
                "Res ER/RR/EE/EmR: " + StatsCalculator.statLevel(stats, TerritoryUpgrade.EFFICIENT_RESOURCES) + " / " + StatsCalculator.statLevel(stats, TerritoryUpgrade.RESOURCE_RATE) + " / " + StatsCalculator.statLevel(stats, TerritoryUpgrade.EFFICIENT_EMERALDS) + " / " + StatsCalculator.statLevel(stats, TerritoryUpgrade.EMERALD_RATE)
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
                && !feature.loadouts().isEmpty()
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
        TerritoryDetails details = detailDataFor(widget);
        detailPanel.ensurePosition(width, height, details == null ? Resources.EMPTY : details.producedResources());
        quickMenu.showAt(mouseX, mouseY, width, height);
    }

    private void requestCurrentStats() {
        feature.requestStats(territoryWidgets.stream()
                .filter(TerritoryWidget::owned)
                .map(widget -> widget.territory().name())
                .toList());
    }

    private void setHeadquarters(String territoryName) {
        feature.setHeadquarters(territoryName);
        recomputeHeadquartersConnectivityIfNeeded();
    }

    private List<String> ownedTerritoryNames() {
        return territoryWidgets.stream()
                .filter(TerritoryWidget::owned)
                .map(widget -> widget.territory().name())
                .toList();
    }

    private void recomputeHeadquartersConnectivityIfNeeded() {
        String signature = headquartersConnectivitySignature();
        if (signature.equals(headquartersConnectivitySignature)) {
            return;
        }

        headquartersConnectivitySignature = signature;
        recomputeHeadquartersConnectivity();
    }

    private String headquartersConnectivitySignature() {
        String headquarters = territoryWidgets.stream()
                .filter(TerritoryWidget::owned)
                .filter(widget -> feature.isHeadquarters(widget.territory().name()))
                .map(widget -> normalizeName(widget.territory().name()))
                .findFirst()
                .orElse("");
        String ownedTerritories = String.join("\0", territoryWidgets.stream()
                .filter(TerritoryWidget::owned)
                .map(widget -> normalizeName(widget.territory().name()))
                .sorted()
                .toList());
        return headquarters + '\u0001' + ownedTerritories;
    }

    private void clearHeadquartersConnectivityCache() {
        territoriesConnectedToHeadquarters.clear();
        headquartersConnectivitySignature = "";
    }

    private void recomputeHeadquartersConnectivity() {
        territoriesConnectedToHeadquarters.clear();

        TerritoryWidget headquarters = territoryWidgets.stream()
                .filter(TerritoryWidget::owned)
                .filter(widget -> feature.isHeadquarters(widget.territory().name()))
                .findFirst()
                .orElse(null);
        if (headquarters == null) {
            return;
        }

        ArrayDeque<TerritoryWidget> queue = new ArrayDeque<>();
        queue.add(headquarters);
        territoriesConnectedToHeadquarters.add(normalizeName(headquarters.territory().name()));

        while (!queue.isEmpty()) {
            TerritoryWidget current = queue.removeFirst();
            for (TerritoryLink link : territoryLinks) {
                if (!link.source().owned() || !link.target().owned()) {
                    continue;
                }

                TerritoryWidget next = null;
                if (link.source() == current) {
                    next = link.target();
                } else if (link.target() == current) {
                    next = link.source();
                }
                if (next == null) {
                    continue;
                }

                String key = normalizeName(next.territory().name());
                if (territoriesConnectedToHeadquarters.add(key)) {
                    queue.addLast(next);
                }
            }
        }
    }

    private boolean disconnectedFromHeadquarters(TerritoryWidget widget) {
        return widget != null
                && widget.owned()
                && !territoriesConnectedToHeadquarters.isEmpty()
                && !territoriesConnectedToHeadquarters.contains(normalizeName(widget.territory().name()));
    }

    private ResourceFlow summarizeResourceFlow() {
        return statsCalculator.summarizeResourceFlow(territoryWidgets.stream()
                .filter(TerritoryWidget::owned)
                .map(TerritoryWidget::territory)
                .toList());
    }

    private TerritoryWidget connectedOwnedTerritory(String connection) {
        TerritoryWidget target = findWidget(widgetsByName, widgetsByNormalizedName, connection);
        return target != null && target.owned() ? target : null;
    }

    private TerritoryNode connectedOwnedTerritoryNode(String connection) {
        TerritoryWidget target = connectedOwnedTerritory(connection);
        return target == null ? null : target.territory();
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

    private TerritoryWidget findWidget(Map<String, TerritoryWidget> byName, Map<String, TerritoryWidget> byNormalizedName, String territoryName) {
        TerritoryWidget target = byName.get(territoryName);
        if (target == null) {
            target = byNormalizedName.get(normalizeName(territoryName));
        }
        return target;
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
                for (TerritoryLoadout loadout : feature.loadouts()) {
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
                    feature.applyLoadout(selectedLoadout.name(), selectedTerritories);
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
            int rows = selectedLoadout == null ? feature.loadouts().size() : 2;
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
