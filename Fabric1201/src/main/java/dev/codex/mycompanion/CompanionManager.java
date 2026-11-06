package dev.codex.mycompanion;

import java.util.Set;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class CompanionManager {
    private CompanionManager() { }

    public static void tick(MinecraftServer server) {
        if (server.getTicks() % 20 != 0) return;
        CompanionState state = CompanionState.get(server);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (!player.isAlive() || player.isSpectator()) continue;
            CompanionFoxEntity fox = find(server, state.get(player.getUuid()));
            if (fox == null || !fox.isAlive()) {
                BlockPos destination = findSafePosition(player);
                if (destination == null) continue;
                CompanionFoxEntity replacement = MyCompanionMod.COMPANION_FOX.create(player.getServerWorld());
                if (replacement == null) continue;
                replacement.setOwnerUuid(player.getUuid());
                replacement.refreshPositionAndAngles(destination.getX() + 0.5, destination.getY(),
                        destination.getZ() + 0.5, player.getYaw(), 0);
                // The UUID must be authoritative before the entity's first server tick.
                UUID previous = state.get(player.getUuid());
                state.put(player.getUuid(), replacement.getUuid());
                if (!player.getServerWorld().spawnEntity(replacement)) {
                    // Retain the previous binding when another mod rejects the spawn.
                    if (previous != null) state.put(player.getUuid(), previous);
                } else if (fox != null) {
                    fox.discard();
                }
            } else if (fox.getWorld() != player.getWorld() || fox.squaredDistanceTo(player) > 256.0) {
                BlockPos destination = findSafePosition(player);
                if (destination == null) continue;
                fox.getNavigation().stop();
                fox.detach();
                fox.setVelocity(Vec3d.ZERO);
                fox.fallDistance = 0;
                fox.teleport(player.getServerWorld(), destination.getX() + 0.5, destination.getY(),
                        destination.getZ() + 0.5, Set.of(), player.getYaw(), 0);
            }
        }
    }

    private static CompanionFoxEntity find(MinecraftServer server, UUID uuid) {
        if (uuid == null) return null;
        for (ServerWorld world : server.getWorlds()) {
            Entity entity = world.getEntity(uuid);
            if (entity instanceof CompanionFoxEntity fox && !fox.isRemoved()) return fox;
        }
        return null;
    }

    static BlockPos findSafePosition(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        BlockPos origin = player.getBlockPos();
        // Deterministic nearest-first search; do not generate chunks or place in midair.
        for (int radius : new int[]{2, 3, 4, 1, 0}) {
            for (int dy : new int[]{0, 1, -1, 2, -2, 3, -3}) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                        BlockPos pos = origin.add(dx, dy, dz);
                        if (!world.isChunkLoaded(pos) || !world.getWorldBorder().contains(pos)
                                || pos.getY() <= world.getBottomY() || pos.getY() + 1 >= world.getTopY()) continue;
                        if (LandPathNodeMaker.getLandNodeType(world, pos.mutableCopy()) != PathNodeType.WALKABLE) continue;
                        Box box = MyCompanionMod.COMPANION_FOX.getDimensions().getBoxAt(
                                pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                        if (world.isSpaceEmpty(box) && !world.containsFluid(box)) return pos;
                    }
                }
            }
        }
        return null;
    }
}
