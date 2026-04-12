package org.nia.niamod.models.events;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.nia.niamod.eventbus.Cancelable;
import org.nia.niamod.eventbus.EventInfo;
import org.nia.niamod.eventbus.Preference;

@EventInfo(preference = Preference.CALLER)
public record HeldItemBobbingEvent(Minecraft minecraft, ItemInHandRenderer itemInHandRenderer, float tickProgress,
                                   boolean sleeping, PoseStack matrixStack) implements Cancelable {
}
