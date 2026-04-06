package org.nia.niamod.features;

import com.wynntils.core.components.Models;
import com.wynntils.models.territories.TerritoryAttackTimer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.core.BlockPos;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.models.misc.Feature;
import org.nia.niamod.models.misc.Safe;
import org.nia.niamod.models.records.Territory;
import org.nia.niamod.models.records.TimerEntry;
import org.nia.niamod.render.BoxRenderer;
import org.nia.niamod.util.WynncraftAPI;

import java.util.Comparator;
import java.util.HashMap;

public class WarTimersFeature extends Feature {

    private final HashMap<String, Territory> territories = new HashMap<>();

    @Safe
    public void init() {
        WynncraftAPI.territoryResponse().forEach((k, v) -> territories.put(k, new Territory(k, new BlockPos(v.location().start()[0], -100, v.location().start()[1]), new BlockPos(v.location().end()[0], 256, v.location().end()[1]))));
        WorldRenderEvents.AFTER_ENTITIES.register(context ->
                runSafe("render", () -> render(context)));
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
                .forEach(t -> BoxRenderer.renderBox(
                        ctx,
                        t.leftCorner().atY(0),
                        t.rightCorner().atY(512),
                        r, g, b
                ));
    }

}
