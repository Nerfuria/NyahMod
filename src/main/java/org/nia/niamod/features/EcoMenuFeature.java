package org.nia.niamod.features;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.nia.niamod.managers.KeybindManager;
import org.nia.niamod.models.gui.screens.EcoManagerScreen;
import org.nia.niamod.models.gui.territory.TowerControls;
import org.nia.niamod.models.gui.territory.TowerStat;
import org.nia.niamod.models.gui.territory.TerritoryDefenseState;
import org.nia.niamod.models.gui.territory.TerritoryNode;
import org.nia.niamod.models.gui.territory.TreasuryLevel;
import org.nia.niamod.models.misc.Feature;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EcoMenuFeature extends Feature {
    private final Map<String, TowerControls> defenseControls = new HashMap<>();
    private final Map<String, TerritoryDefenseState> defenseStates = new HashMap<>();

    @Override
    public void init() {
        KeybindManager.registerKeybinding("Open Eco Menu", GLFW.GLFW_KEY_DOWN, safeRunnable("open_screen", this::openScreen));
    }

    private void openScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new EcoManagerScreen(minecraft.screen, this));
    }

    public TowerControls defenseControlsFor(String territoryName) {
        return defenseControls.computeIfAbsent(normalizeTerritoryName(territoryName), ignored -> new TowerControls());
    }

    public TerritoryDefenseState cachedDefenseStateFor(TerritoryNode territory) {
        return cachedDefenseStateFor(territory, 0, territory == null ? 0 : territory.connections().size());
    }

    public TerritoryDefenseState cachedDefenseStateFor(TerritoryNode territory, int ownedConnections, int totalConnections) {
        if (territory == null) {
            return TerritoryDefenseState.EMPTY;
        }

        String key = normalizeTerritoryName(territory.name());
        return defenseStates.computeIfAbsent(key, ignored -> calculateDefenseState(territory, defenseControlsFor(territory.name()), ownedConnections, totalConnections));
    }

    public TerritoryDefenseState cacheDefenseState(TerritoryNode territory) {
        return cacheDefenseState(territory, 0, territory == null ? 0 : territory.connections().size());
    }

    public TerritoryDefenseState cacheDefenseState(TerritoryNode territory, int ownedConnections, int totalConnections) {
        if (territory == null) {
            return TerritoryDefenseState.EMPTY;
        }

        TerritoryDefenseState state = calculateDefenseState(territory, defenseControlsFor(territory.name()), ownedConnections, totalConnections);
        defenseStates.put(normalizeTerritoryName(territory.name()), state);
        return state;
    }

    public TerritoryDefenseState setTerritoryDefenses(TerritoryNode territory, TowerControls controls) {
        return setTerritoryDefenses(territory, controls, 0, territory == null ? 0 : territory.connections().size());
    }

    public TerritoryDefenseState setTerritoryDefenses(TerritoryNode territory, TowerControls controls, int ownedConnections, int totalConnections) {
        if (territory == null) {
            return TerritoryDefenseState.EMPTY;
        }

        String key = normalizeTerritoryName(territory.name());
        TowerControls cachedControls = controls == null ? new TowerControls() : controls;
        defenseControls.put(key, cachedControls);

        TerritoryDefenseState state = calculateDefenseState(territory, cachedControls, ownedConnections, totalConnections);
        defenseStates.put(key, state);
        applyTerritoryDefenses(territory, cachedControls, state);
        return state;
    }

    public TerritoryDefenseState calculateDefenseState(TerritoryNode territory, TowerControls controls) {
        return calculateDefenseState(territory, controls, 0, territory == null ? 0 : territory.connections().size());
    }

    public TerritoryDefenseState calculateDefenseState(TerritoryNode territory, TowerControls controls, int ownedConnections, int totalConnections) {
        TowerControls activeControls = controls == null ? new TowerControls() : controls;
        return new TerritoryDefenseState(
                calculateTowerStat(territory, activeControls, TowerStat.DAMAGE, ownedConnections, totalConnections),
                calculateTowerStat(territory, activeControls, TowerStat.ATTACK_SPEED, ownedConnections, totalConnections),
                calculateTowerStat(territory, activeControls, TowerStat.HEALTH, ownedConnections, totalConnections),
                calculateTowerStat(territory, activeControls, TowerStat.DEFENSE, ownedConnections, totalConnections),
                calculateTowerStat(territory, activeControls, TowerStat.AURA, ownedConnections, totalConnections),
                calculateTowerStat(territory, activeControls, TowerStat.VOLLEY, ownedConnections, totalConnections),
                calculateAverageDps(territory, activeControls, ownedConnections, totalConnections),
                calculateEffectiveHealth(territory, activeControls, ownedConnections, totalConnections),
                calculateDefenseSummary(territory, activeControls, ownedConnections, totalConnections),
                calculateTreasury(territory, ownedConnections, totalConnections)
        );
    }

    public double calculateTowerStat(TerritoryNode territory, TowerControls controls, TowerStat stat) {
        return calculateTowerStat(territory, controls, stat, 0, territory == null ? 0 : territory.connections().size());
    }

    public double calculateTowerStat(TerritoryNode territory, TowerControls controls, TowerStat stat, int ownedConnections, int totalConnections) {
        double value = calculateBaseTowerStat(territory, controls, stat, ownedConnections, totalConnections);
        if (controls != null && controls.hq() && (stat == TowerStat.HEALTH || stat == TowerStat.DEFENSE)) {
            value *= 2.5;
        }
        return value;
    }

    public double calculateAverageDps(TerritoryNode territory, TowerControls controls) {
        return calculateAverageDps(territory, controls, 0, territory == null ? 0 : territory.connections().size());
    }

    public double calculateAverageDps(TerritoryNode territory, TowerControls controls, int ownedConnections, int totalConnections) {
        return 0.0;
    }

    public double calculateEffectiveHealth(TerritoryNode territory, TowerControls controls) {
        return calculateEffectiveHealth(territory, controls, 0, territory == null ? 0 : territory.connections().size());
    }

    public double calculateEffectiveHealth(TerritoryNode territory, TowerControls controls, int ownedConnections, int totalConnections) {
        return 0.0;
    }

    public double calculateDefenseSummary(TerritoryNode territory, TowerControls controls) {
        return calculateDefenseSummary(territory, controls, 0, territory == null ? 0 : territory.connections().size());
    }

    public double calculateDefenseSummary(TerritoryNode territory, TowerControls controls, int ownedConnections, int totalConnections) {
        return 0.0;
    }

    public TreasuryLevel calculateTreasury(TerritoryNode territory) {
        return calculateTreasury(territory, 0, territory == null ? 0 : territory.connections().size());
    }

    public TreasuryLevel calculateTreasury(TerritoryNode territory, int ownedConnections, int totalConnections) {
        return TreasuryLevel.LOW;
    }

    private double calculateBaseTowerStat(TerritoryNode territory, TowerControls controls, TowerStat stat, int ownedConnections, int totalConnections) {
        return 0.0;
    }

    private void applyTerritoryDefenses(TerritoryNode territory, TowerControls controls, TerritoryDefenseState state) {
        // Future territory-manager side effects can be wired here.
    }

    private String normalizeTerritoryName(String territoryName) {
        return territoryName == null ? "" : territoryName.trim().toLowerCase(Locale.ROOT);
    }
}
