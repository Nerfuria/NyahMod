package org.nia.niamod.features;

import com.wynntils.core.components.Models;
import com.wynntils.models.territories.TerritoryAttackTimer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.util.math.BlockPos;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.models.misc.Feature;
import org.nia.niamod.models.records.Territory;
import org.nia.niamod.models.records.TimerEntry;
import org.nia.niamod.render.BoxRenderer;
import org.nia.niamod.util.WynncraftAPI;

import java.util.Comparator;
import java.util.HashMap;

public class WarTimersFeature extends Feature {

    private final HashMap<String, Territory> territories = new HashMap<>();

    protected void init() {
        WynncraftAPI.territoryResponse().forEach((k, v) -> territories.put(k, new Territory(k, new BlockPos(v.location().start()[0], -100, v.location().start()[1]), new BlockPos(v.location().end()[0], 256, v.location().end()[1]))));
        WorldRenderEvents.AFTER_ENTITIES.register((WorldRenderContext ctx) -> runSafe(() -> render(ctx)));
    }

    public void render(WorldRenderContext ctx) {
        int r = NyahConfig.nyahConfigData.color >> 16 & 0xFF, g = NyahConfig.nyahConfigData.color >> 8 & 0xFF, b = NyahConfig.nyahConfigData.color & 0xFF;
        Models.GuildAttackTimer.getUpcomingTimers()
                .map(timer -> {
                    Territory t = territories.get(timer.territoryName());
                    return new TimerEntry(timer, t, t.distance());
                })
                .filter(e -> e.distance() <= NyahConfig.nyahConfigData.maximumDistance * NyahConfig.nyahConfigData.maximumDistance)
                .sorted(Comparator
                        .comparing(TimerEntry::timer, Comparator.comparing(TerritoryAttackTimer::timerEnd))
                        .thenComparingInt(TimerEntry::distance))
                .limit(NyahConfig.nyahConfigData.maximumTerritories)
                .map(TimerEntry::territory)
                .forEach(t -> BoxRenderer.renderBox(
                        ctx,
                        t.leftCorner().withY(0),
                        t.rightCorner().withY(512),
                        r, g, b
                ));
    }

}
