package org.nia.niamod.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.world.GameMode;
import org.joml.Matrix4f;
import org.nia.niamod.config.NyahConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Final
    @Shadow
    public HeldItemRenderer firstPersonRenderer;
    @Final
    @Shadow
    private MinecraftClient client;

    @Inject(
            method = "renderHand",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/GameRenderer;bobView(Lnet/minecraft/client/util/math/MatrixStack;F)V",
                    shift = At.Shift.BEFORE
            )
    )
    private void onBeforeBobView(
            float tickProgress, boolean sleeping, Matrix4f positionMatrix,
            CallbackInfo ci,
            @Local MatrixStack matrixStack
    ) {
        if (!NyahConfig.nyahConfigData.disableHeldBobbing) return;

        if (!this.client.options.getPerspective().isFirstPerson()
                || sleeping
                || this.client.options.hudHidden
                || this.client.interactionManager.getCurrentGameMode() == GameMode.SPECTATOR) {
            return;
        }

        int light = this.client.getEntityRenderDispatcher()
                .getLight(this.client.player, tickProgress);

        this.firstPersonRenderer.renderItem(
                tickProgress,
                matrixStack,
                this.client.gameRenderer.getEntityRenderCommandQueue(),
                this.client.player,
                light
        );
    }

    @Redirect(
            method = "renderHand",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V"
            )
    )
    private void cancel(
            HeldItemRenderer instance, float tickProgress, MatrixStack matrices, OrderedRenderCommandQueue orderedRenderCommandQueue, ClientPlayerEntity player, int light
    ) {
        if (!NyahConfig.nyahConfigData.disableHeldBobbing) {
            instance.renderItem(tickProgress, matrices, orderedRenderCommandQueue, player, light);
        }
    }
}