package org.nia.niamod.models.misc;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

public class RepeatingTask extends DelayedTask {
    private final IntSupplier intervalTicks;
    private final BooleanSupplier cancelCondition;
    private int currentTicks;

    public RepeatingTask(Runnable task, int delayTicks, IntSupplier intervalTicks, BooleanSupplier cancelCondition) {
        super(task, delayTicks);
        this.intervalTicks = intervalTicks;
        this.cancelCondition = cancelCondition;
    }

    public RepeatingTask(Runnable task, int delayTicks, int intervalTicks) {
        this(task, delayTicks, () -> intervalTicks, () -> false);
    }

    public RepeatingTask(Runnable task, IntSupplier intervalTicks) {
        this(task, 0, intervalTicks, () -> false);
    }


    @Override
    public boolean tick() {
        if (cancelCondition.getAsBoolean()) {
            return true;
        }

        if (ticksRemaining > 0) {
            ticksRemaining--;
        }

        if (ticksRemaining <= 0) {
            task.run();
            ticksRemaining = intervalTicks.getAsInt();
        }

        return false;
    }
}
