package dev.codex.mycompanion;

/** Owner-level preferences survive replacing a missing companion. */
public record CompanionSettings(boolean crowdFeet, boolean triggerTraps) {
    public static final CompanionSettings DEFAULT = new CompanionSettings(true, true);
    public boolean enabled(int option) { return option == 0 ? crowdFeet : option == 1 && triggerTraps; }
    public CompanionSettings toggle(int option) {
        return option == 0 ? new CompanionSettings(!crowdFeet, triggerTraps)
                : option == 1 ? new CompanionSettings(crowdFeet, !triggerTraps) : this;
    }
}
