package dev.codex.mycompanion.client;

import dev.codex.mycompanion.CompanionControlMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class CompanionControlScreen extends AbstractContainerScreen<CompanionControlMenu> {
    private Button feetButton;
    private Button trapsButton;

    public CompanionControlScreen(CompanionControlMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 304, 200);
    }

    private Component label(String key, int option) {
        return Component.translatable(key, Component.translatable(menu.enabled(option)
                ? "screen.my_companion.on" : "screen.my_companion.off"));
    }

    private void toggle(int option) {
        if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, option);
    }

    @Override
    protected void init() {
        super.init();
        feetButton = addRenderableWidget(Button.builder(label("screen.my_companion.feet", 0), button -> toggle(0))
                .bounds(leftPos + 16, topPos + 48, 272, 20).build());
        trapsButton = addRenderableWidget(Button.builder(label("screen.my_companion.traps", 1), button -> toggle(1))
                .bounds(leftPos + 16, topPos + 94, 272, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.my_companion.done"), button -> onClose())
                .bounds(leftPos + 92, topPos + 166, 120, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        feetButton.setMessage(label("screen.my_companion.feet", 0));
        trapsButton.setMessage(label("screen.my_companion.traps", 1));
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractBackground(graphics, mouseX, mouseY, delta);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF26312D);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFF18221E);
        graphics.fill(leftPos + 16, topPos + 36, leftPos + 288, topPos + 37, 0xFFB98B55);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, 16, 12, 0xFFF2D5A8, false);
        graphics.text(font, Component.translatable("screen.my_companion.subtitle"), 16, 26, 0xFFADBDB3, false);
        graphics.text(font, Component.translatable("screen.my_companion.feet_hint"), 16, 74, 0xFFD3DDD6, false);
        graphics.text(font, Component.translatable("screen.my_companion.traps_hint"), 16, 120, 0xFFD3DDD6, false);
        graphics.text(font, Component.translatable("screen.my_companion.saved"), 16, 143, 0xFFADBDB3, false);
    }
}
