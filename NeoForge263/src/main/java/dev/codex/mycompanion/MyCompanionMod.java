package dev.codex.mycompanion;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.fox.Fox;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(MyCompanionMod.MOD_ID)
public final class MyCompanionMod {
    public static final String MOD_ID = "my_companion";
    private static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(MOD_ID);
    public static final DeferredHolder<EntityType<?>, EntityType<CompanionFoxEntity>> COMPANION_FOX =
            ENTITIES.registerEntityType("companion_fox", CompanionFoxEntity::new, MobCategory.CREATURE,
                    builder -> builder.sized(0.6F, 0.7F).clientTrackingRange(4));

    private static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS = DeferredRegister.create(
            net.minecraft.core.registries.Registries.MENU, MOD_ID);
    public static final DeferredHolder<net.minecraft.world.inventory.MenuType<?>, net.minecraft.world.inventory.MenuType<CompanionControlMenu>> CONTROL_PANEL =
            MENUS.register("control_panel", () -> new net.minecraft.world.inventory.MenuType<>(CompanionControlMenu::new,
                    net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    public MyCompanionMod(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
        modEventBus.addListener(MyCompanionMod::registerAttributes);
        NeoForge.EVENT_BUS.addListener(MyCompanionMod::onServerTick);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(COMPANION_FOX.get(), Fox.createAttributes().build());
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        CompanionManager.tick(event.getServer());
    }
}
