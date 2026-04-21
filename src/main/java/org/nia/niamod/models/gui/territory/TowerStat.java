package org.nia.niamod.models.gui.territory;

public enum TowerStat {
    DAMAGE("Damage", 0, 11),
    ATTACK_SPEED("Attacks per second", 0, 11),
    HEALTH("Health", 0, 11),
    DEFENSE("Defense", 0, 11),
    AURA("Aura", 0, 3),
    VOLLEY("Volley", 0, 3);

    private final String label;
    private final int minLevel;
    private final int maxLevel;

    TowerStat(String label, int minLevel, int maxLevel) {
        this.label = label;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
    }

    public String label() {
        return label;
    }

    public int minLevel() {
        return minLevel;
    }

    public int maxLevel() {
        return maxLevel;
    }
}
