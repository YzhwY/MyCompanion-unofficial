package dev.codex.mycompanion;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.FoxEntityRenderer;

public final class MyCompanionClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        net.minecraft.client.gui.screen.ingame.HandledScreens.register(MyCompanionMod.CONTROL_PANEL, CompanionControlScreen::new);
        EntityRendererRegistry.register(MyCompanionMod.COMPANION_FOX, FoxEntityRenderer::new);
    }
}
