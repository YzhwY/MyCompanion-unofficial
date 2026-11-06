package dev.codex.mycompanion;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

public final class CompanionControlScreen extends HandledScreen<CompanionControlHandler> {
    private ButtonWidget feetButton;
    private ButtonWidget trapsButton;

    public CompanionControlScreen(CompanionControlHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        backgroundWidth = 304;
        backgroundHeight = 200;
    }

    private Text label(String key, int option) {
        return Text.translatable(key, Text.translatable(handler.enabled(option)
                ? "screen.my_companion.on" : "screen.my_companion.off"));
    }

    private void toggle(int option) {
        if (client != null && client.interactionManager != null) client.interactionManager.clickButton(handler.syncId, option);
    }

    @Override
    protected void init() {
        super.init();
        feetButton = addDrawableChild(ButtonWidget.builder(label("screen.my_companion.feet", 0), button -> toggle(0))
                .dimensions(x + 16, y + 48, 272, 20).build());
        trapsButton = addDrawableChild(ButtonWidget.builder(label("screen.my_companion.traps", 1), button -> toggle(1))
                .dimensions(x + 16, y + 94, 272, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("screen.my_companion.done"), button -> close())
                .dimensions(x + 92, y + 166, 120, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        feetButton.setMessage(label("screen.my_companion.feet", 0));
        trapsButton.setMessage(label("screen.my_companion.traps", 1));
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.fill(x, y, x + backgroundWidth, y + backgroundHeight, 0xFF26312D);
        context.fill(x + 1, y + 1, x + backgroundWidth - 1, y + backgroundHeight - 1, 0xFF18221E);
        context.fill(x + 16, y + 36, x + 288, y + 37, 0xFFB98B55);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(textRenderer, title, 16, 12, 0xFFF2D5A8, false);
        context.drawText(textRenderer, Text.translatable("screen.my_companion.subtitle"), 16, 26, 0xFFADBDB3, false);
        context.drawText(textRenderer, Text.translatable("screen.my_companion.feet_hint"), 16, 74, 0xFFD3DDD6, false);
        context.drawText(textRenderer, Text.translatable("screen.my_companion.traps_hint"), 16, 120, 0xFFD3DDD6, false);
        context.drawText(textRenderer, Text.translatable("screen.my_companion.saved"), 16, 143, 0xFFADBDB3, false);
    }
}
