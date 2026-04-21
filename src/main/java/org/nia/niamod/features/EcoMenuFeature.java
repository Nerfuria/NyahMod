package org.nia.niamod.features;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.nia.niamod.managers.KeybindManager;
import org.nia.niamod.models.gui.screens.EcoManagerScreen;
import org.nia.niamod.models.gui.territory.DamageRange;
import org.nia.niamod.models.gui.territory.ResourceFlowState;
import org.nia.niamod.models.gui.territory.Resources;
import org.nia.niamod.models.gui.territory.TerritoryNode;
import org.nia.niamod.models.gui.territory.TerritoryResourceStore;
import org.nia.niamod.models.gui.territory.TerritoryUpgrade;
import org.nia.niamod.models.misc.Feature;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EcoMenuFeature extends Feature {
    public static final long HEADQUARTER_RESOURCE_CAPACITY = 120_000L;

    private final Map<String, EnumMap<TerritoryUpgrade, Integer>> territoryUpgrades = new HashMap<>();
    private final Map<String, TerritoryResourceStore> territoryResourceStores = new HashMap<>();
    private String headquarterTerritoryName;

    @Override
    public void init() {
        KeybindManager.registerKeybinding("Open Eco Menu", GLFW.GLFW_KEY_DOWN, safeRunnable("open_screen", this::openScreen));
    }

    private void openScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(new EcoManagerScreen(minecraft.screen, this));
    }

    public Map<TerritoryUpgrade, Integer> upgradesFor(String territoryName) {
        return Collections.unmodifiableMap(new EnumMap<>(mutableUpgradesFor(territoryName)));
    }

    public int upgradeLevel(String territoryName, TerritoryUpgrade upgrade) {
        if (upgrade == null) {
            return 0;
        }
        return mutableUpgradesFor(territoryName).getOrDefault(upgrade, 0);
    }

    public void setUpgradeLevel(String territoryName, TerritoryUpgrade upgrade, int level) {
        if (upgrade == null) {
            return;
        }
        mutableUpgradesFor(territoryName).put(upgrade, clampLevel(upgrade, level));
    }

    public void adjustUpgradeLevel(String territoryName, TerritoryUpgrade upgrade, int delta) {
        setUpgradeLevel(territoryName, upgrade, upgradeLevel(territoryName, upgrade) + delta);
    }

    public boolean isHeadquarters(String territoryName) {
        String territoryKey = normalizeTerritoryName(territoryName);
        String headquarterKey = normalizeTerritoryName(headquarterTerritoryName);
        return !territoryKey.isEmpty() && !headquarterKey.isEmpty() && territoryKey.equals(headquarterKey);
    }

    public String headquarterTerritoryName() {
        return headquarterTerritoryName;
    }

    public void setHeadquarters(String territoryName) {
        String normalized = normalizeTerritoryName(territoryName);
        headquarterTerritoryName = normalized.isEmpty() ? null : territoryName.trim();
    }

    public void toggleHeadquarters(String territoryName) {
        if (isHeadquarters(territoryName)) {
            headquarterTerritoryName = null;
            return;
        }
        setHeadquarters(territoryName);
    }

    public void setTerritoryResourceStore(String territoryName, long current, long max) {
        String key = normalizeTerritoryName(territoryName);
        if (key.isEmpty()) {
            return;
        }
        territoryResourceStores.put(key, new TerritoryResourceStore(current, max));
    }

    public void setHeadquarterResources(long current) {
        if (headquarterTerritoryName == null || headquarterTerritoryName.isBlank()) {
            return;
        }
        territoryResourceStores.put(
                normalizeTerritoryName(headquarterTerritoryName),
                new TerritoryResourceStore(current, HEADQUARTER_RESOURCE_CAPACITY)
        );
    }

    public TerritoryResourceStore resourceStoreFor(String territoryName) {
        String key = normalizeTerritoryName(territoryName);
        TerritoryResourceStore stored = territoryResourceStores.getOrDefault(key, TerritoryResourceStore.EMPTY);
        if (isHeadquarters(territoryName)) {
            long max = stored.max() > 0L ? stored.max() : HEADQUARTER_RESOURCE_CAPACITY;
            return new TerritoryResourceStore(stored.current(), max);
        }
        return stored;
    }

    public DamageRange calculateDamage(TerritoryNode territory, int directConnections, int externalConnections) {
        if (territory == null) {
            return DamageRange.EMPTY;
        }

        double territoryMultiplier = territoryMultiplier(territory, directConnections, externalConnections);
        double bonusMultiplier = percentMultiplier(territory, TerritoryUpgrade.DAMAGE);
        return new DamageRange(
                1_000.0 * bonusMultiplier * territoryMultiplier,
                1_500.0 * bonusMultiplier * territoryMultiplier
        );
    }

    public double calculateAttackSpeed(TerritoryNode territory) {
        if (territory == null) {
            return 0.0;
        }
        return 0.5 * percentMultiplier(territory, TerritoryUpgrade.ATTACK);
    }

    public double calculateHealth(TerritoryNode territory, int directConnections, int externalConnections) {
        if (territory == null) {
            return 0.0;
        }
        return 300_000.0 * percentMultiplier(territory, TerritoryUpgrade.HEALTH) * territoryMultiplier(territory, directConnections, externalConnections);
    }

    public double calculateDefense(TerritoryNode territory) {
        if (territory == null) {
            return 0.0;
        }
        return 0.1 * percentMultiplier(territory, TerritoryUpgrade.DEFENSE);
    }

    public double calculateAura(TerritoryNode territory) {
        return territory == null ? 0.0 : upgradeBonus(territory, TerritoryUpgrade.TOWER_AURA);
    }

    public double calculateVolley(TerritoryNode territory) {
        return territory == null ? 0.0 : upgradeBonus(territory, TerritoryUpgrade.TOWER_VOLLEY);
    }

    public double calculateAverageDps(TerritoryNode territory, int directConnections, int externalConnections) {
        if (territory == null) {
            return 0.0;
        }
        return calculateDamage(territory, directConnections, externalConnections).average() * calculateAttackSpeed(territory);
    }

    public double calculateEffectiveHealth(TerritoryNode territory, int directConnections, int externalConnections) {
        if (territory == null) {
            return 0.0;
        }

        double defense = calculateDefense(territory);
        if (defense >= 1.0) {
            return Double.POSITIVE_INFINITY;
        }
        return calculateHealth(territory, directConnections, externalConnections) / (1.0 - defense);
    }

    public double calculateResourceGainPerHour(TerritoryNode territory) {
        if (territory == null) {
            return 0.0;
        }
        return calculateProducedResourcesPerHour(territory).materialTotal();
    }

    public double calculateEmeraldGainPerHour(TerritoryNode territory) {
        if (territory == null) {
            return 0.0;
        }
        return calculateProducedResourcesPerHour(territory).emeralds();
    }

    public Resources calculateProducedResourcesPerHour(TerritoryNode territory) {
        if (territory == null) {
            return Resources.EMPTY;
        }

        Resources base = territory.resources();
        return new Resources(
                roundProductionPerHour(base.emeralds(), territory, TerritoryUpgrade.EMERALD_RATE, TerritoryUpgrade.EFFICIENT_EMERALDS),
                roundProductionPerHour(base.ore(), territory, TerritoryUpgrade.RESOURCE_RATE, TerritoryUpgrade.EFFICIENT_RESOURCES),
                roundProductionPerHour(base.crops(), territory, TerritoryUpgrade.RESOURCE_RATE, TerritoryUpgrade.EFFICIENT_RESOURCES),
                roundProductionPerHour(base.fish(), territory, TerritoryUpgrade.RESOURCE_RATE, TerritoryUpgrade.EFFICIENT_RESOURCES),
                roundProductionPerHour(base.wood(), territory, TerritoryUpgrade.RESOURCE_RATE, TerritoryUpgrade.EFFICIENT_RESOURCES)
        );
    }

    public long calculateResourceCostPerHour(TerritoryNode territory) {
        if (territory == null) {
            return 0L;
        }

        long total = 0L;
        for (TerritoryUpgrade upgrade : TerritoryUpgrade.values()) {
            total += upgrade.cost(upgradeLevel(territory.name(), upgrade));
        }
        return total;
    }

    public ResourceFlowState summarizeResourceFlow(Collection<TerritoryNode> territories) {
        double resourceGain = 0.0;
        double emeraldGain = 0.0;
        long resourceLoss = 0L;

        for (TerritoryNode territory : territories) {
            resourceGain += calculateResourceGainPerHour(territory);
            emeraldGain += calculateEmeraldGainPerHour(territory);
            resourceLoss += calculateResourceCostPerHour(territory);
        }

        TerritoryResourceStore headquarterStore = resourceStoreFor(headquarterTerritoryName);
        double usagePercent = resourceGain <= 0.0 ? 0.0 : resourceLoss / resourceGain * 100.0;
        return new ResourceFlowState(
                headquarterTerritoryName,
                headquarterStore.current(),
                headquarterStore.max(),
                resourceGain,
                resourceLoss,
                emeraldGain,
                usagePercent
        );
    }

    private EnumMap<TerritoryUpgrade, Integer> mutableUpgradesFor(String territoryName) {
        return territoryUpgrades.computeIfAbsent(normalizeTerritoryName(territoryName), ignored -> new EnumMap<>(TerritoryUpgrade.class));
    }

    private int clampLevel(TerritoryUpgrade upgrade, int level) {
        return Math.max(0, Math.min(level, upgrade.maxLevel()));
    }

    private double territoryMultiplier(TerritoryNode territory, int directConnections, int externalConnections) {
        double connectionMultiplier = 1.0 + (0.3 * directConnections);
        if (isHeadquarters(territory.name())) {
            return connectionMultiplier * (1.5 + (0.25 * externalConnections));
        }
        return connectionMultiplier;
    }

    private double upgradeBonus(TerritoryNode territory, TerritoryUpgrade upgrade) {
        return upgrade.bonus(upgradeLevel(territory.name(), upgrade));
    }

    private double percentMultiplier(TerritoryNode territory, TerritoryUpgrade upgrade) {
        return 1.0 + (upgradeBonus(territory, upgrade) / 100.0);
    }

    private double calculateProductionPerHour(int basePerHour, TerritoryNode territory, TerritoryUpgrade rateUpgrade, TerritoryUpgrade efficientUpgrade) {
        if (basePerHour <= 0 || territory == null) {
            return 0.0;
        }

        double basePerProduction = basePerHour / 900.0;
        double secondsPerProduction = Math.max(1.0, upgradeBonus(territory, rateUpgrade));
        double efficientMultiplier = 1.0 + (upgradeBonus(territory, efficientUpgrade) / 100.0);
        return basePerProduction * (3600.0 / secondsPerProduction) * efficientMultiplier;
    }

    private int roundProductionPerHour(int basePerHour, TerritoryNode territory, TerritoryUpgrade rateUpgrade, TerritoryUpgrade efficientUpgrade) {
        return (int) Math.round(calculateProductionPerHour(basePerHour, territory, rateUpgrade, efficientUpgrade));
    }

    private String normalizeTerritoryName(String territoryName) {
        return territoryName == null ? "" : territoryName.trim().toLowerCase(Locale.ROOT);
    }
}
