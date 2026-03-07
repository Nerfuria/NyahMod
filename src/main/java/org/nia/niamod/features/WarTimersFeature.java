package org.nia.niamod.features;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.wynntils.core.components.Models;
import com.wynntils.models.territories.TerritoryAttackTimer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.util.math.BlockPos;
import org.nia.niamod.NiamodClient;
import org.nia.niamod.config.NyahConfig;
import org.nia.niamod.render.BoxRenderer;
import org.nia.niamod.util.WebUtils;

import java.lang.reflect.Type;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class WarTimersFeature {

    private final HashMap<String, Territory> territories = new HashMap<>();

    public void init() {
        Type type = new TypeToken<Map<String, TerritoryResponse>>() {
        }.getType();
        Map<String, TerritoryResponse> data = new Gson().fromJson(WebUtils.queryAPI("https://api.wynncraft.com/v3/guild/list/territory"), type);
        data.forEach((k, v) -> territories.put(k, new Territory(k, new BlockPos(v.location.start[0], -100, v.location.start[1]), new BlockPos(v.location.end[0], 256, v.location.end[1]))));
        WorldRenderEvents.AFTER_ENTITIES.register(this::render);
    }

    public void render(WorldRenderContext ctx) {
        int r = NyahConfig.nyahConfigData.color >> 16 & 0xFF, g = NyahConfig.nyahConfigData.color >> 8 & 0x55, b = NyahConfig.nyahConfigData.color & 0x55;
        Models.GuildAttackTimer.getUpcomingTimers()
                .map(timer -> new Object[]{
                        timer,
                        territories.get(timer.territoryName()),
                        territories.get(timer.territoryName()).distance()
                })
                .filter(o -> (int) o[2] <= NyahConfig.nyahConfigData.maximumDistance * NyahConfig.nyahConfigData.maximumDistance)
                .sorted(Comparator
                        .comparing(o -> ((TerritoryAttackTimer) ((Object[]) o)[0]).timerEnd())
                        .thenComparing(o -> (int) ((Object[]) o)[2]))
                .limit(NyahConfig.nyahConfigData.maximumTerritories)
                .map(o -> (Territory) o[1])
                .forEach(t -> BoxRenderer.renderBox(ctx, t.leftCorner.withY(0), t.rightCorner.withY(512), r, g, b));
    }

    private static class Territory {
        private final BlockPos middle;
        public String name;
        public BlockPos leftCorner;
        public BlockPos rightCorner;

        public Territory(String name, BlockPos leftCorner, BlockPos rightCorner) {
            this.name = name;
            this.leftCorner = leftCorner;
            this.rightCorner = rightCorner;

            middle = new BlockPos((leftCorner.getX() + rightCorner.getX()) / 2, 0, (leftCorner.getZ() + rightCorner.getZ()) / 2);
        }

        public int distance() {
            return (int) middle.getSquaredDistance(NiamodClient.mc.player.getBlockPos().withY(0));
        }
    }

    private static class TerritoryResponse {
        public Location location;

        public TerritoryResponse(Location location) {
            this.location = location;
        }

        private class Location {
            public int[] start;
            public int[] end;

            public Location(int[] start, int[] end) {
                this.start = start;
                this.end = end;
            }
        }
    }
}
