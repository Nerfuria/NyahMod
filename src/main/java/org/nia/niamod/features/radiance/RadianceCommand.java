package org.nia.niamod.features.radiance;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.managers.FeatureManager;

import java.util.Locale;
import java.util.function.Consumer;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class RadianceCommand {
    private static final String PREFIX = "[RadianceSync] ";
    private static boolean hiddenCommandHandlerRegistered;

    private RadianceCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    literal("radiancesync")
                            .executes(ctx -> openConfig(ctx.getSource()))
                            .then(literal("aspect")
                                    .executes(ctx -> cycleAspectTier(ctx.getSource()))
                                    .then(argument("tier", StringArgumentType.word())
                                            .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                    new String[]{"none", "t1", "t2", "t3"}, builder))
                                            .executes(ctx -> setAspectTier(
                                                    ctx.getSource(),
                                                    StringArgumentType.getString(ctx, "tier")))))
                            .then(literal("requirewar")
                                    .then(argument("value", StringArgumentType.word())
                                            .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                    new String[]{"on", "off"}, builder))
                                            .executes(ctx -> setRequireWar(
                                                    ctx.getSource(),
                                                    StringArgumentType.getString(ctx, "value")))))
                            .then(literal("key")
                                    .then(argument("value", StringArgumentType.greedyString())
                                            .executes(ctx -> setGroupKey(
                                                    ctx.getSource(),
                                                    StringArgumentType.getString(ctx, "value")))))
                            .then(literal("overlay")
                                    .executes(ctx -> openOverlayEditor(ctx.getSource())))
            );
        });

        if (!hiddenCommandHandlerRegistered) {
            hiddenCommandHandlerRegistered = true;
            ClientSendMessageEvents.ALLOW_COMMAND.register(command -> !handleHiddenCommand(command));
        }
    }

    private static int openConfig(FabricClientCommandSource source) {
        Minecraft client = Minecraft.getInstance();
        client.submit(() -> client.setScreen(NyahConfig.getConfigScreen(client.screen)));
        return 1;
    }

    private static int cycleAspectTier(FabricClientCommandSource source) {
        RadianceOverlaySync sync = getSync(source);
        if (sync == null) {
            return 0;
        }
        int next = (sync.getSelfAspectTier() + 1) % 4;
        sync.setSelfAspectTier(next);
        sendFeedback(source, "Your Radiance Tier = " + next);
        return 1;
    }

    private static int setAspectTier(FabricClientCommandSource source, String rawTier) {
        RadianceOverlaySync sync = getSync(source);
        if (sync == null) {
            return 0;
        }
        Integer tier = parseAspectTier(rawTier);
        if (tier == null) {
            sendError(source, "tier expects none/t1/t2/t3.");
            return 0;
        }
        sync.setSelfAspectTier(tier);
        sendFeedback(source, "Your Radiance Tier = " + tier);
        return 1;
    }

    private static int setGroupKey(FabricClientCommandSource source, String rawKey) {
        RadianceOverlaySync sync = getSync(source);
        if (sync == null) {
            return 0;
        }
        String key = rawKey == null ? "" : rawKey.trim();
        if (key.isBlank()) {
            sendError(source, "key expects a non-empty value.");
            return 0;
        }
        if (key.length() > 64) {
            sendError(source, "key must be 64 characters or fewer.");
            return 0;
        }
        sync.setGroupKey(key);
        sendFeedback(source, "Radiance sync group key updated.");
        return 1;
    }

    private static int setRequireWar(FabricClientCommandSource source, String rawValue) {
        RadianceOverlaySync sync = getSync(source);
        if (sync == null) {
            return 0;
        }
        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        boolean next;
        switch (normalized) {
            case "on" -> next = true;
            case "off" -> next = false;
            default -> {
                sendError(source, "requirewar expects on/off.");
                return 0;
            }
        }
        sync.setRequireWar(next);
        sendFeedback(source, "Overlay requires being in war: " + next);
        return 1;
    }

    private static int openOverlayEditor(FabricClientCommandSource source) {
        RadianceOverlaySync sync = getSync(source);
        if (sync == null) {
            return 0;
        }
        Minecraft client = Minecraft.getInstance();
        client.submit(() -> client.setScreen(new RadianceOverlayEditorScreen(client.screen, sync)));
        return 1;
    }

    private static RadianceOverlaySync getSync(FabricClientCommandSource source) {
        RadianceOverlaySync sync = getSyncOrNull();
        if (sync == null) {
            sendError(source, "Radiance overlay not initialized.");
        }
        return sync;
    }

    private static boolean handleHiddenCommand(String rawCommand) {
        if (rawCommand == null) {
            return false;
        }
        String command = rawCommand.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        if (command.equals("radiancesync connect")) {
            enableManualConnectFromChat();
            return true;
        }
        if (command.equals("radiancesync disconnect")) {
            disableManualConnectFromChat();
            return true;
        }
        return false;
    }

    private static void enableManualConnectFromChat() {
        RadianceOverlaySync sync = getSyncForHiddenCommand();
        if (sync == null) {
            return;
        }
        setManualConnectRequested(sync, true, RadianceCommand::sendClientFeedback, RadianceCommand::sendClientError);
    }

    private static void disableManualConnectFromChat() {
        RadianceOverlaySync sync = getSyncForHiddenCommand();
        if (sync == null) {
            return;
        }
        setManualConnectRequested(sync, false, RadianceCommand::sendClientFeedback, RadianceCommand::sendClientError);
    }

    private static RadianceOverlaySync getSyncForHiddenCommand() {
        RadianceOverlaySync sync = getSyncOrNull();
        if (sync == null) {
            sendClientError("Radiance overlay not initialized.");
        }
        return sync;
    }

    private static RadianceOverlaySync getSyncOrNull() {
        var feature = FeatureManager.getRadianceSyncFeature();
        if (feature == null || feature.getOverlay() == null) {
            return null;
        }
        return feature.getOverlay();
    }

    private static void setManualConnectRequested(RadianceOverlaySync sync,
                                                  boolean enabled,
                                                  Consumer<String> feedback,
                                                  Consumer<String> error) {
        if (enabled && sync.getGroupKey().isBlank()) {
            error.accept("Set a Group Key before forcing a connection.");
            return;
        }
        if (sync.isManualConnectRequested() == enabled) {
            feedback.accept(enabled
                    ? "Manual connection is already enabled."
                    : "Manual sync connection is already disabled.");
            return;
        }
        sync.setManualConnectRequested(enabled);
        feedback.accept(enabled
                ? "Manual sync connection enabled. Use /radiancesync disconnect to stop testing outside war."
                : "Manual sync connection disabled.");
    }

    private static Integer parseAspectTier(String rawValue) {
        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "none" -> 0;
            case "t1" -> 1;
            case "t2" -> 2;
            case "t3" -> 3;
            default -> null;
        };
    }

    private static void sendFeedback(FabricClientCommandSource source, String message) {
        source.sendFeedback(Component.literal(PREFIX + message));
    }

    private static void sendError(FabricClientCommandSource source, String message) {
        source.sendError(Component.literal(PREFIX + message));
    }

    private static void sendClientFeedback(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.displayClientMessage(Component.literal(PREFIX + message), false);
        }
    }

    private static void sendClientError(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.displayClientMessage(Component.literal(PREFIX + message).withStyle(ChatFormatting.RED), false);
        }
    }

}
