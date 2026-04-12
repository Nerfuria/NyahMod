package org.nia.niamod.managers;

import lombok.experimental.UtilityClass;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.nia.niamod.models.misc.DelayedTask;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@UtilityClass
public class Scheduler {
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() / 2));
    private static ConcurrentLinkedDeque<DelayedTask> tasks;

    public static void init() {
        tasks = new ConcurrentLinkedDeque<>();
        ClientTickEvents.END_CLIENT_TICK.register((s) -> tick());
    }

    public static void schedule(Runnable task, int delayTicks) {
        tasks.add(new DelayedTask(task, delayTicks));
    }

    public static void scheduleAsync(Runnable task, int delayTicks) {
        tasks.add(new DelayedTask(() -> EXECUTOR_SERVICE.execute(task), delayTicks));
    }

    public static void tick() {
        tasks.removeIf(DelayedTask::tick);
    }
}
