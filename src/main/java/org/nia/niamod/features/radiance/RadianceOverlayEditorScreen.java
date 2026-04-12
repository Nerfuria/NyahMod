package org.nia.niamod.features.radiance;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.nia.niamod.config.NyahConfig;

import java.util.Locale;

public final class RadianceOverlayEditorScreen extends Screen {
    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_SCALE = 3.0f;
    private static final int MIN_OFFSET = -1000;
    private static final int MAX_OFFSET = 1000;
    private static final float SCALE_STEP = 0.05f;

    private final Screen parent;
    private final RadianceOverlaySync module;

    private final int originalOffsetX;
    private final int originalOffsetY;
    private final float originalScale;

    private boolean dragging;
    private boolean closeHandled;
    private double dragOffsetX;
    private double dragOffsetY;

    public RadianceOverlayEditorScreen(Screen parent, RadianceOverlaySync module) {
        super(Component.literal("Radiance Overlay Editor"));
        this.parent = parent;
        this.module = module;
        this.originalOffsetX = NyahConfig.nyahConfigData.getRadianceSyncOverlayOffsetX();
        this.originalOffsetY = NyahConfig.nyahConfigData.getRadianceSyncOverlayOffsetY();
        this.originalScale = NyahConfig.nyahConfigData.getRadianceSyncOverlayScale();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    protected void init() {
        int buttonWidth = 98;
        int buttonY = this.height - 26;
        int centerX = this.width / 2;

        addRenderableWidget(Button.builder(Component.literal("-"), btn -> adjustScale(-SCALE_STEP))
                .pos(centerX - 154, buttonY)
                .size(24, 20)
                .build());

        addRenderableWidget(Button.builder(Component.literal("+"), btn -> adjustScale(SCALE_STEP))
                .pos(centerX - 126, buttonY)
                .size(24, 20)
                .build());

        addRenderableWidget(Button.builder(Component.literal("Done"), btn -> {
            NyahConfig.nyahConfigData.setRadianceSyncOverlayScale(clamp(NyahConfig.nyahConfigData.getRadianceSyncOverlayScale(), MIN_SCALE, MAX_SCALE));
            NyahConfig.nyahConfigData.setRadianceSyncOverlayOffsetX(clamp(NyahConfig.nyahConfigData.getRadianceSyncOverlayOffsetX(), MIN_OFFSET, MAX_OFFSET));
            NyahConfig.nyahConfigData.setRadianceSyncOverlayOffsetY(clamp(NyahConfig.nyahConfigData.getRadianceSyncOverlayOffsetY(), MIN_OFFSET, MAX_OFFSET));
            NyahConfig.save();
            closeHandled = true;
            onClose();
        }).pos(centerX - 98, buttonY).size(buttonWidth, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Cancel"), btn -> {
            restoreOriginalValues();
            closeHandled = true;
            onClose();
        }).pos(centerX + 2, buttonY).size(buttonWidth, 20).build());
    }

    @Override
    public void onClose() {
        if (!closeHandled) {
            restoreOriginalValues();
        }
        dragging = false;
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xAA000000);
        renderPreview(context);

        String hintA = "Left-click and drag the preview to move it.";
        String hintB = "Scroll wheel or +/- buttons to resize.";
        String hintC = "Scale: " + String.format(Locale.ROOT, "%.2fx", NyahConfig.nyahConfigData.getRadianceSyncOverlayScale());

