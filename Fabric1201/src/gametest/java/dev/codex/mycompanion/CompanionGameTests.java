package dev.codex.mycompanion;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.TestContext;
import net.minecraft.nbt.NbtCompound;

public final class CompanionGameTests implements net.fabricmc.fabric.api.gametest.v1.FabricGameTest {
    private static CompanionFoxEntity spawn(TestContext helper, ServerPlayerEntity player, BlockPos origin) {
        var fox = MyCompanionMod.COMPANION_FOX.create(helper.getWorld());
        fox.setOwnerUuid(player.getUuid());
        fox.refreshPositionAndAngles(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, 0, 0);
        CompanionState.get(helper.getWorld().getServer()).put(player.getUuid(), fox.getUuid());
        helper.getWorld().spawnEntity(fox);
        return fox;
    }

    @net.minecraft.test.GameTest(templateName = EMPTY_STRUCTURE, batchId = "invulnerability")
    public void invulnerability(TestContext helper) {
        var world = helper.getWorld();
        var fox = MyCompanionMod.COMPANION_FOX.create(world);
        var owner = java.util.UUID.randomUUID();
        fox.setOwnerUuid(owner);
        CompanionState.get(world.getServer()).put(owner, fox.getUuid());
        var origin = helper.getAbsolutePos(new BlockPos(1, 2, 1));
        fox.refreshPositionAndAngles(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, 0, 0);
        world.spawnEntity(fox);
        float health = fox.getHealth();
        var sources = fox.getDamageSources();
        var creativePlayer = helper.createMockCreativePlayer();
        var attacks = java.util.List.of(sources.generic(), sources.lava(), sources.onFire(), sources.fall(),
                sources.explosion(null, null), sources.drown(), sources.inWall(), sources.wither(),
                sources.freeze(), sources.magic(), sources.outOfWorld(), sources.genericKill(), sources.playerAttack(creativePlayer));
        helper.assertTrue(fox.isInvulnerable(), "New companion has the vanilla invulnerability flag");
        for (var attack : attacks) {
            helper.assertTrue(fox.isInvulnerableTo(attack), "Every damage source must be rejected by the immunity check");
            helper.assertTrue(!fox.damage(attack, Float.MAX_VALUE), "Damage must be rejected at entrypoint");
            helper.assertTrue(fox.getHealth() == health && fox.isAlive(), "Even lethal damage must leave health unchanged");
        }
        fox.kill();
        helper.assertTrue(fox.isAlive() && !fox.isRemoved(), "Kill command damage must not kill companion");
        fox.refreshPositionAndAngles(origin.getX() + 0.5, world.getBottomY() - 80, origin.getZ() + 0.5, 0, 0);
        fox.tick();
        helper.assertTrue(fox.isAlive() && fox.getHealth() == health && !fox.isRemoved(), "Real void tick must not kill companion");
        fox.refreshPositionAndAngles(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, 0, 0);
        NbtCompound legacy = fox.writeNbt(new NbtCompound());
        legacy.putBoolean("Invulnerable", false);
        var loaded = MyCompanionMod.COMPANION_FOX.create(world);
        loaded.readNbt(legacy);
        helper.assertTrue(loaded.isInvulnerable(), "Legacy save must gain invulnerability when loaded");
        loaded.setInvulnerable(false);
        helper.assertTrue(!loaded.damage(sources.genericKill(), Float.MAX_VALUE) && loaded.isAlive(), "Immunity must not rely only on saved flag");
        var wild = net.minecraft.entity.EntityType.FOX.create(world);
        float wildHealth = wild.getHealth();
        helper.assertTrue(wild.damage(sources.generic(), 1.0F) && wild.getHealth() < wildHealth, "Ordinary fox must retain normal damage behavior");
        fox.discard();
        helper.assertTrue(fox.isRemoved(), "Invulnerability must not prevent lifecycle cleanup");
        helper.complete();
    }

