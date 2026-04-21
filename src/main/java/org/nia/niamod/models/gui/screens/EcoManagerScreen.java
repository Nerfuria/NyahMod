package org.nia.niamod.models.gui.screens;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
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
import org.nia.niamod.models.gui.component.ConnectionEdge;
import org.nia.niamod.models.gui.component.EcoMenu;
import org.nia.niamod.models.gui.component.TerritoryWidget;
import org.nia.niamod.models.gui.component.Connection;
import org.nia.niamod.models.gui.render.UiRect;
import org.nia.niamod.models.gui.territory.Resources;
import org.nia.niamod.models.gui.territory.StaticTerritoryData;
import org.nia.niamod.models.gui.territory.TerritoryDefenseState;
import org.nia.niamod.models.gui.territory.TerritoryNode;
import org.nia.niamod.models.gui.territory.TerritoryResourceColors;
import org.nia.niamod.models.gui.territory.TowerControls;
import org.nia.niamod.models.gui.territory.WorldBounds;
import org.nia.niamod.models.gui.theme.ClickGuiTheme;
import org.nia.niamod.render.Render2D;
import org.nia.niamod.util.FileUtils;
import org.nia.niamod.util.WynncraftAPI;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
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
    private static final double MIN_ZOOM = 0.35;
    private static final double MAX_ZOOM = 6.0;
    private static final double ZOOM_STEP = 1.16;
    private static final StaticTerritoryData EMPTY_STATIC_DATA = new StaticTerritoryData(Resources.EMPTY, List.of());

    private final Screen parent;
    private final EcoMenuFeature ecoFeature;
    private final Map<String, StaticTerritoryData> staticTerritoryData;
    private final List<TerritoryWidget> territoryWidgets = new ArrayList<>();
    private final List<TerritoryWidget> visibleTerritoryWidgets = new ArrayList<>();
    private final List<ConnectionEdge> connectionEdges = new ArrayList<>();
    private final List<Connection> visibleConnections = new ArrayList<>();
    private final Map<String, TerritoryWidget> widgetsByName = new HashMap<>();
    private final Map<String, TerritoryWidget> widgetsByNormalizedName = new HashMap<>();
    private final Map<String, TowerControls> localDefenseControls = new HashMap<>();
    private final Map<String, TerritoryDefenseState> localDefenseStates = new HashMap<>();
    private final EcoMenu detailPanel = new EcoMenu();

    private boolean loadRequested;
    private boolean loading = true;
    private boolean refreshInFlight;
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
    private long nextRefreshMillis;
    private String guildName;
    private String status = "Loading territory data...";
    private WorldBounds worldBounds = WorldBounds.EMPTY;
    private TerritoryWidget selectedTerritory;

    public EcoManagerScreen(Screen parent) {
        this(parent, FeatureManager.getEcoMenuFeature());
    }

    public EcoManagerScreen(Screen parent, EcoMenuFeature ecoFeature) {
        super(Component.literal("Eco Manager"));
        this.parent = parent;
        this.ecoFeature = ecoFeature;
        this.guildName = configuredGuildName();
        this.staticTerritoryData = loadStaticTerritoryData();
    }

    @Override
    protected void init() {
        if (!loadRequested) {
            loadRequested = true;
            loadTerritoriesAsync(false);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float delta) {
        refreshTerritoriesIfDue();

        ClickGuiTheme theme = NiaClickGuiScreen.configuredTheme();
        layoutTerritories();
        drawMap(g, mouseX, mouseY, theme);
        super.render(g, mouseX, mouseY, delta);
        drawDetailPanel(g, mouseX, mouseY, theme);
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
        if (selectedTerritory != null && detailPanel.mouseClicked(mouseX, mouseY, button, width, height)) {
            return true;
        }

        for (int i = visibleTerritoryWidgets.size() - 1; i >= 0; i--) {
            if (visibleTerritoryWidgets.get(i).mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
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
        if (detailPanel.mouseDragged(event.x(), event.y(), width, height)) {
            return true;
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
        if (detailPanel.mouseReleased()) {
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
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        draggingMap = false;
        detailPanel.stopDragging();
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void refreshTerritoriesIfDue() {
        if (!loadRequested || refreshInFlight || territoryWidgets.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now >= nextRefreshMillis) {
            loadTerritoriesAsync(true);
        }
    }

    private void loadTerritoriesAsync(boolean keepExistingOnFailure) {
        refreshInFlight = true;
        loading = territoryWidgets.isEmpty();
        if (loading) {
            status = "Loading territory data...";
        }

        try {
            WynncraftAPI.territoryResponseAsync().whenComplete((response, throwable) -> {
                Minecraft minecraft = Minecraft.getInstance();
                minecraft.execute(() -> {
                    refreshInFlight = false;
                    if (throwable != null) {
                        failLoad(throwable, keepExistingOnFailure);
                        return;
                    }
                    replaceTerritories(response);
                });
            });
        } catch (Throwable throwable) {
            refreshInFlight = false;
            failLoad(throwable, keepExistingOnFailure);
        }
    }

    private void failLoad(Throwable throwable, boolean keepExisting) {
        loading = false;
        scheduleNextRefresh();
        if (keepExisting && !territoryWidgets.isEmpty()) {
            status = "Territory refresh failed";
            NiamodClient.LOGGER.warn("Failed to refresh eco territory data", throwable);
            return;
        }

        territoryWidgets.clear();
        visibleTerritoryWidgets.clear();
        connectionEdges.clear();
        visibleConnections.clear();
        widgetsByName.clear();
        widgetsByNormalizedName.clear();
        worldBounds = WorldBounds.EMPTY;
        selectedTerritory = null;
        layoutDirty = true;
        status = "Failed to load territories";
        NiamodClient.LOGGER.warn("Failed to load eco territory data", throwable);
    }

    private void replaceTerritories(Map<String, TerritoryResponse> response) {
        loading = false;
        scheduleNextRefresh();

        String selectedName = selectedTerritory == null ? null : selectedTerritory.territory().name();
        territoryWidgets.clear();
        visibleTerritoryWidgets.clear();
        connectionEdges.clear();
        visibleConnections.clear();
        widgetsByName.clear();
        widgetsByNormalizedName.clear();
        selectedTerritory = null;
        layoutDirty = true;

        if (response == null || response.isEmpty()) {
            worldBounds = WorldBounds.EMPTY;
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

        territoryWidgets.addAll(loadedWidgets);
        for (TerritoryWidget widget : territoryWidgets) {
            widgetsByName.put(widget.territory().name(), widget);
            widgetsByNormalizedName.put(normalizeName(widget.territory().name()), widget);
        }
        buildConnectionEdges();
        cacheDefenseStates();
        worldBounds = WorldBounds.fromTerritories(territoryWidgets.stream().map(TerritoryWidget::territory).toList());

        if (selectedName != null) {
            selectedTerritory = widgetsByNormalizedName.get(normalizeName(selectedName));
        }

        if (territoryWidgets.isEmpty()) {
            status = "No territories for " + guildName;
        } else {
            status = territoryWidgets.size() + (territoryWidgets.size() == 1 ? " territory" : " territories");
        }
    }

    private TerritoryWidget toWidget(String name, TerritoryResponse response) {
        if (name == null || name.isBlank() || response == null || !isOwnedBySelectedGuild(response)) {
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
        return new TerritoryWidget(territory, this::selectTerritory);
    }

    private void buildConnectionEdges() {
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
                    connectionEdges.add(new ConnectionEdge(widget, target));
                }
            }
        }
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

        if (territoryWidgets.isEmpty() || !worldBounds.valid()) {
            return;
        }

        double availableW = Math.max(1.0, canvas.width() - MAP_PADDING * 2.0);
        double availableH = Math.max(1.0, canvas.height() - MAP_PADDING * 2.0);
        double worldW = Math.max(1.0, worldBounds.width());
        double worldH = Math.max(1.0, worldBounds.height());
        double baseScale = Math.min(availableW / worldW, availableH / worldH);
        if (!Double.isFinite(baseScale) || baseScale <= 0) {
            baseScale = 1.0;
        }
        double scale = baseScale * zoom;
        double worldCenterX = (worldBounds.minX() + worldBounds.maxX()) / 2.0;
        double worldCenterZ = (worldBounds.minZ() + worldBounds.maxZ()) / 2.0;
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

        for (ConnectionEdge edge : connectionEdges) {
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
                visibleConnections.add(new Connection(source, target, clippedLine[0], clippedLine[1], clippedLine[2], clippedLine[3]));
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
                widget.render(g, font, mouseX, mouseY, theme, now, canvas);
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

    private void drawDetailPanel(GuiGraphics g, int mouseX, int mouseY, ClickGuiTheme theme) {
        if (selectedTerritory == null) {
            return;
        }

        TerritoryNode territory = selectedTerritory.territory();
        detailPanel.render(
                g,
                font,
                mouseX,
                mouseY,
                theme,
                selectedTerritory,
                guildName,
                controlsFor(territory),
                defenseStateFor(territory),
                ownedConnectionCount(territory),
                totalConnectionCount(territory),
                width,
                height,
                this::onTerritoryDefenseControlsChanged,
                () -> selectedTerritory = null
        );
    }

    private void drawConnections(GuiGraphics g) {
        double phase = System.currentTimeMillis() / 70.0;
        int color = Render2D.withAlpha(TerritoryResourceColors.connectionColor(), 155);

        for (Connection edge : visibleConnections) {
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
        selectedTerritory = widget;
        controlsFor(widget.territory());
        detailPanel.ensurePosition(width, height);
    }

    private TowerControls controlsFor(TerritoryNode territory) {
        if (ecoFeature != null) {
            return ecoFeature.defenseControlsFor(territory.name());
        }
        return localDefenseControls.computeIfAbsent(normalizeName(territory.name()), ignored -> new TowerControls());
    }

    private TerritoryDefenseState defenseStateFor(TerritoryNode territory) {
        if (ecoFeature != null) {
            return ecoFeature.cachedDefenseStateFor(territory, ownedConnectionCount(territory), totalConnectionCount(territory));
        }
        return localDefenseStates.computeIfAbsent(normalizeName(territory.name()), ignored -> TerritoryDefenseState.EMPTY);
    }

    private void onTerritoryDefenseControlsChanged(TerritoryNode territory, TowerControls controls) {
        if (ecoFeature != null) {
            ecoFeature.setTerritoryDefenses(territory, controls, ownedConnectionCount(territory), totalConnectionCount(territory));
            return;
        }
        localDefenseControls.put(normalizeName(territory.name()), controls);
        localDefenseStates.put(normalizeName(territory.name()), TerritoryDefenseState.EMPTY);
    }

    private void cacheDefenseStates() {
        for (TerritoryWidget widget : territoryWidgets) {
            cacheDefenseState(widget.territory());
        }
    }

    private void cacheDefenseState(TerritoryNode territory) {
        if (ecoFeature != null) {
            ecoFeature.cacheDefenseState(territory, ownedConnectionCount(territory), totalConnectionCount(territory));
            return;
        }
        localDefenseStates.putIfAbsent(normalizeName(territory.name()), TerritoryDefenseState.EMPTY);
    }

    private int ownedConnectionCount(TerritoryNode territory) {
        int count = 0;
        for (String connection : territory.connections()) {
            TerritoryWidget target = widgetsByName.get(connection);
            if (target == null) {
                target = widgetsByNormalizedName.get(normalizeName(connection));
            }
            if (target != null) {
                count++;
            }
        }
        return count;
    }

    private int totalConnectionCount(TerritoryNode territory) {
        return (int) territory.connections().stream()
                .filter(connection -> connection != null && !connection.isBlank())
                .count();
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

    private void scheduleNextRefresh() {
        nextRefreshMillis = System.currentTimeMillis() + refreshIntervalMillis();
    }

    private long refreshIntervalMillis() {
        return Math.max(1L, NyahConfig.getData().getEcoTerritoryRefreshSeconds()) * 1000L;
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

    private UiRect canvasBounds() {
        return new UiRect(0, 0, Math.max(1, width), Math.max(1, height));
    }
}
