package dev.codex.mycompanion;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

public final class CompanionState extends SavedData {
    public static final Codec<CompanionState> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, UUIDUtil.CODEC).optionalFieldOf("Owners", Map.of()).forGetter(state -> state.companions),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, CompanionSettings.CODEC).optionalFieldOf("Settings", Map.of()).forGetter(state -> state.preferences)
    ).apply(instance, CompanionState::new));
    public static final SavedDataType<CompanionState> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(MyCompanionMod.MOD_ID, "owners"), CompanionState::new, CODEC);
    private final Map<UUID, UUID> companions;

    private final Map<UUID, CompanionSettings> preferences;
    public CompanionState() { this(Map.of(), Map.of()); }
    private CompanionState(Map<UUID, UUID> entries, Map<UUID, CompanionSettings> preferences) {
        companions = new HashMap<>(entries);
        this.preferences = new HashMap<>(preferences);
    }

    public CompanionSettings settings(UUID owner) { return preferences.getOrDefault(owner, CompanionSettings.DEFAULT); }

    public void toggle(UUID owner, int option) {
        if (option < 0 || option > 1) return;
        preferences.put(owner, settings(owner).toggle(option));
        setDirty();
    }

    public static CompanionState get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public UUID get(UUID owner) { return companions.get(owner); }

    public void put(UUID owner, UUID companion) {
        companions.put(owner, companion);
        setDirty();
    }

    public void restore(UUID owner, UUID previous) {
        if (previous == null) companions.remove(owner);
        else companions.put(owner, previous);
        setDirty();
    }
}
