package dev.codex.mycompanion;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.server.level.ServerPlayer;

/** Intentionally gets in the owner's way when they look down. */
public final class CrowdOwnerFeetGoal extends Goal {
    private final CompanionFoxEntity fox;
    private ServerPlayer owner;
    private int repathTicks;

    public CrowdOwnerFeetGoal(CompanionFoxEntity fox) {
        this.fox = fox;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean available(float pitch) {
        return fox.canPlayWith(owner) && fox.isBehaviorEnabled(0) && owner.getXRot() >= pitch;
    }

    @Override
    public boolean canUse() {
        owner = fox.getOwner();
        return available(60.0F);
    }

    @Override
    public boolean canContinueToUse() { return available(50.0F); }

    @Override
    public void start() { repathTicks = 0; fox.setSitting(false); }

    @Override
    public void stop() { fox.getNavigation().stop(); owner = null; }

    @Override
    public void tick() {
        fox.getLookControl().setLookAt(owner, 20.0F, fox.getMaxHeadXRot());
        if (fox.distanceToSqr(owner) < 0.36) {
            fox.getNavigation().stop();
        } else if (--repathTicks <= 0) {
            repathTicks = adjustedTickDelay(6);
            fox.getNavigation().moveTo(fox.getNavigation().createPath(owner.blockPosition(), 0), 1.5);
        }
    }
}
