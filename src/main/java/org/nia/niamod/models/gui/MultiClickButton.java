package org.nia.niamod.models.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

public class MultiClickButton extends Button {

    private static final int LEFT_CLICK = 0;
    private static final int RIGHT_CLICK = 1;
    private static final int MIDDLE_CLICK = 2;

    private final Consumer<Button> onLeft;
    private final Consumer<Button> onRight;
    private final Consumer<Button> onMiddle;
    private final boolean background;

    protected MultiClickButton(int x, int y, int width, int height, net.minecraft.network.chat.Component text,
                               CreateNarration narration,
                               Consumer<Button> onLeft,
                               Consumer<Button> onRight,
                               Consumer<Button> onMiddle,
                               boolean background) {
        super(x, y, width, height, text, null, narration);
        this.onLeft = onLeft;
        this.onRight = onRight;
        this.onMiddle = onMiddle;
        this.background = background;
    }

    public static Builder builder(net.minecraft.network.chat.Component message,
                                  Consumer<Button> onLeft,
                                  Consumer<Button> onRight,
                                  Consumer<Button> onMiddle,
                                  boolean background) {
        return new Builder(message, onLeft, onRight, onMiddle, background);
    }

    @Override
    protected void renderContents(GuiGraphics context, int mouseX, int mouseY, float deltaTicks) {
        if (background) renderDefaultSprite(context);
        renderDefaultLabel(context.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE));
    }

    @Override
    public void onClick(MouseButtonEvent click, boolean doubled) {
        switch (click.input()) {
            case LEFT_CLICK -> onLeft.accept(this);
            case RIGHT_CLICK -> onRight.accept(this);
            case MIDDLE_CLICK -> onMiddle.accept(this);
        }
    }

    @Override
    protected boolean isValidClickButton(MouseButtonInfo input) {
        int k = input.input();
        return k == LEFT_CLICK || k == RIGHT_CLICK || k == MIDDLE_CLICK;
    }

    @Environment(EnvType.CLIENT)
    public static class Builder {
        private final net.minecraft.network.chat.Component message;
        private final Consumer<Button> onLeft;
        private final Consumer<Button> onRight;
        private final Consumer<Button> onMiddle;
        private CreateNarration narration = DEFAULT_NARRATION;
        private @Nullable Tooltip tooltip;
        private int x, y;
        private int width = 150;
        private int height = 20;
        private boolean background;

        public Builder(net.minecraft.network.chat.Component message,
                       Consumer<Button> onLeft,
                       Consumer<Button> onRight,
                       Consumer<Button> onMiddle,
                       boolean background) {
            this.message = message;
            this.onLeft = onLeft;
            this.onRight = onRight;
            this.onMiddle = onMiddle;
            this.background = background;
        }

        public Builder position(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public Builder width(int width) {
            this.width = width;
            return this;
        }

        public Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder dimensions(int x, int y, int w, int h) {
            return position(x, y).size(w, h);
        }

        public Builder background(boolean background) {
            this.background = background;
            return this;
        }

        public Builder tooltip(@Nullable Tooltip tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        public Builder narration(CreateNarration narration) {
            this.narration = narration;
            return this;
        }

        public MultiClickButton build() {
            MultiClickButton button = new MultiClickButton(
                    x, y, width, height, message, narration, onLeft, onRight, onMiddle, background);
            button.setTooltip(tooltip);
            return button;
        }
    }
}