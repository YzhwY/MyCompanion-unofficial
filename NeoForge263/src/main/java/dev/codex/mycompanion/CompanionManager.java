package dev.codex.mycompanion;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class CompanionManager {
    private CompanionManager() { }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) return;
        CompanionState state = CompanionState.get(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) maintain(player, state);
    }

    static void maintain(ServerPlayer player, CompanionState state) {
        if (!player.isAlive() || player.isSpectator()) return;
        CompanionFoxEntity fox = find(player.level().getServer(), state.get(player.getUUID()));
        if (fox == null || !fox.isAlive()) {
            CompanionFoxEntity replacement = MyCompanionMod.COMPANION_FOX.get().create(player.level(), EntitySpawnReason.EVENT);
            if (replacement == null) return;
            BlockPos destination = findSafePosition(player, replacement);
            if (destination == null) return;
            replacement.setOwnerUuid(player.getUUID());
            replacement.snapTo(Vec3.atBottomCenterOf(destination), player.getYRot(), 0);
            UUID previous = state.get(player.getUUID());
            state.put(player.getUUID(), replacement.getUUID());
            if (!player.level().addFreshEntity(replacement)) state.restore(player.getUUID(), previous);
            else if (fox != null) fox.discard();
        } else if (fox.level() != player.level() || fox.distanceToSqr(player) > 256.0) {
            // PathfindingContext takes its cache from the mob's level; use a destination-level
            // probe when crossing dimensions so cached path types cannot come from the old world.
            CompanionFoxEntity probe = fox.level() == player.level() ? fox
                    : MyCompanionMod.COMPANION_FOX.get().create(player.level(), EntitySpawnReason.EVENT);
            if (probe == null) return;
            BlockPos destination = findSafePosition(player, probe);
            if (destination == null) return;
            fox.getNavigation().stop();
            fox.unRide();
            fox.fallDistance = 0;
            fox.teleport(new TeleportTransition(player.level(), Vec3.atBottomCenterOf(destination),
                    Vec3.ZERO, player.getYRot(), 0, TeleportTransition.DO_NOTHING));
        }
    }

    private static CompanionFoxEntity find(MinecraftServer server, UUID uuid) {
        if (uuid == null) return null;
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(uuid);
            if (entity instanceof CompanionFoxEntity fox && !fox.isRemoved()) return fox;
        }
        return null;
    }

    static BlockPos findSafePosition(ServerPlayer player, CompanionFoxEntity probe) {
        ServerLevel level = player.level();
        BlockPos origin = player.blockPosition();
        PathfindingContext context = new PathfindingContext(level, probe);
        for (int radius : new int[]{2, 3, 4, 1, 0}) {
            for (int dy : new int[]{0, 1, -1, 2, -2, 3, -3}) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                        BlockPos pos = origin.offset(dx, dy, dz);
                        if (!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)
                                || pos.getY() <= level.getMinY() || pos.getY() + 1 >= level.getMaxY()) continue;
                        if (WalkNodeEvaluator.getPathTypeStatic(context, pos.mutable()) != PathType.WALKABLE) continue;
                        AABB box = MyCompanionMod.COMPANION_FOX.get().getDimensions().makeBoundingBox(Vec3.atBottomCenterOf(pos));
                        if (level.noCollision(box) && !level.containsAnyLiquid(box)) return pos;
                    }
                }
            }
        }
        return null;
    }
}
