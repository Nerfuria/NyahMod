package org.nia.niamod.models.gui.territory;

public enum TerritoryUpgrade {
    DAMAGE(new Level[]{
            new Level(0L, 0.0),
            new Level(100L, 40.0),
            new Level(300L, 80.0),
            new Level(600L, 120.0),
            new Level(1200L, 160.0),
            new Level(2400L, 200.0),
            new Level(4800L, 240.0),
            new Level(8400L, 280.0),
            new Level(12000L, 320.0),
            new Level(15600L, 360.0),
            new Level(19200L, 400.0),
            new Level(22800L, 440.0)
    }),
    ATTACK(new Level[]{
            new Level(0L, 0.0),
            new Level(100L, 50.0),
            new Level(300L, 100.0),
            new Level(600L, 150.0),
            new Level(1200L, 220.0),
            new Level(2400L, 300.0),
            new Level(4800L, 400.0),
            new Level(8400L, 500.0),
            new Level(12000L, 620.0),
            new Level(15600L, 660.0),
            new Level(19200L, 740.0),
            new Level(22800L, 840.0)
    }),
    HEALTH(new Level[]{
            new Level(0L, 0.0),
            new Level(100L, 50.0),
            new Level(300L, 100.0),
            new Level(600L, 150.0),
            new Level(1200L, 220.0),
            new Level(2400L, 300.0),
            new Level(4800L, 400.0),
            new Level(8400L, 520.0),
            new Level(12000L, 640.0),
            new Level(15600L, 760.0),
            new Level(19200L, 880.0),
            new Level(22800L, 1000.0)
    }),
    DEFENSE(new Level[]{
            new Level(0L, 0.0),
            new Level(100L, 300.0),
            new Level(300L, 450.0),
            new Level(600L, 525.0),
            new Level(1200L, 600.0),
            new Level(2400L, 650.0),
            new Level(4800L, 690.0),
            new Level(8400L, 720.0),
            new Level(12000L, 740.0),
            new Level(15600L, 760.0),
            new Level(19200L, 780.0),
            new Level(22800L, 800.0)
    }),
    TOWER_AURA(new Level[]{
            new Level(0L, 0.0),
            new Level(800L, 24.0),
            new Level(1600L, 18.0),
            new Level(3200L, 12.0)
    }),
    TOWER_VOLLEY(new Level[]{
            new Level(0L, 0.0),
            new Level(200L, 20.0),
            new Level(400L, 15.0),
            new Level(800L, 10.0)
    }),
    STRONGER_MINIONS(new Level[]{
            new Level(0L, 0.0),
            new Level(200L, 150.0),
            new Level(400L, 200.0),
            new Level(800L, 250.0),
            new Level(1600L, 300.0)
    }),
    TOWER_MULTI_ATTACKS(new Level[]{
            new Level(0L, 1.0),
            new Level(4800L, 2.0)
    }),
    GATHERING_EXPERIENCE(new Level[]{
            new Level(0L, 0.0),
            new Level(600L, 10.0),
            new Level(1300L, 20.0),
            new Level(2000L, 30.0),
            new Level(2700L, 40.0),
            new Level(3400L, 50.0),
            new Level(5500L, 60.0),
            new Level(10000L, 80.0),
            new Level(20000L, 100.0)
    }),
    MOB_EXPERIENCE(new Level[]{
            new Level(0L, 0.0),
            new Level(600L, 10.0),
            new Level(1200L, 20.0),
            new Level(1800L, 30.0),
            new Level(2400L, 40.0),
            new Level(3000L, 50.0),
            new Level(5000L, 60.0),
            new Level(10000L, 80.0),
            new Level(20000L, 100.0)
    }),
    MOB_DAMAGE(new Level[]{
            new Level(0L, 0.0),
            new Level(600L, 10.0),
            new Level(1200L, 20.0),
            new Level(1800L, 40.0),
            new Level(2400L, 60.0),
            new Level(3000L, 80.0),
            new Level(5000L, 120.0),
            new Level(10000L, 160.0),
            new Level(20000L, 200.0)
    }),
    PVP_DAMAGE(new Level[]{
            new Level(0L, 0.0),
            new Level(600L, 5.0),
            new Level(1200L, 10.0),
            new Level(1800L, 15.0),
            new Level(2400L, 20.0),
            new Level(3000L, 25.0),
            new Level(5000L, 40.0),
            new Level(10000L, 65.0),
            new Level(20000L, 80.0)
    }),
    XP_SEEKING(new Level[]{
            new Level(0L, 0.0),
            new Level(100L, 36000.0),
            new Level(200L, 66000.0),
            new Level(400L, 120000.0),
            new Level(800L, 228000.0),
            new Level(1600L, 456000.0),
            new Level(3200L, 900000.0),
            new Level(6400L, 1740000.0),
            new Level(9600L, 2580000.0),
            new Level(12800L, 3360000.0)
    }),
    TOME_SEEKING(new Level[]{
            new Level(0L, 0.0),
            new Level(400L, 0.15),
            new Level(3200L, 1.2),
            new Level(6400L, 2.4)
    }),
    EMERALD_SEEKING(new Level[]{
            new Level(0L, 0.0),
            new Level(200L, 0.3),
            new Level(800L, 3.0),
            new Level(1600L, 6.0),
            new Level(3200L, 12.0),
            new Level(6400L, 24.0)
    }),
    RESOURCE_STORAGE(new Level[]{
            new Level(0L, 0.0),
            new Level(400L, 100.0),
            new Level(800L, 300.0),
            new Level(2000L, 700.0),
            new Level(5000L, 1400.0),
            new Level(16000L, 3300.0),
            new Level(48000L, 7900.0)
    }),
    EMERALD_STORAGE(new Level[]{
            new Level(0L, 0.0),
            new Level(200L, 100.0),
            new Level(400L, 300.0),
            new Level(1000L, 700.0),
            new Level(2500L, 1400.0),
            new Level(8000L, 3300.0),
            new Level(24000L, 7900.0)
    }),
    EFFICIENT_RESOURCES(new Level[]{
            new Level(0L, 0.0),
            new Level(6000L, 50.0),
            new Level(12000L, 100.0),
            new Level(24000L, 150.0),
            new Level(48000L, 200.0),
            new Level(96000L, 250.0),
            new Level(192000L, 300.0)
    }),
    RESOURCE_RATE(new Level[]{
            new Level(0L, 4.0),
            new Level(6000L, 3.0),
            new Level(18000L, 2.0),
            new Level(32000L, 1.0)
    }),
    EFFICIENT_EMERALDS(new Level[]{
            new Level(0L, 0.0),
            new Level(2000L, 35.0),
            new Level(8000L, 100.0),
            new Level(32000L, 300.0)
    }),
    EMERALD_RATE(new Level[]{
            new Level(0L, 4.0),
            new Level(2000L, 3.0),
            new Level(8000L, 2.0),
            new Level(32000L, 1.0)
    });

    private final Level[] levels;

    TerritoryUpgrade(Level[] levels) {
        this.levels = levels;
    }

    public double bonus(int level) {
        int clamped = Math.max(0, Math.min(level, levels.length - 1));
        return levels[clamped].bonus();
    }

    public Level[] levels() {
        return levels;
    }

    public record Level(long cost, double bonus) {
    }
}
