package org.nia.niamod.models.gui.territory;

public class TowerControls {
    private boolean hq;
    private int damageLevel;
    private int attackSpeedLevel;
    private int healthLevel;
    private int defenseLevel;
    private int auraLevel;
    private int volleyLevel;

    public boolean hq() {
        return hq;
    }

    public void toggleHq() {
        hq = !hq;
    }

    public int level(TowerStat stat) {
        return switch (stat) {
            case DAMAGE -> damageLevel;
            case ATTACK_SPEED -> attackSpeedLevel;
            case HEALTH -> healthLevel;
            case DEFENSE -> defenseLevel;
            case AURA -> auraLevel;
            case VOLLEY -> volleyLevel;
        };
    }

    public void adjust(TowerStat stat, int delta) {
        setLevel(stat, level(stat) + delta);
    }

    private void setLevel(TowerStat stat, int level) {
        int clamped = Math.max(stat.minLevel(), Math.min(stat.maxLevel(), level));
        switch (stat) {
            case DAMAGE -> damageLevel = clamped;
            case ATTACK_SPEED -> attackSpeedLevel = clamped;
            case HEALTH -> healthLevel = clamped;
            case DEFENSE -> defenseLevel = clamped;
            case AURA -> auraLevel = clamped;
            case VOLLEY -> volleyLevel = clamped;
        }
    }
}
