package dev.codex.mycompanion;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class MyCompanionMod implements ModInitializer {
    public static final String MOD_ID = "my_companion";
    public static final EntityType<CompanionFoxEntity> COMPANION_FOX = Registry.register(
            Registries.ENTITY_TYPE, new Identifier(MOD_ID, "companion_fox"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, CompanionFoxEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6F, 0.7F)).trackRangeBlocks(64).build());

    public static final net.minecraft.screen.ScreenHandlerType<CompanionControlHandler> CONTROL_PANEL = Registry.register(
            Registries.SCREEN_HANDLER, new Identifier(MOD_ID, "control_panel"),
            new net.minecraft.screen.ScreenHandlerType<>(CompanionControlHandler::new, net.minecraft.resource.featuretoggle.FeatureFlags.VANILLA_FEATURES));

    @Override
    public void onInitialize() {
        FabricDefaultAttributeRegistry.register(COMPANION_FOX, FoxEntity.createFoxAttributes());
        ServerTickEvents.END_SERVER_TICK.register(CompanionManager::tick);
    }
}
