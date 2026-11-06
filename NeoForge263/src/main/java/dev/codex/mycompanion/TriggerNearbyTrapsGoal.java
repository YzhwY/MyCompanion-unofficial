package dev.codex.mycompanion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;

/** Walks onto real pressure plates / armed, attached tripwire; never changes redstone directly. */
public final class TriggerNearbyTrapsGoal extends Goal {
    private final CompanionFoxEntity fox;
    private ServerPlayer owner;
    private BlockPos target;
    private Path path;
    private long nextSearchTick;
    private int remainingTicks;
    private int repathTicks;

    public TriggerNearbyTrapsGoal(CompanionFoxEntity fox) {
        this.fox = fox;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public static boolean isTrigger(BlockState state) {
        return state.getBlock() instanceof BasePressurePlateBlock
                || state.is(Blocks.TRIPWIRE) && state.getValue(TripWireBlock.ATTACHED) && !state.getValue(TripWireBlock.DISARMED);
    }

    private boolean available() { return fox.canPlayWith(owner) && fox.isBehaviorEnabled(1); }

    @Override
    public boolean canUse() {
        owner = fox.getOwner();
        if (!available() || fox.level().getGameTime() < nextSearchTick) return false;
        nextSearchTick = fox.level().getGameTime() + 40;
        var candidates = new ArrayList<BlockPos>();
        BlockPos origin = owner.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-8, -2, -8), origin.offset(8, 2, 8))) {
            if (pos.distSqr(origin) > 64 || !fox.level().hasChunkAt(pos)) continue;
            if (isTrigger(fox.level().getBlockState(pos))) candidates.add(pos.immutable());
        }
        candidates.sort(Comparator.comparingDouble(pos -> pos.distToCenterSqr(fox.position())));
        for (int i = 0; i < Math.min(candidates.size(), 8); i++) {
            BlockPos candidate = candidates.get(i);
            Path candidatePath = pathTo(candidate);
            if (candidatePath != null && (candidatePath.canReach() || arrivesAt(candidatePath, candidate))) {
                target = candidate;
                path = candidatePath;
                return true;
            }
        }
        return false;
    }

    private boolean arrivesAt(Path path, BlockPos target) {
        var end = path.getEndNode();
        return end != null && end.x == target.getX() && end.z == target.getZ()
                && Math.abs(end.y - target.getY()) <= 1;
    }

    private Path pathTo(BlockPos pos) {
        Path result = fox.getNavigation().createPath(pos, 0);
        if (result != null && (result.canReach() || arrivesAt(result, pos))) return result;
        // Thin pressure plates raise the walkable node above their own block.
        if (fox.level().getBlockState(pos).getBlock() instanceof BasePressurePlateBlock) {
            return fox.getNavigation().createPath(pos.above(), 0);
        }
        return result;
    }

    @Override
    public boolean canContinueToUse() {
        return available() && remainingTicks > 0 && target != null
                && target.distToCenterSqr(owner.position()) <= 81
                && fox.level().hasChunkAt(target) && isTrigger(fox.level().getBlockState(target));
    }

    @Override
    public void start() {
        remainingTicks = adjustedTickDelay(100);
        repathTicks = 0;
        fox.getNavigation().moveTo(path, 1.4);
    }

    @Override
    public void stop() {
        fox.getNavigation().stop();
        target = null;
        path = null;
        owner = null;
        nextSearchTick = fox.level().getGameTime() + 60;
    }

    @Override
    public void tick() {
        remainingTicks--;
        fox.getLookControl().setLookAt(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
        if (--repathTicks <= 0) {
            repathTicks = adjustedTickDelay(10);
            fox.getNavigation().moveTo(pathTo(target), 1.4);
        }
    }
}
