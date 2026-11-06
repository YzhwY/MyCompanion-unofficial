package dev.codex.mycompanion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.block.AbstractPressurePlateBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.TripwireBlock;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

/** Walks onto real pressure plates / armed, attached tripwire; never changes redstone directly. */
public final class TriggerNearbyTrapsGoal extends Goal {
    private final CompanionFoxEntity fox;
    private ServerPlayerEntity owner;
    private BlockPos target;
    private Path path;
    private long nextSearchTick;
    private int remainingTicks;
    private int repathTicks;

    public TriggerNearbyTrapsGoal(CompanionFoxEntity fox) {
        this.fox = fox;
        setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    public static boolean isTrigger(BlockState state) {
        return state.getBlock() instanceof AbstractPressurePlateBlock
                || state.isOf(Blocks.TRIPWIRE) && state.get(TripwireBlock.ATTACHED) && !state.get(TripwireBlock.DISARMED);
    }

    private boolean available() { return fox.canPlayWith(owner) && fox.isBehaviorEnabled(1); }

    @Override
    public boolean canStart() {
        owner = fox.getOwner();
        if (!available() || fox.getWorld().getTime() < nextSearchTick) return false;
        nextSearchTick = fox.getWorld().getTime() + 40;
        var candidates = new ArrayList<BlockPos>();
        BlockPos origin = owner.getBlockPos();
        for (BlockPos pos : BlockPos.iterate(origin.add(-8, -2, -8), origin.add(8, 2, 8))) {
            if (pos.getSquaredDistance(origin) > 64 || !fox.getWorld().isChunkLoaded(pos)) continue;
            if (isTrigger(fox.getWorld().getBlockState(pos))) candidates.add(pos.toImmutable());
        }
        candidates.sort(Comparator.comparingDouble(pos -> pos.getSquaredDistance(fox.getPos())));
        for (int i = 0; i < Math.min(candidates.size(), 8); i++) {
            BlockPos candidate = candidates.get(i);
            Path candidatePath = pathTo(candidate);
            if (candidatePath != null && (candidatePath.reachesTarget() || arrivesAt(candidatePath, candidate))) {
                target = candidate;
                path = candidatePath;
                return true;
            }
        }
        return false;
    }

    private boolean arrivesAt(Path path, BlockPos target) {
        var end = path.getEnd();
        return end != null && end.x == target.getX() && end.z == target.getZ()
                && Math.abs(end.y - target.getY()) <= 1;
    }

    private Path pathTo(BlockPos pos) {
        Path result = fox.getNavigation().findPathTo(pos, 0);
        if (result != null && (result.reachesTarget() || arrivesAt(result, pos))) return result;
        // Thin pressure plates raise the walkable node above their own block.
        if (fox.getWorld().getBlockState(pos).getBlock() instanceof AbstractPressurePlateBlock) {
            return fox.getNavigation().findPathTo(pos.up(), 0);
        }
        return result;
    }

    @Override
    public boolean shouldContinue() {
        return available() && remainingTicks > 0 && target != null
                && target.getSquaredDistance(owner.getPos()) <= 81
                && fox.getWorld().isChunkLoaded(target) && isTrigger(fox.getWorld().getBlockState(target));
    }

    @Override
    public void start() {
        remainingTicks = getTickCount(100);
        repathTicks = 0;
        fox.getNavigation().startMovingAlong(path, 1.4);
    }

    @Override
    public void stop() {
        fox.getNavigation().stop();
        target = null;
        path = null;
        owner = null;
        nextSearchTick = fox.getWorld().getTime() + 60;
    }

    @Override
    public void tick() {
        remainingTicks--;
        fox.getLookControl().lookAt(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
        if (--repathTicks <= 0) {
            repathTicks = getTickCount(10);
            fox.getNavigation().startMovingAlong(pathTo(target), 1.4);
        }
    }
}
