package org.nia.niamod.mixin.wynntils;

import com.wynntils.core.consumers.functions.Function;
import com.wynntils.core.consumers.functions.FunctionManager;
import org.nia.niamod.managers.Features;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FunctionManager.class)
public abstract class FunctionManagerMixin {
    @Invoker("registerFunction")
    protected abstract void invokeRegisterFunction(Function<?> function);

    @Inject(method = "registerAllFunctions", at = @At("TAIL"))
    private void niamod$afterRegisterAllFunctions(CallbackInfo ci) {
        invokeRegisterFunction(Features.getResTickFeature().ResTickFunction);
    }
}