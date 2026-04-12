package org.nia.niamod.features;

import com.wynntils.core.components.Models;
import com.wynntils.models.territories.TerritoryAttackTimer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.core.BlockPos;
import org.lwjgl.glfw.GLFW;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.managers.KeybindManager;
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
    }

    @Safe
    public void render(WorldRenderContext ctx) {
        int color = NyahConfig.nyahConfigData.getColor();
        int r = color >> 16 & 0xFF, g = color >> 8 & 0xFF, b = color & 0xFF;
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
                .forEach(t -> Render3D.renderBox(
                        ctx,
                        t.leftCorner().atY(0),
                        t.rightCorner().atY(512),
                        r, g, b
                ));
        if (showAll) {
            int colorq = NyahConfig.nyahConfigData.getNotQColor();
            int rq = colorq >> 16 & 0xFF, gq = colorq >> 8 & 0xFF, bq = colorq & 0xFF;
            territories.values()
                    .stream()
                    .map(territory -> new TimerEntry(null, territory, territory.distance()))
                    .filter(t -> t.distance() <= NyahConfig.nyahConfigData.getMaximumDistance() * NyahConfig.nyahConfigData.getMaximumDistance())
                    .sorted(Comparator.comparing(TimerEntry::distance))
                    .limit(NyahConfig.nyahConfigData.getMaximumTerritories())
                    .forEach(t -> Render3D.renderBox(
                            ctx,
                            t.territory().leftCorner().atY(0),
                            t.territory().rightCorner().atY(512),
                            rq, gq, bq
                    ));
        }
    }

}
