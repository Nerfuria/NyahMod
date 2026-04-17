package org.nia.niamod.managers;

import lombok.experimental.UtilityClass;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import org.nia.niamod.models.gui.OverlayManagerScreen;
import org.nia.niamod.models.gui.render.TextOverlay;

import java.util.ArrayList;
import java.util.List;

import static org.nia.niamod.NiamodClient.mc;

@UtilityClass
public class OverlayManager {

    private final List<TextOverlay> textOverlays = new ArrayList<>();

    public static void init() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("niamod", "overlays"),
                OverlayManager::onHudRender);
    }

    public static void openConfig() {
        mc.setScreen(new OverlayManagerScreen(Minecraft.getInstance().screen, textOverlays));
    }

    public static void registerOverlay(TextOverlay overlay) {
        textOverlays.add(overlay);
    }

    private static void onHudRender(GuiGraphics drawContext, DeltaTracker tickCounter) {
        if (Minecraft.getInstance().screen instanceof OverlayManagerScreen) {
            return;
        }
        int centreX = Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2;
        int centreY = Minecraft.getInstance().getWindow().getGuiScaledHeight() / 2;
        textOverlays.forEach(overlay -> {
            if (!overlay.isEnabled()) return;
            drawContext.pose().pushMatrix();
            drawContext.pose().translate(centreX, centreY);
            drawContext.pose().translate(overlay.getXOffset(), overlay.getYOffset());
            drawContext.pose().scale(overlay.getScale(), overlay.getScale());
            overlay.onHudRender(drawContext, tickCounter);
            drawContext.pose().popMatrix();
        });
    }
}
