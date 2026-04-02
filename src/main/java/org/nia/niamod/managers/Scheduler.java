package org.nia.niamod.managers;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.nia.niamod.models.misc.DelayedTask;

import java.util.LinkedList;
import java.util.List;

public class Scheduler {
    private static List<DelayedTask> tasks;

    public static void init() {
        tasks = new LinkedList<>();
        ClientTickEvents.END_CLIENT_TICK.register((s) -> tick());
    }

    public static void schedule(Runnable task, int delayTicks) {
        tasks.add(new DelayedTask(task, delayTicks));
    }

    public static void tick() {
        tasks.removeIf(DelayedTask::tick);
    }
}
