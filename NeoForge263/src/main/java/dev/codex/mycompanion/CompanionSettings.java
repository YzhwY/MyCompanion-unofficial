package dev.codex.mycompanion;

/** Owner-level preferences survive replacing a missing companion. */
public record CompanionSettings(boolean crowdFeet, boolean triggerTraps) {
    public static final com.mojang.serialization.Codec<CompanionSettings> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(instance -> instance.group(
            com.mojang.serialization.Codec.BOOL.optionalFieldOf("CrowdFeet", true).forGetter(CompanionSettings::crowdFeet),
            com.mojang.serialization.Codec.BOOL.optionalFieldOf("TriggerTraps", true).forGetter(CompanionSettings::triggerTraps)
    ).apply(instance, CompanionSettings::new));
    public static final CompanionSettings DEFAULT = new CompanionSettings(true, true);
    public boolean enabled(int option) { return option == 0 ? crowdFeet : option == 1 && triggerTraps; }
    public CompanionSettings toggle(int option) {
        return option == 0 ? new CompanionSettings(!crowdFeet, triggerTraps)
                : option == 1 ? new CompanionSettings(crowdFeet, !triggerTraps) : this;
    }
}
