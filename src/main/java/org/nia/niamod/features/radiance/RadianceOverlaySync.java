package org.nia.niamod.features.radiance;

import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.mixin.PlayerListHudAccessor;
import org.nia.niamod.models.config.RadianceOverlayMode;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RadianceOverlaySync {
    private static final Pattern RADIANCE_COOLDOWN_PATTERN = Pattern.compile(
            "(?i)\\bradiance\\b[^0-9]*\\(?\\s*(\\d{1,2})\\s*:\\s*(\\d{2})\\s*\\)?");
    private static final long OVERLAY_MAX_MS = 15000L;
    private static final long CONNECTION_NOTICE_TIMEOUT_MS = 6000L;

    private final WarStateTracker warStateTracker;
    private final CloudflareSync cloudflareSync = new CloudflareSync();

    private long timerStartMs;
    private double timerDurationSeconds;
    private double timerBufferedDurationSeconds;
    private boolean timerActive;
    private boolean pinged;
    private long castPromptUntilMs;

    private boolean wasOnCooldown;
    private boolean connectionNoticePending;
    private long connectionNoticeDeadlineMs;
    private boolean syncConnectionActive;
    @Getter
    private boolean manualConnectRequested;

    public RadianceOverlaySync(WarStateTracker warStateTracker) {
        this.warStateTracker = warStateTracker;
    }

    private static String sanitizeText(String value) {
        String cleaned = value.replace("\u0000", "");
        cleaned = cleaned.replaceAll("(?i)§[0-9a-fk-or]", "");
        cleaned = cleaned.replaceAll("(?i)&[0-9a-fk-or]", "");
        cleaned = cleaned.replaceAll("&\\{[^}]*}", "");
        cleaned = cleaned.replaceAll("&\\[[^]]*]", "");
        cleaned = cleaned.replaceAll("&<[^>]*>", "");
        return cleaned.trim();
    }

    public void onJoin() {
        connectionNoticePending = false;
        syncConnectionActive = false;
        manualConnectRequested = false;
        warStateTracker.reset();
        cloudflareSync.reset();
    }

    public boolean isRequireWar() {
        return NyahConfig.nyahConfigData.isRadianceSyncRequireWar();
    }

    public void setRequireWar(boolean requireWar) {
        if (NyahConfig.nyahConfigData.isRadianceSyncRequireWar() == requireWar) {
            return;
        }
        NyahConfig.nyahConfigData.setRadianceSyncRequireWar(requireWar);
        NyahConfig.save();
    }

    public void setManualConnectRequested(boolean manualConnectRequested) {
        if (this.manualConnectRequested == manualConnectRequested) {
            return;
        }
        this.manualConnectRequested = manualConnectRequested;
        if (!manualConnectRequested && !syncConnectionActive) {
            connectionNoticePending = false;
            cloudflareSync.reset();
        }
    }

    public int getSelfAspectTier() {
        return NyahConfig.nyahConfigData.getRadianceSyncSelfTier();
    }

    public void setSelfAspectTier(int tier) {
        if (tier < 0 || tier > 3 || NyahConfig.nyahConfigData.getRadianceSyncSelfTier() == tier) {
            return;
        }
        NyahConfig.nyahConfigData.setRadianceSyncSelfTier(tier);
        NyahConfig.save();
    }

    public RadianceOverlayMode getOverlayMode() {
        RadianceOverlayMode mode = NyahConfig.nyahConfigData.getRadianceSyncOverlayMode();
        return mode == null ? RadianceOverlayMode.CAST : mode;
    }

    public boolean isStatusOverlayMode() {
        return getOverlayMode() == RadianceOverlayMode.STATUS;
    }

    public String getGroupKey() {
        String key = NyahConfig.nyahConfigData.getRadianceSyncGroupKey();
        return key == null ? "" : key;
    }

    public void setGroupKey(String key) {
        String normalized = key == null ? "" : key.trim();
        if (normalized.equals(getGroupKey())) {
            return;
        }
        NyahConfig.nyahConfigData.setRadianceSyncGroupKey(normalized);
        NyahConfig.save();
        cloudflareSync.reset();
    }

    public void onClientTick(Minecraft client) {
        long now = System.currentTimeMillis();
        if (client == null || client.player == null) {
            return;
        }
        String key = getGroupKey();
        String playerName = client.player.getGameProfile().name();
        String playerUuid = client.player.getGameProfile().id() == null
                ? ""
                : client.player.getGameProfile().id().toString();
        boolean warActive = warStateTracker.isInWar();
        boolean connectAllowed = warActive || manualConnectRequested;
        updateSyncConnection(client, now, key, playerName, playerUuid, connectAllowed);

        if (NyahConfig.nyahConfigData.isRadianceSyncRequireWar() && !warActive) {
            clearTimer();
            clearCastPrompt();
            wasOnCooldown = false;
            return;
        }
        boolean statusMode = isStatusOverlayMode();

        Double cooldownSecondsBoxed = getCooldownRemainingSeconds(client);
        boolean onCooldown = cooldownSecondsBoxed != null;
        if (cooldownSecondsBoxed != null) {
            if (!wasOnCooldown) {
                double cooldownSeconds = cooldownSecondsBoxed;
                double remainingDuration = getDetectedRemainingDurationSeconds(NyahConfig.nyahConfigData.getRadianceSyncSelfTier(), cooldownSeconds);
                cloudflareSync.send(NyahConfig.nyahConfigData.getRadianceSyncSelfTier(), toMillis(remainingDuration));
                if (statusMode) {
                    startTimerAt(now, remainingDuration, remainingDuration);
                }
            }
        }
        wasOnCooldown = onCooldown;

        CloudflareSync.Entry cfEntry = cloudflareSync.consumePending();
        if (cfEntry != null) {
            int tier = cfEntry.tier();
            if (tier >= 0 && tier <= 3) {
                double remainingDuration = getRemoteRemainingDurationSeconds(now, cfEntry, tier);
                if (remainingDuration > 0.0) {
                    double buffered = statusMode
                            ? remainingDuration
                            : applyCastModePairBuffer(NyahConfig.nyahConfigData.getRadianceSyncSelfTier(), tier, remainingDuration);
                    startTimerAt(now, remainingDuration, buffered);
                }
            }
        }

        if (timerActive) {
            double remaining = getRemainingBufferedSeconds(now);
            if (!pinged && remaining <= 0.0) {
                playPing(client);
                pinged = true;
                if (!statusMode) {
                    showCastPrompt(now);
                }
            }
            if (!statusMode && remaining <= 0.0 && pinged && !isCastPromptActive(now)) {
                clearTimer();
            }
        }

        if (timerActive && now - timerStartMs > OVERLAY_MAX_MS) {
            clearTimer();
        }
        if (!timerActive && !isCastPromptActive(now)) {
            clearCastPrompt();
        }
    }

    public void onHudRender(GuiGraphics drawContext, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        if (NyahConfig.nyahConfigData.isRadianceSyncRequireWar() && !warStateTracker.isInWar()) {
            return;
        }
        long now = System.currentTimeMillis();
        boolean statusMode = isStatusOverlayMode();
        boolean castPromptActive = !statusMode && isCastPromptActive(now);

        float scale = NyahConfig.nyahConfigData.getRadianceSyncOverlayScale();
        int width = drawContext.guiWidth();
        int height = drawContext.guiHeight();
        int baseX = (int) ((width / 2.0f) / scale);
        int baseY = (int) ((height * 2.0f / 3.0f) / scale);
        baseX += Math.round(NyahConfig.nyahConfigData.getRadianceSyncOverlayOffsetX() / scale);
        baseY -= Math.round(NyahConfig.nyahConfigData.getRadianceSyncOverlayOffsetY() / scale);
        drawContext.pose().pushMatrix();
        drawContext.pose().scale(scale, scale);
        int primaryLineY = baseY;
        int cooldownLineY = baseY + 12;
        if (statusMode) {
            if (timerActive) {
                double remaining = Math.max(0.0, getRemainingSeconds(now));
                String text = String.format(Locale.ROOT, "RADIANCE: %.1fs", remaining);
                int color = remaining <= 0.0 ? 0xFFFF5555 : 0xFFFFFF55;
                drawCenteredText(drawContext, client, text, baseX, primaryLineY, color);
            }
            drawCooldownLine(drawContext, client, baseX, cooldownLineY);
        } else {
            if (timerActive) {
                double remaining = getRemainingBufferedSeconds(now);
                if (remaining > 0.0) {
                    String text = String.format(Locale.ROOT, "RADIANCE IN %.1fs", Math.max(0.0, remaining));
                    drawCenteredText(drawContext, client, text, baseX, primaryLineY, 0xFFFFFF55);
                } else if (castPromptActive) {
                    drawCenteredText(drawContext, client, "CAST RADIANCE", baseX, primaryLineY, 0xFFFF5555);
                }
            }
            drawCooldownLine(drawContext, client, baseX, cooldownLineY);
        }
        drawContext.pose().popMatrix();
    }

    private void updateSyncConnection(Minecraft client,
                                      long now,
                                      String key,
                                      String playerName,
                                      String playerUuid,
                                      boolean warActive) {
        boolean hasIdentity = playerName != null && !playerName.isBlank()
                && !playerUuid.isBlank();
        boolean shouldConnect = warActive && isValidKey(key) && hasIdentity;
        if (shouldConnect) {
            if (!syncConnectionActive) {
                connectionNoticePending = true;
                connectionNoticeDeadlineMs = now + CONNECTION_NOTICE_TIMEOUT_MS;
            }
            cloudflareSync.tick(key, playerName, playerUuid);
        } else {
            connectionNoticePending = false;
            if (syncConnectionActive) {
                cloudflareSync.reset();
            }
        }
        syncConnectionActive = shouldConnect;

        if (!connectionNoticePending) {
            return;
        }
        if (cloudflareSync.isConnected()) {
            connectionNoticePending = false;
            client.player.displayClientMessage(Component.literal("[RadianceSync] Wavelink Protocol active").withStyle(ChatFormatting.GREEN), false);
        } else if (now >= connectionNoticeDeadlineMs) {
            connectionNoticePending = false;
            client.player.displayClientMessage(Component.literal("[RadianceSync] Wavelink unreachable").withStyle(ChatFormatting.RED), false);
        }
    }

    private void startTimerAt(long startMs, double durationSeconds, double bufferedDurationSeconds) {
        timerActive = true;
        timerStartMs = startMs;
        timerDurationSeconds = durationSeconds;
        timerBufferedDurationSeconds = bufferedDurationSeconds;
        clearCastPrompt();
        pinged = false;
    }

    private void clearTimer() {
        timerActive = false;
        pinged = false;
    }

    private void showCastPrompt(long now) {
        long durationMs = Math.round(NyahConfig.nyahConfigData.getRadianceSyncCastPromptSeconds() * 1000.0);
        castPromptUntilMs = now + Math.max(0L, durationMs);
    }

    private boolean isCastPromptActive(long now) {
        return now < castPromptUntilMs;
    }

    private void clearCastPrompt() {
        castPromptUntilMs = 0L;
    }

    private void drawCooldownLine(GuiGraphics drawContext, Minecraft client, int centerX, int y) {
        CooldownOverlayLine cooldownLine = getCooldownOverlayLine(client);
        drawCenteredText(drawContext, client, cooldownLine.text, centerX, y, cooldownLine.color);
    }

    private CooldownOverlayLine getCooldownOverlayLine(Minecraft client) {
        Double cooldownSeconds = getCooldownRemainingSeconds(client);
        if (cooldownSeconds == null) {
            return new CooldownOverlayLine("RADIANCE READY", 0xFF55FF55);
        }
        String cooldownText = String.format(Locale.ROOT, "COOLDOWN: %.0fs", Math.max(0.0, cooldownSeconds));
        return new CooldownOverlayLine(cooldownText, 0xFF55FFFF);
    }

    private void drawCenteredText(GuiGraphics drawContext,
                                  Minecraft client,
                                  String text,
                                  int centerX,
                                  int y,
                                  int color) {
        int textWidth = client.font.width(text);
        drawContext.drawString(client.font, text, centerX - textWidth / 2, y, color, true);
    }

    private double getRemainingSeconds(long nowMs) {
        double durationMs = timerDurationSeconds * 1000.0;
        return (durationMs - (nowMs - timerStartMs)) / 1000.0;
    }

    private double getRemainingBufferedSeconds(long nowMs) {
        double durationMs = timerBufferedDurationSeconds * 1000.0;
        return (durationMs - (nowMs - timerStartMs)) / 1000.0;
    }

    private void playPing(Minecraft client) {
        if (!NyahConfig.nyahConfigData.isRadianceSyncPingEnabled() || client.player == null) {
            return;
        }
        client.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(),
                NyahConfig.nyahConfigData.getRadianceSyncPingVolume(),
                NyahConfig.nyahConfigData.getRadianceSyncPingPitch());
    }

    private double getDurationForTier(int tier) {
        return switch (tier) {
            case 1 -> 9.0;
            case 2 -> 10.5;
            case 3 -> 11.5;
            default -> 7.0;
        };
    }

    private double applyCastModePairBuffer(int selfTier, int partnerTier, double durationSeconds) {
        double bufferSeconds = getCastModePairBufferSeconds(selfTier, partnerTier);
        return Math.max(0.0, durationSeconds - bufferSeconds);
    }

    private double getCastModePairBufferSeconds(int selfTier, int partnerTier) {
        if (selfTier == 3 && partnerTier == 3) {
            return 4.0;
        }
        if ((selfTier == 0 && partnerTier == 3) || (selfTier == 3 && partnerTier == 0)) {
            return 2.0;
        }
        double selfExtra = getTierExtraDuration(selfTier);
        double partnerExtra = getTierExtraDuration(partnerTier);
        return (selfExtra + partnerExtra) / 2.0;
    }

    private double getTierExtraDuration(int tier) {
        return switch (tier) {
            case 1 -> 2.0;
            case 2 -> 3.5;
            case 3 -> 4.5;
            default -> 0.0;
        };
    }

    private double getDetectedRemainingDurationSeconds(int tier, double cooldownSeconds) {
        return Math.max(0.0, Math.min(getDurationForTier(tier), cooldownSeconds));
    }

    private double getRemoteRemainingDurationSeconds(long now, CloudflareSync.Entry entry, int tier) {
        if (entry.remainingMs() > 0L) {
            long remainingMs = entry.remainingMs();
            remainingMs -= cloudflareSync.getEstimatedOneWayLatencyMs();
            if (entry.receivedAtMs() > 0L && now > entry.receivedAtMs()) {
                remainingMs -= (now - entry.receivedAtMs());
            }
            if (remainingMs <= 0L) {
                return 0.0;
            }
            return remainingMs / 1000.0;
        }
        return getDurationForTier(tier);
    }

    private Double getCooldownRemainingSeconds(Minecraft client) {
        if (!(client.gui.getTabList() instanceof PlayerListHudAccessor accessor)) {
            return null;
        }
        Component footer = accessor.niamod$getFooter();
        if (footer == null) {
            return null;
        }
        String footerText = sanitizeText(footer.getString());
        if (footerText.isBlank()) {
            return null;
        }
        for (String line : footerText.split("\\n")) {
            if (line.isBlank()) {
                continue;
            }
            Matcher matcher = RADIANCE_COOLDOWN_PATTERN.matcher(line);
            if (matcher.find()) {
                int minutes = parseIntSafe(matcher.group(1));
                int seconds = parseIntSafe(matcher.group(2));
                if (minutes >= 0 && seconds >= 0) {
                    return minutes * 60.0 + seconds;
                }
            }
        }
        return null;
    }

    private int parseIntSafe(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private long toMillis(double seconds) {
        return Math.max(0L, Math.round(seconds * 1000.0));
    }

    private boolean isValidKey(String key) {
        return key != null && !key.isBlank() && key.length() <= 64;
    }

    private record CooldownOverlayLine(String text, int color) {
    }
}
