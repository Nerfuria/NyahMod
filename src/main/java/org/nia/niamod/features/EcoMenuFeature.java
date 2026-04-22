package org.nia.niamod.features;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.nia.niamod.features.eco.EcoLoadouts;
import org.nia.niamod.features.eco.EcoGameActions;
import org.nia.niamod.features.eco.EcoUncommittedChanges;
import org.nia.niamod.managers.KeybindManager;
import org.nia.niamod.models.gui.screens.EcoManagerScreen;
import org.nia.niamod.models.territory.TerritoryLoadout;
import org.nia.niamod.models.territory.TerritoryRoute;
import org.nia.niamod.models.territory.TerritoryUpgrade;
import org.nia.niamod.models.misc.Feature;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public class EcoMenuFeature extends Feature {
    private static final List<TerritoryLoadout> LOADOUTS = EcoLoadouts.defaults();

    private final EcoGameActions gameActions;
    private final EcoUncommittedChanges uncommittedChanges = new EcoUncommittedChanges();
    private final Map<String, EnumMap<TerritoryUpgrade, Integer>> territoryStats = new HashMap<>();
    private final Map<String, String> territoryLoadouts = new HashMap<>();
    private final Map<String, Integer> territoryTaxes = new HashMap<>();
    private final Map<String, Boolean> territoryBorders = new HashMap<>();
    private final Map<String, TerritoryRoute> territoryRoutes = new HashMap<>();
    private String headquartersTerritoryName;

    public EcoMenuFeature() {
        this(new EcoGameActions());
    }

    public EcoMenuFeature(EcoGameActions gameActions) {
        this.gameActions = gameActions == null ? new EcoGameActions() : gameActions;
    }

    @Override
    public void init() {
        KeybindManager.registerKeybinding("Open Eco Menu", GLFW.GLFW_KEY_DOWN, safeRunnable("open_screen", this::openScreen));
    }

    private void openScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new EcoManagerScreen(minecraft.screen, this));
    }

    public void requestStats(Collection<String> territoryNames) {
        forEachTerritory(territoryNames, key -> territoryStats.putIfAbsent(key, blankStats()));
    }

    public Map<TerritoryUpgrade, Integer> statsFor(String territoryName) {
        String key = normalize(territoryName);
        return Collections.unmodifiableMap(new EnumMap<>(key.isEmpty() ? blankStats() : mutableStatsFor(key)));
    }

    public int statLevel(String territoryName, TerritoryUpgrade stat) {
        String key = normalize(territoryName);
        return stat == null || key.isEmpty() ? 0 : mutableStatsFor(key).getOrDefault(stat, 0);
    }

    public void setStat(String territoryName, TerritoryUpgrade stat, int level) {
        if (stat == null) {
            return;
        }

        String key = normalize(territoryName);
        if (!key.isEmpty()) {
            int nextLevel = clamp(level, 0, stat.maxLevel());
            EnumMap<TerritoryUpgrade, Integer> stats = mutableStatsFor(key);
            if (stats.getOrDefault(stat, 0) == nextLevel) {
                return;
            }
            stats.put(stat, nextLevel);
            territoryLoadouts.remove(key);
            uncommittedChanges.markStat(key, stat);
            gameActions.pushStat(cleanTerritoryName(territoryName), stat, nextLevel);
        }
    }

    public void adjustStat(String territoryName, TerritoryUpgrade stat, int delta) {
        setStat(territoryName, stat, statLevel(territoryName, stat) + delta);
    }

    public List<TerritoryLoadout> loadouts() {
        return LOADOUTS;
    }

    public String loadoutFor(String territoryName) {
        return territoryLoadouts.getOrDefault(normalize(territoryName), "");
    }

    public boolean applyLoadout(String loadoutName, Collection<String> territoryNames) {
        TerritoryLoadout loadout = findLoadout(loadoutName);
        if (loadout == null || territoryNames == null || territoryNames.isEmpty()) {
            return false;
        }

        List<String> targets = territoryNames.stream()
                .map(this::cleanTerritoryName)
                .filter(territoryName -> !territoryName.isEmpty())
                .toList();
        if (targets.isEmpty()) {
            return false;
        }

        forEachTerritory(targets, key -> {
            EnumMap<TerritoryUpgrade, Integer> stats = blankStats();
            for (Map.Entry<TerritoryUpgrade, Integer> entry : loadout.levels().entrySet()) {
                TerritoryUpgrade upgrade = entry.getKey();
                if (upgrade != null) {
                    stats.put(upgrade, clamp(entry.getValue() == null ? 0 : entry.getValue(), 0, upgrade.maxLevel()));
                }
            }
            territoryStats.put(key, stats);
            territoryLoadouts.put(key, loadout.name());
            uncommittedChanges.markAllStats(key);
        });
        gameActions.applyLoadout(loadout, targets);
        return true;
    }

    public void setHeadquarters(String territoryName) {
        String cleaned = cleanTerritoryName(territoryName);
        if (cleaned.isEmpty() || normalize(cleaned).equals(normalize(headquartersTerritoryName))) {
            return;
        }

        headquartersTerritoryName = cleaned;
        uncommittedChanges.markHeadquarters(normalize(cleaned));
        gameActions.pushHeadquarters(cleaned);
    }

    public boolean isHeadquarters(String territoryName) {
        String key = normalize(territoryName);
        return !key.isEmpty() && key.equals(normalize(headquartersTerritoryName));
    }

    public int tax(String territoryName) {
        return territoryTaxes.getOrDefault(normalize(territoryName), 0);
    }

    public void setTax(String territoryName, int percent) {
        String key = normalize(territoryName);
        if (!key.isEmpty()) {
            int nextTax = clamp(percent, 0, 70);
            if (territoryTaxes.getOrDefault(key, 0) == nextTax) {
                return;
            }
            territoryTaxes.put(key, nextTax);
            uncommittedChanges.markTax(key);
            gameActions.pushTax(cleanTerritoryName(territoryName), nextTax);
        }
    }

    public boolean bordersOpen(String territoryName) {
        return territoryBorders.getOrDefault(normalize(territoryName), false);
    }

    public void setBordersOpen(String territoryName, boolean open) {
        String key = normalize(territoryName);
        if (!key.isEmpty()) {
            if (territoryBorders.getOrDefault(key, false) == open) {
                return;
            }
            territoryBorders.put(key, open);
            uncommittedChanges.markBorders(key);
            gameActions.pushBorders(cleanTerritoryName(territoryName), open);
        }
    }

    public TerritoryRoute territoryRoute(String territoryName) {
        return territoryRoutes.getOrDefault(normalize(territoryName), TerritoryRoute.FASTEST);
    }

    public void setTerritoryRoute(String territoryName, TerritoryRoute route) {
        String key = normalize(territoryName);
        if (!key.isEmpty() && route != null) {
            if (territoryRoute(territoryName) == route) {
                return;
            }
            territoryRoutes.put(key, route);
            uncommittedChanges.markRoute(key);
            gameActions.pushRoute(cleanTerritoryName(territoryName), route);
        }
    }

    public void toggleTerritoryRoute(String territoryName) {
        setTerritoryRoute(territoryName, territoryRoute(territoryName).toggled());
    }

    public void applyTerritoryRoute(String territoryName) {
        applyTerritoryRoute(territoryName, territoryRoute(territoryName));
    }

    public void applyTerritoryRoute(String territoryName, TerritoryRoute route) {
        String key = normalize(territoryName);
        if (key.isEmpty() || route == null) {
            return;
        }

        gameActions.pushRoute(cleanTerritoryName(territoryName), route);
    }

    public void receiveStatsFromGame(String territoryName, Map<TerritoryUpgrade, Integer> stats) {
        String key = normalize(territoryName);
        if (key.isEmpty() || stats == null) {
            return;
        }

        EnumMap<TerritoryUpgrade, Integer> current = mutableStatsFor(key);
        for (Map.Entry<TerritoryUpgrade, Integer> entry : stats.entrySet()) {
            TerritoryUpgrade stat = entry.getKey();
            if (stat == null) {
                continue;
            }

            int level = clamp(entry.getValue() == null ? 0 : entry.getValue(), 0, stat.maxLevel());
            if (uncommittedChanges.hasStat(key, stat)) {
                if (current.getOrDefault(stat, 0) == level) {
                    uncommittedChanges.clearStat(key, stat);
                }
                continue;
            }
            current.put(stat, level);
        }
    }

    public void receiveTaxFromGame(String territoryName, int percent) {
        String key = normalize(territoryName);
        if (key.isEmpty()) {
            return;
        }

        int tax = clamp(percent, 0, 70);
        if (uncommittedChanges.hasTax(key)) {
            if (territoryTaxes.getOrDefault(key, 0) == tax) {
                uncommittedChanges.clearTax(key);
            }
            return;
        }
        territoryTaxes.put(key, tax);
    }

    public void receiveBordersFromGame(String territoryName, boolean open) {
        String key = normalize(territoryName);
        if (key.isEmpty()) {
            return;
        }

        if (uncommittedChanges.hasBorders(key)) {
            if (territoryBorders.getOrDefault(key, false) == open) {
                uncommittedChanges.clearBorders(key);
            }
            return;
        }
        territoryBorders.put(key, open);
    }

    public void receiveRouteFromGame(String territoryName, TerritoryRoute route) {
        String key = normalize(territoryName);
        if (key.isEmpty() || route == null) {
            return;
        }

        if (uncommittedChanges.hasRoute(key)) {
            if (territoryRoute(territoryName) == route) {
                uncommittedChanges.clearRoute(key);
            }
            return;
        }
        territoryRoutes.put(key, route);
    }

    public void receiveHeadquartersFromGame(String territoryName) {
        String cleaned = cleanTerritoryName(territoryName);
        String key = normalize(cleaned);
        if (uncommittedChanges.hasHeadquarters()) {
            if (uncommittedChanges.headquarters().equals(key)) {
                uncommittedChanges.clearHeadquarters();
            }
            return;
        }
        headquartersTerritoryName = cleaned.isEmpty() ? null : cleaned;
    }

    public void pushUncommittedChanges() {
        uncommittedChanges.stats().forEach((territoryKey, stats) -> {
            EnumMap<TerritoryUpgrade, Integer> current = mutableStatsFor(territoryKey);
            for (TerritoryUpgrade stat : stats) {
                gameActions.pushStat(territoryKey, stat, current.getOrDefault(stat, 0));
            }
        });
        for (String territoryKey : uncommittedChanges.taxes()) {
            gameActions.pushTax(territoryKey, territoryTaxes.getOrDefault(territoryKey, 0));
        }
        for (String territoryKey : uncommittedChanges.borders()) {
            gameActions.pushBorders(territoryKey, territoryBorders.getOrDefault(territoryKey, false));
        }
        for (String territoryKey : uncommittedChanges.routes()) {
            gameActions.pushRoute(territoryKey, territoryRoutes.getOrDefault(territoryKey, TerritoryRoute.FASTEST));
        }
        if (uncommittedChanges.hasHeadquarters()) {
            gameActions.pushHeadquarters(uncommittedChanges.headquarters());
        }
    }

    private TerritoryLoadout findLoadout(String loadoutName) {
        String key = normalize(loadoutName);
        for (TerritoryLoadout loadout : LOADOUTS) {
            if (normalize(loadout.name()).equals(key)) {
                return loadout;
            }
        }
        return null;
    }

    private void forEachTerritory(Collection<String> territoryNames, Consumer<String> action) {
        if (territoryNames == null) {
            return;
        }

        for (String territoryName : territoryNames) {
            String key = normalize(territoryName);
            if (!key.isEmpty()) {
                action.accept(key);
            }
        }
    }

    private EnumMap<TerritoryUpgrade, Integer> mutableStatsFor(String territoryName) {
        return territoryStats.computeIfAbsent(normalize(territoryName), ignored -> blankStats());
    }

    private EnumMap<TerritoryUpgrade, Integer> blankStats() {
        EnumMap<TerritoryUpgrade, Integer> stats = new EnumMap<>(TerritoryUpgrade.class);
        for (TerritoryUpgrade upgrade : TerritoryUpgrade.values()) {
            stats.put(upgrade, 0);
        }
        return stats;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String normalize(String territoryName) {
        return territoryName == null ? "" : territoryName.trim().toLowerCase(Locale.ROOT);
    }

    private String cleanTerritoryName(String territoryName) {
        return territoryName == null ? "" : territoryName.trim();
    }
}
