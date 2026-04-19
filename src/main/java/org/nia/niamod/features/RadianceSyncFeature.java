package org.nia.niamod.features;

import lombok.Getter;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.nia.niamod.eventbus.NiaEventBus;
import org.nia.niamod.eventbus.Subscribe;
import org.nia.niamod.features.radiance.RadianceCommand;
import org.nia.niamod.features.radiance.RadianceOverlaySync;
import org.nia.niamod.features.radiance.WarStateTracker;
import org.nia.niamod.managers.OverlayManager;
import org.nia.niamod.models.events.ChatMessageReceivedEvent;
import org.nia.niamod.models.misc.Feature;
import org.nia.niamod.models.misc.Safe;

@SuppressWarnings("unused")
public class RadianceSyncFeature extends Feature {

    @Getter
    private RadianceOverlaySync overlay;
    private WarStateTracker warStateTracker;

    @Override
    @Safe
    public void init() {
        warStateTracker = new WarStateTracker();
        overlay = new RadianceOverlaySync(warStateTracker);

        NiaEventBus.subscribe(this);
        ClientTickEvents.END_CLIENT_TICK.register(client ->
                runSafe("onClientTick", () -> onClientTick(client)));
        OverlayManager.registerOverlay(overlay);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                runSafe("onJoin", this::onJoin));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                runSafe("onJoin", this::onJoin));
        RadianceCommand.register();
    }

    @Subscribe
    @Safe
    public void onChatMessage(ChatMessageReceivedEvent event) {
        if (event.message() != null) {
            warStateTracker.handleMessage(event.message().getString());
        }
    }

    @Safe
    public void onClientTick(Minecraft client) {
        overlay.onClientTick(client);
    }

    @Safe
    public void onHudRender(GuiGraphics drawContext, DeltaTracker tickCounter) {
        overlay.onHudRender(drawContext, tickCounter);
    }

    @Safe
    public void onJoin() {
        overlay.onJoin();
    }
}
