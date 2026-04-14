package org.nia.niamod.features;

import com.wynntils.core.components.Models;
import com.wynntils.core.text.StyledText;
import com.wynntils.core.text.fonts.WynnFont;
import com.wynntils.models.territories.TerritoryAttackTimer;
import com.wynntils.utils.colors.CustomColor;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.lwjgl.glfw.GLFW;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.managers.KeybindManager;
import org.nia.niamod.managers.Scheduler;
import org.nia.niamod.models.misc.Feature;
import org.nia.niamod.models.misc.Safe;
import org.nia.niamod.models.records.Territory;
import org.nia.niamod.models.records.TimerEntry;
import org.nia.niamod.render.Render3D;
import org.nia.niamod.util.WynncraftAPI;

import java.util.Comparator;
import java.util.HashMap;

public class WarTimersFeature extends Feature {

    private final HashMap<String, Territory> territories = new HashMap<>();
    private boolean showAll;

    @Safe
    public void init() {
        WynncraftAPI.territoryResponse().forEach((k, v) -> territories.put(k, new Territory(k, new BlockPos(v.location().start()[0], -100, v.location().start()[1]), new BlockPos(v.location().end()[0], 256, v.location().end()[1]))));
        WorldRenderEvents.AFTER_ENTITIES.register(context ->
                runSafe("render", () -> render(context)));
        KeybindManager.registerKeybinding("Show unqueued territories", GLFW.GLFW_KEY_SEMICOLON, () -> showAll = !showAll);

        scheduleWarn();
    }

    private void scheduleWarn() {
        Scheduler.scheduleRepeating(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            MutableComponent prelim = StyledText.fromString(WynnFont.asBackgroundFont("QUEUES", CustomColor.fromInt(0xFFEE6B6E), CustomColor.fromInt(0xFF3b1344), "NONE", "FLAG")).getComponent();
            Models.GuildAttackTimer.getUpcomingTimers()
                    .filter(timer -> timer.timerEnd() - System.currentTimeMillis() < NyahConfig.nyahConfigData.getMaxTimeTerr() * 60 * 1000L)
                    .sorted(Comparator.comparing(TerritoryAttackTimer::timerEnd))
                    .limit(NyahConfig.nyahConfigData.getTerritoryWarningCount())
                    .map(timer -> Component.literal(" " + timer.territoryName() + " - " + timeFromNow(timer.timerEnd()) + "\n").withColor(0xFFEE6B6E))
                    .forEach(timer -> mc.player.displayClientMessage(prelim.copy().append(timer), false));
        }, () -> NyahConfig.nyahConfigData.getWarnTime(), this::isDisabled);
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        if (enabled) {
            scheduleWarn();
        }
        this.enabled = enabled;
    }

    private String timeFromNow(long end) {
        long now = System.currentTimeMillis();
        long diff = end - now;
        int minutes = Math.toIntExact((diff / 1000) / 60);
        int seconds = Math.toIntExact((diff / 1000) % 60);

        return String.format("%02d:%02d", minutes, seconds);
    }

    @Safe
    public void render(WorldRenderContext ctx) {
        int color = NyahConfig.nyahConfigData.getColor();
        int r = color >> 16 & 0xFF, g = color >> 8 & 0xFF, b = color & 0xFF;

        int colorInside = NyahConfig.nyahConfigData.getColorInside();
        int ri = colorInside >> 16 & 0xFF, gi = colorInside >> 8 & 0xFF, bi = colorInside & 0xFF;

        LocalPlayer player = Minecraft.getInstance().player;

        Models.GuildAttackTimer.getUpcomingTimers()
                .map(timer -> {
                    Territory t = territories.get(timer.territoryName());
                    return new TimerEntry(timer, t, t.distance());
                })
                .filter(e -> e.distance() <= NyahConfig.nyahConfigData.getMaximumDistance() * NyahConfig.nyahConfigData.getMaximumDistance())
                .sorted(Comparator
                        .comparing(TimerEntry::timer, Comparator.comparing(TerritoryAttackTimer::timerEnd))
                        .thenComparingInt(TimerEntry::distance))
                .limit(NyahConfig.nyahConfigData.getMaximumTerritories())
                .map(TimerEntry::territory)
                .forEach(t -> {
                    render(ctx, r, g, b, ri, gi, bi, player, t);
                });
        if (showAll) {
            int colorq = NyahConfig.nyahConfigData.getNotQColor();
            int rq = colorq >> 16 & 0xFF, gq = colorq >> 8 & 0xFF, bq = colorq & 0xFF;

            int colorqinside = NyahConfig.nyahConfigData.getNotQInsideColor();
            int rqi = colorqinside >> 16 & 0xFF, gqi = colorqinside >> 8 & 0xFF, bqi = colorqinside & 0xFF;

            territories.values()
                    .stream()
                    .map(territory -> new TimerEntry(null, territory, territory.distance()))
                    .filter(t -> t.distance() <= NyahConfig.nyahConfigData.getMaximumDistance() * NyahConfig.nyahConfigData.getMaximumDistance()
                            && Models.GuildAttackTimer.getUpcomingTimers()
                            .noneMatch(timer -> timer.territoryName().equals(t.territory().name())))
                    .sorted(Comparator.comparing(TimerEntry::distance))
                    .limit(NyahConfig.nyahConfigData.getMaximumTerritories())
                    .forEach(t -> {
                        render(ctx, rq, gq, bq, rqi, gqi, bqi, player, t.territory());
                    });
        }
    }

    private void render(WorldRenderContext ctx, int r, int g, int b, int ri, int gi, int bi, LocalPlayer player, Territory t) {
        var min = t.leftCorner();
        var max = t.rightCorner();

        boolean inside = player != null &&
                player.getX() >= min.getX() && player.getX() - 1 <= max.getX() &&
                player.getZ() >= min.getZ() && player.getZ() - 1 <= max.getZ();
        int cr = inside ? ri : r;
        int cg = inside ? gi : g;
        int cb = inside ? bi : b;
        Render3D.renderBox(
                ctx,
                min.atY(0),
                max.atY(512),
                cr, cg, cb
        );
    }

}
