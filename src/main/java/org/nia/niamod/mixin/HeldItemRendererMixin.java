package org.nia.niamod.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.nia.niamod.config.NyahConfig.nyahConfigData;

@Mixin(ItemInHandRenderer.class)
public class HeldItemRendererMixin {
    @Inject(
            method = "renderArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"
            )
    )
    public void onRender(AbstractClientPlayer player, float tickProgress, float pitch, InteractionHand hand, float swingProgress, ItemStack item, float equipProgress, PoseStack matrices, SubmitNodeCollector orderedRenderCommandQueue, int light, CallbackInfo ci) {
        if (hand == InteractionHand.MAIN_HAND) {
            matrices.mulPose(new Matrix4f()
                    .translate(nyahConfigData.getXOffset() / 100f, nyahConfigData.getYOffset() / 100f, nyahConfigData.getZOffset() / 100f)
                    .rotateX((float) Math.toRadians(nyahConfigData.getXRotation()))
                    .rotateY((float) Math.toRadians(nyahConfigData.getYRotation()))
                    .rotateZ((float) Math.toRadians(nyahConfigData.getZRotation()))
                    .scale(nyahConfigData.getItemScale()));
        }
    }
}
