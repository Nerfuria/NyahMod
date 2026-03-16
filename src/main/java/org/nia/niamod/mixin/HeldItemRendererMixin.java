package org.nia.niamod.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.nia.niamod.config.NyahConfig.nyahConfigData;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
    @Inject(
            method = "renderFirstPersonItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V"
            )
    )
    public void onRender(AbstractClientPlayerEntity player, float tickProgress, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, OrderedRenderCommandQueue orderedRenderCommandQueue, int light, CallbackInfo ci) {
        if (hand == Hand.MAIN_HAND) {
            matrices.multiplyPositionMatrix(new Matrix4f()
                    .translate(nyahConfigData.xOffset / 100f, nyahConfigData.yOffset / 100f, nyahConfigData.zOffset / 100f)
                    .rotateX((float) Math.toRadians(nyahConfigData.xRotation))
                    .rotateY((float) Math.toRadians(nyahConfigData.yRotation))
                    .rotateZ((float) Math.toRadians(nyahConfigData.zRotation))
                    .scale(nyahConfigData.itemScale));
        }
    }
}
