package org.nia.niamod.models.gui;

import com.google.common.collect.Lists;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;
import org.nia.niamod.models.records.State;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.nia.niamod.NiamodClient.mc;

public class IgnoreEntry extends TooltipListEntry<Text> {

    private static final int BUTTON_WIDTH = 80;

    private final ButtonWidget ignoreWidget;
    private final MultiClickButton favouriteWidget;
    private final List<PressableWidget> widgets;

    private final int emojiWidth;
    private final int startWidth;

    private boolean edited = false;

    public IgnoreEntry(
            Text fieldName,
            @Nullable Supplier<Optional<Text[]>> tooltipSupplier,
            Consumer<ButtonWidget> onIgnore,
            Consumer<ButtonWidget> onFavourite,
            Consumer<ButtonWidget> onAvoid,
            Consumer<ButtonWidget> onReset,
            State state
    ) {
        super(fieldName, tooltipSupplier);

        var renderer = mc.textRenderer;
        emojiWidth = renderer.getWidth("♥");
        startWidth = renderer.getWidth("@@@@@@@@@@@@@@@@");

        ignoreWidget = ButtonWidget.builder(Text.of("§2Ignore"), onIgnore::accept)
                .dimensions(0, 0, BUTTON_WIDTH, 20)
                .build();

        favouriteWidget = MultiClickButton
                .builder(
                        Text.of(state.code + "♥"),
                        markThen(onFavourite),
                        markThen(onAvoid),
                        markThen(onReset),
                        false)
                .tooltip(Tooltip.of(Text.of("Left click to favourite, Right click to avoid, Middle click to reset")))
                .dimensions(0, 0, emojiWidth + 8, 20)
                .build();

        widgets = Lists.newArrayList(ignoreWidget, favouriteWidget);
    }

    private Consumer<ButtonWidget> markThen(Consumer<ButtonWidget> action) {
        return button -> {
            edited = true;
            action.accept(button);
        };
    }

    @Override
    public boolean isEdited() {
        return edited;
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
    public Iterator<String> getSearchTags() {
        return Collections.singleton(getFieldName().toString()).iterator();
    }


    @Override
    public List<? extends Selectable> narratables() {
        return widgets;
    }

    @Override
    public List<? extends Element> children() {
        return widgets;
    }

    @Override
    public void render(DrawContext graphics, int index, int y, int x,
                       int entryWidth, int entryHeight,
                       int mouseX, int mouseY, boolean isHovered, float delta) {
        super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);

        var renderer = mc.textRenderer;
        graphics.drawText(renderer, getDisplayedFieldName(), x, y + 6, getPreferredTextColor(), true);

        ignoreWidget.setPosition(x + startWidth + 4, y);
        ignoreWidget.setWidth(BUTTON_WIDTH);
        ignoreWidget.setFocused(ignoreWidget.isMouseOver(mouseX, mouseY));
        favouriteWidget.setPosition(x - emojiWidth - 10, y);

        favouriteWidget.render(graphics, mouseX, mouseY, delta);
        ignoreWidget.render(graphics, mouseX, mouseY, delta);
    }
}