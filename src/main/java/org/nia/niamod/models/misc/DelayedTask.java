package org.nia.niamod.models.misc;

public class DelayedTask {
    private final Runnable task;
    private int ticksRemaining;

    public DelayedTask(Runnable task, int delayTicks) {
        this.task = task;
        this.ticksRemaining = delayTicks;
    }

    public boolean tick() {
        if (ticksRemaining > 0) {
            ticksRemaining--;
        }

        if (ticksRemaining <= 0) {
            task.run();
            return true;
        }

        return false;
    }
}