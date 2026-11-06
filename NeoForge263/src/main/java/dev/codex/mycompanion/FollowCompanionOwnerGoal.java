package dev.codex.mycompanion;

import java.util.EnumSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;

public final class FollowCompanionOwnerGoal extends Goal {
    private final CompanionFoxEntity fox;
    private ServerPlayer owner;
    private int repathTicks;

    public FollowCompanionOwnerGoal(CompanionFoxEntity fox) {
        this.fox = fox;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        owner = fox.getOwner();
        return available() && fox.distanceToSqr(owner) > 16.0;
    }

    private boolean available() {
        return owner != null && owner.isAlive() && !owner.isSpectator()
                && owner.level() == fox.level() && !fox.isPassenger();
    }

    @Override
    public boolean canContinueToUse() { return available() && fox.distanceToSqr(owner) > 4.0; }

    @Override
    public void start() {
        repathTicks = 0;
        fox.setSitting(false);
    }

    @Override
    public void stop() {
        fox.getNavigation().stop();
        owner = null;
    }

    @Override
    public void tick() {
        fox.getLookControl().setLookAt(owner, 10.0F, fox.getMaxHeadXRot());
        if (--repathTicks <= 0) {
            repathTicks = adjustedTickDelay(10);
            fox.getNavigation().moveTo(owner, 1.3);
        }
    }
}
