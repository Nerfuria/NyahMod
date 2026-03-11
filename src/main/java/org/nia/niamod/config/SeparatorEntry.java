package org.nia.niamod.config;

import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class SeparatorEntry extends TooltipListEntry<Text> {
    private final int separatorLength;
    private final Text name;

    public SeparatorEntry(Text fieldName, @Nullable Supplier<Optional<Text[]>> tooltipSupplier) {
        super(fieldName, tooltipSupplier);
        separatorLength = MinecraftClient.getInstance().textRenderer.getWidth(fieldName);
        this.name = fieldName;
    }

    @Override
    public Optional<Text> getDefaultValue() {
        return Optional.empty();
    }

    @Override
    public Text getValue() {
        return null;
    }

    @Override
    public List<? extends Selectable> narratables() {
        return List.of();
    }

    @Override
    public List<? extends Element> children() {
        return List.of();
    }

    @Override
    public void render(DrawContext graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        graphics.drawText(
                MinecraftClient.getInstance().textRenderer,
                name,
                x + (entryWidth - separatorLength) / 2,
                y + 6,
                getPreferredTextColor(),
                true
        );
    }
}
