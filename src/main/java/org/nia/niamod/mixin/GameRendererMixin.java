package org.nia.niamod.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.level.GameType;
import org.joml.Matrix4f;
import org.nia.niamod.config.NyahConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Final
    @Shadow
    public ItemInHandRenderer itemInHandRenderer;
    @Final
    @Shadow
    private Minecraft minecraft;

    @Inject(
            method = "renderItemInHand",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;bobView(Lcom/mojang/blaze3d/vertex/PoseStack;F)V",
                    shift = At.Shift.BEFORE
            )
    )
    private void onBeforeBobView(
            float tickProgress, boolean sleeping, Matrix4f positionMatrix,
            CallbackInfo ci,
            @Local PoseStack matrixStack
    ) {
        if (!NyahConfig.nyahConfigData.disableHeldBobbing) return;

        if (!this.minecraft.options.getCameraType().isFirstPerson()
                || sleeping
                || this.minecraft.options.hideGui
                || this.minecraft.gameMode.getPlayerMode() == GameType.SPECTATOR) {
            return;
        }

        int light = this.minecraft.getEntityRenderDispatcher()
                .getPackedLightCoords(this.minecraft.player, tickProgress);

        this.itemInHandRenderer.renderHandsWithItems(
                tickProgress,
                matrixStack,
                this.minecraft.gameRenderer.getSubmitNodeStorage(),
                this.minecraft.player,
                light
        );
    }

    @WrapOperation(
            method = "renderItemInHand",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/player/LocalPlayer;I)V"
            )
    )
    private void cancel(
            ItemInHandRenderer instance, float tickProgress, PoseStack matrices, SubmitNodeCollector orderedRenderCommandQueue, LocalPlayer player, int light, Operation<Void> original
    ) {
        if (!NyahConfig.nyahConfigData.disableHeldBobbing || !this.minecraft.options.bobView().get()) {
            original.call(instance, tickProgress, matrices, orderedRenderCommandQueue, player, light);
        }
    }
}