package org.nia.niamod.models.events;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.nia.niamod.eventbus.EventInfo;
import org.nia.niamod.eventbus.Preference;

@EventInfo(preference = Preference.CALLER)
public record SlotRenderEvent(GuiGraphics context, ItemStack stack, int slotX, int slotY) {
}
