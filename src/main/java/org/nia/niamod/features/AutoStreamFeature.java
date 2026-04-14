package org.nia.niamod.features;

import net.minecraft.client.Minecraft;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.eventbus.NiaEventBus;
import org.nia.niamod.eventbus.Subscribe;
import org.nia.niamod.managers.Scheduler;
import org.nia.niamod.models.events.BossBarNameEvent;
import org.nia.niamod.models.misc.Feature;

public class AutoStreamFeature extends Feature {

    private long lastSeen;

    public void init() {
        NiaEventBus.subscribe(this);
    }

    @Subscribe
    private void onBossBar(BossBarNameEvent event) {
        if (event.getTitle().getString().contains("Streamer mode enabled")) {
            lastSeen = System.currentTimeMillis();
            return;
        }
        if (lastSeen - System.currentTimeMillis() < NyahConfig.nyahConfigData.getStreamCooldown()) return;
        Scheduler.schedule(() -> Minecraft.getInstance().getConnection().sendCommand("stream"), 10);
    }

}
