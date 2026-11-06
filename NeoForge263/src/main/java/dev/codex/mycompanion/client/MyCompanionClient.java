package dev.codex.mycompanion.client;

import dev.codex.mycompanion.MyCompanionMod;
import net.minecraft.client.renderer.entity.FoxRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@Mod(value = MyCompanionMod.MOD_ID, dist = Dist.CLIENT)
public final class MyCompanionClient {
    public MyCompanionClient(IEventBus modEventBus) {
        modEventBus.addListener(MyCompanionClient::registerRenderers);
        modEventBus.addListener(MyCompanionClient::registerScreens);
    }

    private static void registerScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(MyCompanionMod.CONTROL_PANEL.get(), CompanionControlScreen::new);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MyCompanionMod.COMPANION_FOX.get(), FoxRenderer::new);
    }
}
