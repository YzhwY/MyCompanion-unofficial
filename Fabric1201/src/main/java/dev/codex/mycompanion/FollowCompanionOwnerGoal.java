package dev.codex.mycompanion;

import java.util.EnumSet;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.server.network.ServerPlayerEntity;

public final class FollowCompanionOwnerGoal extends Goal {
    private final CompanionFoxEntity fox;
    private ServerPlayerEntity owner;
    private int repathTicks;

    public FollowCompanionOwnerGoal(CompanionFoxEntity fox) {
        this.fox = fox;
        setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        owner = fox.getOwner();
        return available() && fox.squaredDistanceTo(owner) > 16.0;
    }

    private boolean available() {
        return owner != null && owner.isAlive() && !owner.isSpectator()
                && owner.getWorld() == fox.getWorld() && !fox.hasVehicle();
    }

    @Override
    public boolean shouldContinue() {
        return available() && fox.squaredDistanceTo(owner) > 4.0;
    }

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
        fox.getLookControl().lookAt(owner, 10.0F, fox.getMaxLookPitchChange());
        if (--repathTicks <= 0) {
            repathTicks = getTickCount(10);
            fox.getNavigation().startMovingTo(owner, 1.3);
        }
    }
}
