package org.nia.niamod.models.gui.territory;

public record TerritoryDefenseState(
        double damage,
        double attackSpeed,
        double health,
        double defense,
        double aura,
        double volley,
        double averageDps,
        double effectiveHealth,
        double defenseSummary,
        TreasuryLevel treasury
) {
    public static final TerritoryDefenseState EMPTY = new TerritoryDefenseState(
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            0.0,
            TreasuryLevel.LOW
    );

    public double towerStat(TowerStat stat) {
        return switch (stat) {
            case DAMAGE -> damage;
            case ATTACK_SPEED -> attackSpeed;
            case HEALTH -> health;
            case DEFENSE -> defense;
            case AURA -> aura;
            case VOLLEY -> volley;
        };
    }
}
