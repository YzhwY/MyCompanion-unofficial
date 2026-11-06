package dev.codex.mycompanion;

import java.util.EnumSet;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.server.network.ServerPlayerEntity;

/** Intentionally gets in the owner's way when they look down. */
public final class CrowdOwnerFeetGoal extends Goal {
    private final CompanionFoxEntity fox;
    private ServerPlayerEntity owner;
    private int repathTicks;

    public CrowdOwnerFeetGoal(CompanionFoxEntity fox) {
        this.fox = fox;
        setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    private boolean available(float pitch) {
        return fox.canPlayWith(owner) && fox.isBehaviorEnabled(0) && owner.getPitch() >= pitch;
    }

    @Override
    public boolean canStart() {
        owner = fox.getOwner();
        return available(60.0F);
    }

    @Override
    public boolean shouldContinue() { return available(50.0F); }

    @Override
    public void start() { repathTicks = 0; fox.setSitting(false); }

    @Override
    public void stop() { fox.getNavigation().stop(); owner = null; }

    @Override
    public void tick() {
        fox.getLookControl().lookAt(owner, 20.0F, fox.getMaxLookPitchChange());
        if (fox.squaredDistanceTo(owner) < 0.36) {
            fox.getNavigation().stop();
        } else if (--repathTicks <= 0) {
            repathTicks = getTickCount(6);
            fox.getNavigation().startMovingAlong(fox.getNavigation().findPathTo(owner.getBlockPos(), 0), 1.5);
        }
    }
}
