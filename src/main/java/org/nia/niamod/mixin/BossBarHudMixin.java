package org.nia.niamod.mixin;

import net.minecraft.entity.boss.BossBar;
import net.minecraft.text.Text;
import org.nia.niamod.managers.FeatureManager;
import org.nia.niamod.models.events.BossBarNameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(BossBar.class)
public class BossBarHudMixin {

    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static Text onInit(Text value) {
        return BossBarNameEvent.MODIFY.invoker().modify(value);
    }

    @ModifyVariable(method = "setName", at = @At("HEAD"), argsOnly = true)
    private Text setName(Text value) {
        return BossBarNameEvent.MODIFY.invoker().modify(value);
    }
}