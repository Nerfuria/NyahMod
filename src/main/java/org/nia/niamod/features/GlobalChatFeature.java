package org.nia.niamod.features;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.wynntils.core.text.StyledText;
import com.wynntils.utils.colors.CustomColor;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.eventbus.NiaEventBus;
import org.nia.niamod.eventbus.Subscribe;
import org.nia.niamod.managers.Scheduler;
import org.nia.niamod.models.events.ServerJoinEvent;
import org.nia.niamod.models.misc.Feature;
import org.nia.niamod.models.misc.Safe;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import com.wynntils.core.text.fonts.WynnFont;

public class GlobalChatFeature extends Feature implements WebSocket.Listener {
    private WebSocket ws;
    private int tries = 5;

    @Override
    public void init() {
        NiaEventBus.subscribe(this);
        ClientCommandRegistrationCallback.EVENT.register(((dispatcher, buildContext) -> {
            dispatcher.register(ClientCommandManager.literal("gc").then(ClientCommandManager.argument("message", StringArgumentType.greedyString()).executes(this::onMessage)));
            dispatcher.register(ClientCommandManager.literal("globalchat").then(ClientCommandManager.argument("message", StringArgumentType.greedyString()).executes(this::onMessage)));
        }));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (ws != null) {
                ws.sendClose(WebSocket.NORMAL_CLOSURE, "Client disconnected").thenRun(() -> ws = null);
            }
        });
    }

    private int onMessage(CommandContext<FabricClientCommandSource> ctx) {
        if (!NyahConfig.nyahConfigData.isGlobalChatEnabled()) {
            Minecraft.getInstance().player.displayClientMessage(Component.literal("Global Chat is disabled."), false);
        }
        String message = StringArgumentType.getString(ctx, "message");
        if (ws != null) {
            ws.sendText(message, true);
        } else {
            Minecraft.getInstance().player.displayClientMessage(Component.literal("Not connected to Websocket"), false);
        }
        return 1;
    }

    @Subscribe
    @Safe
    public void onJoin(ServerJoinEvent event) {
        Scheduler.scheduleAsync(
                () -> tryConnect(event.serverId()),
                100);
    }

    private void tryConnect(String serverId) {
        if (tries == 0) return;
        try {
            HttpClient client = HttpClient.newHttpClient();
            ws = client.newWebSocketBuilder().header("User-Agent", "NiaMod").buildAsync(URI.create(NyahConfig.nyahConfigData.getGlobalChatURL() + "?serverId=" + serverId + "&name=" + Minecraft.getInstance().getUser().getName()), this).join();
        } catch (CompletionException e) {
            tries--;
            Scheduler.scheduleAsync(() -> tryConnect(serverId), 100);
        }
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        Minecraft.getInstance().execute(() -> {
            MutableComponent prelim = StyledText.fromString(WynnFont.asBackgroundFont("Attacker Com", CustomColor.fromInt(0xFFFFFFFF),CustomColor.fromInt(0xFF3b1344), "NONE", "FLAG")).getComponent();
            JsonObject obj = JsonParser.parseString(data.toString()).getAsJsonObject();
            if (obj.has("error")) {
                Minecraft.getInstance().player.displayClientMessage(prelim.append(Component.literal(" You have been rate limited.").withColor(0xFFBF0A30)), false);

            } else {
                Minecraft.getInstance().player.displayClientMessage(prelim.append(Component.literal(" " + obj.get("user").getAsString()).withColor(0xFFCEA2FD).append(Component.literal(": " + obj.get("msg").getAsString()).withColor(0xFFFFFFFF))), false);
            }
        });
        return WebSocket.Listener.super.onText(webSocket, data, last);
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (!enabled && ws != null) ws.sendClose(WebSocket.NORMAL_CLOSURE, "Feature disabled").thenRun(() -> ws = null);
        super.setEnabled(enabled);
    }

}
