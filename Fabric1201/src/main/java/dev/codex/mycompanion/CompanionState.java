package dev.codex.mycompanion;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

public final class CompanionState extends PersistentState {
    private final Map<UUID, UUID> companions = new HashMap<>();
    private final Map<UUID, CompanionSettings> preferences = new HashMap<>();

    public static CompanionState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(
                CompanionState::read, CompanionState::new, "my_companion_owners");
    }

    static CompanionState read(NbtCompound nbt) {
        CompanionState state = new CompanionState();
        NbtCompound owners = nbt.getCompound("Owners");
        for (String key : owners.getKeys()) {
            try {
                if (owners.containsUuid(key)) state.companions.put(UUID.fromString(key), owners.getUuid(key));
            } catch (IllegalArgumentException ignored) {
                // A malformed entry must not stop the remaining players loading.
            }
        }
        NbtCompound settings = nbt.getCompound("Settings");
        for (String key : settings.getKeys()) {
            try {
                NbtCompound value = settings.getCompound(key);
                state.preferences.put(UUID.fromString(key), new CompanionSettings(
                        !value.contains("CrowdFeet") || value.getBoolean("CrowdFeet"),
                        !value.contains("TriggerTraps") || value.getBoolean("TriggerTraps")));
            } catch (IllegalArgumentException ignored) { }
        }
        return state;
    }

    public CompanionSettings settings(UUID owner) { return preferences.getOrDefault(owner, CompanionSettings.DEFAULT); }

    public void toggle(UUID owner, int option) {
        if (option < 0 || option > 1) return;
        preferences.put(owner, settings(owner).toggle(option));
        markDirty();
    }

    public UUID get(UUID owner) { return companions.get(owner); }

    public void put(UUID owner, UUID companion) {
        companions.put(owner, companion);
        markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound owners = new NbtCompound();
        companions.forEach((owner, companion) -> owners.putUuid(owner.toString(), companion));
        nbt.put("Owners", owners);
        NbtCompound settings = new NbtCompound();
        preferences.forEach((owner, value) -> {
            NbtCompound entry = new NbtCompound();
            entry.putBoolean("CrowdFeet", value.crowdFeet());
            entry.putBoolean("TriggerTraps", value.triggerTraps());
            settings.put(owner.toString(), entry);
        });
        nbt.put("Settings", settings);
        return nbt;
    }
}