        context.drawString(this.font, hintA,
                (this.width - this.font.width(hintA)) / 2, 8, 0xFFE6E6E6, true);
        context.drawString(this.font, hintB,
                (this.width - this.font.width(hintB)) / 2, 20, 0xFFE6E6E6, true);
        context.drawString(this.font, hintC,
                (this.width - this.font.width(hintC)) / 2, this.height - 38, 0xFFFFFF55, true);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubleClick) {
        if (super.mouseClicked(click, doubleClick)) {
            return true;
        }
        if (click.input() != 0) {
            return false;
        }
        double mouseX = click.x();
        double mouseY = click.y();
        PreviewBounds bounds = getPreviewBounds();
        if (!bounds.contains(mouseX, mouseY)) {
            return false;
        }
        dragging = true;
        dragOffsetX = mouseX - getAnchorScreenX();
        dragOffsetY = mouseY - getAnchorScreenY();
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
        if (!dragging || click.input() != 0) {
            return super.mouseDragged(click, deltaX, deltaY);
        }
        double mouseX = click.x();
        double mouseY = click.y();
        double targetAnchorX = mouseX - dragOffsetX;
        double targetAnchorY = mouseY - dragOffsetY;

        int offsetX = (int) Math.round(targetAnchorX - (this.width / 2.0));
        int offsetY = (int) Math.round((this.height * 2.0 / 3.0) - targetAnchorY);

        NyahConfig.nyahConfigData.setRadianceSyncOverlayOffsetX(clamp(offsetX, MIN_OFFSET, MAX_OFFSET));
        NyahConfig.nyahConfigData.setRadianceSyncOverlayOffsetY(clamp(offsetY, MIN_OFFSET, MAX_OFFSET));
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (click.input() == 0) {
            dragging = false;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        PreviewBounds bounds = getPreviewBounds();
        if (bounds.contains(mouseX, mouseY) && verticalAmount != 0.0) {
            adjustScale((float) (verticalAmount * SCALE_STEP));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void renderPreview(GuiGraphics context) {
        PreviewBounds bounds = getPreviewBounds();
        drawOutline(context, bounds, 0xFFFFFFFF);

        float scale = clamp(NyahConfig.nyahConfigData.getRadianceSyncOverlayScale(), MIN_SCALE, MAX_SCALE);
        int baseX = (int) ((this.width / 2.0f) / scale);
        baseX += Math.round(NyahConfig.nyahConfigData.getRadianceSyncOverlayOffsetX() / scale);
        int baseY = (int) ((this.height * 2.0f / 3.0f) / scale);
        baseY -= Math.round(NyahConfig.nyahConfigData.getRadianceSyncOverlayOffsetY() / scale);

        String primary = isStatusMode() ? "RADIANCE: 8.7s" : "RADIANCE IN 3.0s";
        String cooldown = "COOLDOWN: 12s";

        context.pose().pushMatrix();
        context.pose().scale(scale, scale);
        drawCenteredLine(context, primary, baseX, baseY, 0xFFFFFF55);
        drawCenteredLine(context, cooldown, baseX, baseY + 12, 0xFF55FFFF);
        context.pose().popMatrix();
    }

    private void drawCenteredLine(GuiGraphics context, String text, int centerX, int y, int color) {
        int width = this.font.width(text);
        context.drawString(this.font, text, centerX - width / 2, y, color, true);
    }

    private PreviewBounds getPreviewBounds() {
        float scale = clamp(NyahConfig.nyahConfigData.getRadianceSyncOverlayScale(), MIN_SCALE, MAX_SCALE);
        String primary = isStatusMode() ? "RADIANCE: 8.7s" : "RADIANCE IN 3.0s";
        String cooldown = "COOLDOWN: 12s";
        int maxTextWidth = Math.max(this.font.width(primary), this.font.width(cooldown));

        double halfWidth = (maxTextWidth * scale) / 2.0;
        double totalHeight = (12 + this.font.lineHeight) * scale;

        int anchorX = getAnchorScreenX();
        int anchorY = getAnchorScreenY();

        int left = (int) Math.floor(anchorX - halfWidth) - 6;
        int top = anchorY - 4;
        int right = (int) Math.ceil(anchorX + halfWidth) + 6;
        int bottom = (int) Math.ceil(anchorY + totalHeight) + 4;
        return new PreviewBounds(left, top, right, bottom);
    }

    private int getAnchorScreenX() {
        return (int) Math.round(this.width / 2.0 + NyahConfig.nyahConfigData.getRadianceSyncOverlayOffsetX());
    }

    private int getAnchorScreenY() {
        return (int) Math.round((this.height * 2.0 / 3.0) - NyahConfig.nyahConfigData.getRadianceSyncOverlayOffsetY());
    }

    private boolean isStatusMode() {
        return module.isStatusOverlayMode();
    }

    private void restoreOriginalValues() {
        NyahConfig.nyahConfigData.setRadianceSyncOverlayOffsetX(originalOffsetX);
        NyahConfig.nyahConfigData.setRadianceSyncOverlayOffsetY(originalOffsetY);
        NyahConfig.nyahConfigData.setRadianceSyncOverlayScale(originalScale);
    }

    private void adjustScale(float delta) {
        NyahConfig.nyahConfigData.setRadianceSyncOverlayScale(clamp(
                NyahConfig.nyahConfigData.getRadianceSyncOverlayScale() + delta, MIN_SCALE, MAX_SCALE));
    }

    private void drawOutline(GuiGraphics context, PreviewBounds bounds, int color) {
        context.fill(bounds.left, bounds.top, bounds.right, bounds.top + 1, color);
        context.fill(bounds.left, bounds.bottom - 1, bounds.right, bounds.bottom, color);
        context.fill(bounds.left, bounds.top, bounds.left + 1, bounds.bottom, color);
        context.fill(bounds.right - 1, bounds.top, bounds.right, bounds.bottom, color);
    }

    private record PreviewBounds(int left, int top, int right, int bottom) {

        private boolean contains(double x, double y) {
                return x >= left && x <= right && y >= top && y <= bottom;
            }
        }
}
