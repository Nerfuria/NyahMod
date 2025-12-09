package net.d0cr.niamod.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.d0cr.niamod.features.Features;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.network.packet.s2c.login.LoginHelloS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLoginNetworkHandler.class)
public class ClientLoginNetworkHandlerMixin {

    @Inject(method = "onHello", at = @At("RETURN"))
    public void onHello(LoginHelloS2CPacket packet, CallbackInfo ci, @Local String string) {
        Features.getWsFeature().init(string);
    }

}
