package org.nia.niamod.features;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import org.joml.Matrix4f;
import org.nia.niamod.eventbus.NiaEventBus;
import org.nia.niamod.eventbus.Subscribe;
import org.nia.niamod.models.events.HeldItemBobbingEvent;
import org.nia.niamod.models.events.HeldItemRenderEvent;
import org.nia.niamod.models.misc.Feature;
import org.nia.niamod.models.misc.Safe;

import static org.nia.niamod.config.NyahConfig.nyahConfigData;

public class ViewModelTransformationFeature extends Feature {
    @Override
    public void init() {
        NiaEventBus.subscribe(this);
    }

    @Subscribe
    public void modifyRender(HeldItemRenderEvent event) {
        if (event.hand() == InteractionHand.MAIN_HAND) {
            event.matrix().mulPose(new Matrix4f()
                    .translate(nyahConfigData.getXOffset() / 100f, nyahConfigData.getYOffset() / 100f, nyahConfigData.getZOffset() / 100f)
                    .rotateX((float) Math.toRadians(nyahConfigData.getXRotation()))
                    .rotateY((float) Math.toRadians(nyahConfigData.getYRotation()))
                    .rotateZ((float) Math.toRadians(nyahConfigData.getZRotation()))
                    .scale(nyahConfigData.getItemScale()));
        }
    }

    @Subscribe
    @Safe
    public void onHeldItemBobbing(HeldItemBobbingEvent event) {
        if (!nyahConfigData.isDisableHeldBobbing()) return;

        Minecraft mc = event.minecraft();
        if (mc == null || mc.player == null || mc.gameMode == null) return;
        if (!mc.options.bobView().get()) return;
        if (!mc.options.getCameraType().isFirstPerson()) return;
        if (event.sleeping()) return;
        if (mc.options.hideGui) return;
        if (mc.gameMode.getPlayerMode() == GameType.SPECTATOR) return;

        int light = mc.getEntityRenderDispatcher()
                .getPackedLightCoords(mc.player, event.tickProgress());

        event.itemInHandRenderer().renderHandsWithItems(
                event.tickProgress(),
                event.matrixStack(),
                mc.gameRenderer.getSubmitNodeStorage(),
                mc.player,
                light
        );

        event.cancel();
    }

}