    @net.minecraft.test.GameTest(templateName = EMPTY_STRUCTURE, batchId = "settings")
    public void settings(TestContext helper) {
        var player = helper.createMockCreativeServerPlayerInWorld();
        var origin = helper.getAbsolutePos(new BlockPos(2, 3, 2));
        player.refreshPositionAndAngles(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, 0, 0);
        var fox = spawn(helper, player, origin);
        var state = CompanionState.get(helper.getWorld().getServer());
        var panel = new CompanionControlHandler(1, player.getInventory(), fox);
        helper.assertTrue(panel.onButtonClick(player, 0) && !panel.enabled(0), "Owner can toggle feet behavior");
        helper.assertTrue(panel.onButtonClick(player, 1) && !panel.enabled(1), "Owner can toggle triggers");
        var stranger = helper.createMockCreativeServerPlayerInWorld();
        stranger.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), 0, 0);
        helper.assertTrue(!panel.onButtonClick(stranger, 0), "Other players cannot toggle settings");
        helper.assertTrue(!panel.onButtonClick(player, 2), "Invalid option is rejected");
        var decoded = CompanionState.read(state.writeNbt(new NbtCompound()));
        helper.assertTrue(!decoded.settings(player.getUuid()).crowdFeet() && !decoded.settings(player.getUuid()).triggerTraps(), "Settings survive NBT round trip");
        helper.assertTrue(CompanionState.read(new NbtCompound()).settings(player.getUuid()).crowdFeet(), "Old saves get default settings");
        fox.discard();
        var replacement = spawn(helper, player, origin);
        helper.assertTrue(!replacement.isBehaviorEnabled(0) && !replacement.isBehaviorEnabled(1), "Settings survive replacement");
        helper.assertTrue(state.settings(stranger.getUuid()).crowdFeet(), "Settings are independent per player");
        replacement.discard();
        helper.getWorld().getServer().getPlayerManager().remove(player);
        helper.getWorld().getServer().getPlayerManager().remove(stranger);
        player.discard();
        stranger.discard();
        helper.complete();
    }

    @net.minecraft.test.GameTest(templateName = "my_companion:test_arena", tickLimit = 500, batchId = "mischief")
    public void mischief(net.minecraft.test.TestContext helper) {
        var level = helper.getWorld();
        var state = CompanionState.get(level.getServer());
        var player = helper.createMockCreativeServerPlayerInWorld();
        BlockPos origin = helper.getAbsolutePos(new BlockPos(8, 3, 8));
        for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) {
            level.setBlockState(origin.add(x, -1, z), Blocks.STONE.getDefaultState());
            for (int y = 0; y < 4; y++) level.setBlockState(origin.add(x, y, z), Blocks.AIR.getDefaultState());
        }
        player.refreshPositionAndAngles(Vec3d.ofBottomCenter(origin).x, origin.getY(), Vec3d.ofBottomCenter(origin).z, 0, 0);
        player.setPitch(75);
        
        var fox = (CompanionFoxEntity) spawn(helper, player, origin);
        fox.refreshPositionAndAngles(origin.getX() + 4.5, origin.getY(), origin.getZ() + 0.5, 0, 0);
        state.toggle(player.getUuid(), 1);
        var feet = new CrowdOwnerFeetGoal(fox);
        helper.assertTrue(feet.canStart(), "Looking down enables underfoot goal");
        player.setPitch(0);
        helper.assertTrue(!feet.canStart(), "Looking forward must not enable underfoot goal");
        player.setPitch(75);
        helper.waitAndRun(60, () -> {
            helper.assertTrue(fox.squaredDistanceTo(player) < 2.25, "Fox must physically approach owner feet: pos=" + fox.getPos() + ", player=" + player.getPos() + ", pitch=" + player.getPitch() + ", enabled=" + fox.isBehaviorEnabled(0));
            fox.interactMob(player, net.minecraft.util.Hand.MAIN_HAND);
            helper.assertTrue(player.currentScreenHandler instanceof CompanionControlHandler, "Right click opens real control menu");
            var panel = (CompanionControlHandler) player.currentScreenHandler;
            helper.assertTrue(!new CrowdOwnerFeetGoal(fox).canStart(), "Panel pauses mischief");
            player.closeHandledScreen();
            panel.onButtonClick(player, 0);
            helper.assertTrue(!new CrowdOwnerFeetGoal(fox).canStart(), "Disabled feet goal must not run");
            panel.onButtonClick(player, 1);
            player.setPitch(0);
            BlockPos plate = origin.add(3, 0, 0);
            level.setBlockState(plate, Blocks.OAK_PRESSURE_PLATE.getDefaultState());
            helper.assertTrue(TriggerNearbyTrapsGoal.isTrigger(level.getBlockState(plate)), "Pressure plate is a trigger");
            helper.assertTrue(!TriggerNearbyTrapsGoal.isTrigger(Blocks.TRIPWIRE_HOOK.getDefaultState()), "Bare hook is not a walk target");
            helper.waitAndRun(80, () -> {
                helper.assertTrue(level.getBlockState(plate).get(net.minecraft.block.PressurePlateBlock.POWERED),
                        "Fox must physically press pressure plate: pos=" + fox.getPos() + ", owner=" + player.getPos() + ", settings=" + state.settings(player.getUuid()) + ", goal=" + new TriggerNearbyTrapsGoal(fox).canStart() + ", path=" + fox.getNavigation().findPathTo(plate, 0) + ", nav=" + fox.getNavigation().isIdle());
                level.setBlockState(plate, Blocks.AIR.getDefaultState());
                fox.refreshPositionAndAngles(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5, 0, 0);
                BlockPos westHook = origin.add(-2, 0, 3);
                BlockPos eastHook = origin.add(2, 0, 3);
                level.setBlockState(westHook.west(), Blocks.STONE.getDefaultState());
                level.setBlockState(eastHook.east(), Blocks.STONE.getDefaultState());
                level.setBlockState(westHook, Blocks.TRIPWIRE_HOOK.getDefaultState().with(
                        net.minecraft.block.TripwireHookBlock.FACING, net.minecraft.util.math.Direction.EAST));
                level.setBlockState(eastHook, Blocks.TRIPWIRE_HOOK.getDefaultState().with(
                        net.minecraft.block.TripwireHookBlock.FACING, net.minecraft.util.math.Direction.WEST));
                for (int x = -1; x <= 1; x++) level.setBlockState(origin.add(x, 0, 3), Blocks.TRIPWIRE.getDefaultState());
                helper.waitAndRun(120, () -> {
                    helper.assertTrue(level.getBlockState(westHook).get(net.minecraft.block.TripwireHookBlock.POWERED),
                            "Fox must physically trip connected tripwire hook");
                    fox.discard();
                    level.getServer().getPlayerManager().remove(player);
                    player.discard();
                    helper.complete();
                });
            });
        });
    }

}
