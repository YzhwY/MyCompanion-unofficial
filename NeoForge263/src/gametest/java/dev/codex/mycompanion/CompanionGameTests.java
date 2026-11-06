package dev.codex.mycompanion;

import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Opt-in integration tests, excluded from ordinary release builds. */
@Mod(MyCompanionMod.MOD_ID)
public final class CompanionGameTests {
    private static final Identifier TEST_ID = Identifier.fromNamespaceAndPath(MyCompanionMod.MOD_ID, "companion_lifecycle");
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, MyCompanionMod.MOD_ID);

    public CompanionGameTests(IEventBus bus) {
        FUNCTIONS.register("companion_lifecycle", () -> CompanionGameTests::lifecycle);
        FUNCTIONS.register("mischief", () -> CompanionGameTests::mischief);
        FUNCTIONS.register("invulnerability", () -> CompanionGameTests::invulnerability);
        FUNCTIONS.register(bus);
        bus.addListener(CompanionGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(Identifier.fromNamespaceAndPath(MyCompanionMod.MOD_ID, "empty"));
        var data = new TestData<>(environment, Identifier.withDefaultNamespace("empty"), 200, 0, true);
        event.registerTest(TEST_ID, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, TEST_ID), data));
        var immunityId = Identifier.fromNamespaceAndPath(MyCompanionMod.MOD_ID, "invulnerability");
        var immunityEnv = event.registerEnvironment(Identifier.fromNamespaceAndPath(MyCompanionMod.MOD_ID, "invulnerability_environment"));
        event.registerTest(immunityId, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, immunityId),
                new TestData<>(immunityEnv, Identifier.withDefaultNamespace("empty"), 100, 0, true)));
        var id = Identifier.fromNamespaceAndPath(MyCompanionMod.MOD_ID, "mischief");
        var separate = event.registerEnvironment(Identifier.fromNamespaceAndPath(MyCompanionMod.MOD_ID, "mischief_environment"));
        event.registerTest(id, new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id),
                new TestData<>(separate, Identifier.fromNamespaceAndPath(MyCompanionMod.MOD_ID, "test_arena"), 500, 0, true)));
    }

    private static void invulnerability(GameTestHelper helper) {
        var level = helper.getLevel();
        var fox = MyCompanionMod.COMPANION_FOX.get().create(level, EntitySpawnReason.EVENT);
        var owner = UUID.randomUUID();
        fox.setOwnerUuid(owner);
        CompanionState.get(level.getServer()).put(owner, fox.getUUID());
        var origin = helper.absolutePos(new BlockPos(1, 2, 1));
        fox.snapTo(Vec3.atBottomCenterOf(origin));
        level.addFreshEntity(fox);
        float health = fox.getHealth();
        var sources = fox.damageSources();
        var creativePlayer = helper.makeMockServerPlayer(GameType.CREATIVE);
        var attacks = java.util.List.of(sources.generic(), sources.lava(), sources.onFire(), sources.fall(),
                sources.explosion(null, null), sources.drown(), sources.inWall(), sources.wither(),
                sources.freeze(), sources.magic(), sources.fellOutOfWorld(), sources.genericKill(), sources.playerAttack(creativePlayer));
        helper.assertTrue(fox.isPermanentlyInvulnerable(), "New companion has permanent invulnerability flag");
        for (var attack : attacks) {
            helper.assertTrue(fox.isInvulnerableTo(level, attack), "Every damage source must be rejected by immunity check");
            helper.assertTrue(!fox.hurtServer(level, attack, Float.MAX_VALUE), "Damage must be rejected at entrypoint");
            helper.assertTrue(fox.getHealth() == health && fox.isAlive(), "Even lethal damage must leave health unchanged");
        }
        fox.kill(level);
        helper.assertTrue(fox.isAlive() && !fox.isRemoved(), "Kill command damage must not kill companion");
        fox.snapTo(origin.getX() + 0.5, level.getMinY() - 80, origin.getZ() + 0.5);
        fox.tick();
        helper.assertTrue(fox.isAlive() && fox.getHealth() == health && !fox.isRemoved(), "Real void tick must not kill companion");
        fox.snapTo(Vec3.atBottomCenterOf(origin));
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        fox.saveWithoutId(output);
        var legacy = output.buildResult();
        legacy.putBoolean("Invulnerable", false);
        var loaded = MyCompanionMod.COMPANION_FOX.get().create(level, EntitySpawnReason.LOAD);
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), legacy));
        helper.assertTrue(loaded.isPermanentlyInvulnerable(), "Legacy save must gain invulnerability when loaded");
        loaded.setPermanentlyInvulnerable(false);
        helper.assertTrue(!loaded.hurtServer(level, sources.genericKill(), Float.MAX_VALUE) && loaded.isAlive(), "Immunity must not rely only on saved flag");
        var wild = net.minecraft.world.entity.EntityTypes.FOX.create(level, EntitySpawnReason.EVENT);
        float wildHealth = wild.getHealth();
        helper.assertTrue(wild.hurtServer(level, sources.generic(), 1.0F) && wild.getHealth() < wildHealth, "Ordinary fox retains normal damage behavior");
        fox.discard();
        helper.assertTrue(fox.isRemoved(), "Invulnerability must not prevent lifecycle cleanup");
        helper.succeed();
    }

    private static void mischief(GameTestHelper helper) {
        var level = helper.getLevel();
        var state = CompanionState.get(level.getServer());
        var player = helper.makeMockServerPlayerInLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(8, 3, 8));
        for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) {
            level.setBlockAndUpdate(origin.offset(x, -1, z), Blocks.STONE.defaultBlockState());
            for (int y = 0; y < 4; y++) level.setBlockAndUpdate(origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
        }
        player.snapTo(Vec3.atBottomCenterOf(origin));
        player.setXRot(75);
        CompanionManager.maintain(player, state);
        var fox = (CompanionFoxEntity) level.getEntity(state.get(player.getUUID()));
        fox.snapTo(Vec3.atBottomCenterOf(origin.offset(4, 0, 0)));
        state.toggle(player.getUUID(), 1);
        var feet = new CrowdOwnerFeetGoal(fox);
        helper.assertTrue(feet.canUse(), "Looking down enables underfoot goal");
        player.setXRot(0);
        helper.assertTrue(!feet.canUse(), "Looking forward must not enable underfoot goal");
        player.setXRot(75);
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(fox.distanceToSqr(player) < 2.25, "Fox must physically approach owner feet: pos=" + fox.position() + ", player=" + player.position() + ", pitch=" + player.getXRot() + ", enabled=" + fox.isBehaviorEnabled(0));
            fox.mobInteract(player, net.minecraft.world.InteractionHand.MAIN_HAND);
            helper.assertTrue(player.containerMenu instanceof CompanionControlMenu, "Right click opens real control menu");
            var panel = (CompanionControlMenu) player.containerMenu;
            helper.assertTrue(!new CrowdOwnerFeetGoal(fox).canUse(), "Panel pauses mischief");
            player.closeContainer();
            panel.clickMenuButton(player, 0);
            helper.assertTrue(!new CrowdOwnerFeetGoal(fox).canUse(), "Disabled feet goal must not run");
            panel.clickMenuButton(player, 1);
            player.setXRot(0);
            BlockPos plate = origin.offset(3, 0, 0);
            level.setBlockAndUpdate(plate, Blocks.OAK_PRESSURE_PLATE.defaultBlockState());
            helper.assertTrue(TriggerNearbyTrapsGoal.isTrigger(level.getBlockState(plate)), "Pressure plate is a trigger");
            helper.assertTrue(!TriggerNearbyTrapsGoal.isTrigger(Blocks.TRIPWIRE_HOOK.defaultBlockState()), "Bare hook is not a walk target");
            helper.runAfterDelay(80, () -> {
                helper.assertTrue(level.getBlockState(plate).getValue(net.minecraft.world.level.block.PressurePlateBlock.POWERED),
                        "Fox must physically press pressure plate: pos=" + fox.position() + ", owner=" + player.position() + ", settings=" + state.settings(player.getUUID()) + ", goal=" + new TriggerNearbyTrapsGoal(fox).canUse() + ", path=" + fox.getNavigation().createPath(plate, 0) + ", nav=" + fox.getNavigation().isDone() + ", playable=" + fox.canPlayWith(fox.getOwner()) + ", reach=" + fox.getNavigation().createPath(plate, 0).canReach() + ", above=" + fox.getNavigation().createPath(plate.above(), 0).canReach() + ", end=" + fox.getNavigation().createPath(plate, 0).getEndNode() + ", block=" + level.getBlockState(plate));
                level.setBlockAndUpdate(plate, Blocks.AIR.defaultBlockState());
                fox.snapTo(Vec3.atBottomCenterOf(origin));
                BlockPos westHook = origin.offset(-2, 0, 3);
                BlockPos eastHook = origin.offset(2, 0, 3);
                level.setBlockAndUpdate(westHook.west(), Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(eastHook.east(), Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(westHook, Blocks.TRIPWIRE_HOOK.defaultBlockState().setValue(
                        net.minecraft.world.level.block.TripWireHookBlock.FACING, net.minecraft.core.Direction.EAST));
                level.setBlockAndUpdate(eastHook, Blocks.TRIPWIRE_HOOK.defaultBlockState().setValue(
                        net.minecraft.world.level.block.TripWireHookBlock.FACING, net.minecraft.core.Direction.WEST));
                for (int x = -1; x <= 1; x++) level.setBlockAndUpdate(origin.offset(x, 0, 3), Blocks.TRIPWIRE.defaultBlockState());
                helper.runAfterDelay(120, () -> {
                    helper.assertTrue(level.getBlockState(westHook).getValue(net.minecraft.world.level.block.TripWireHookBlock.POWERED),
                            "Fox must physically trip connected tripwire hook");
                    fox.discard();
                    level.getServer().getPlayerList().remove(player);
                    player.discard();
                    helper.succeed();
                });
            });
        });
    }

    private static void lifecycle(GameTestHelper helper) {
        var level = helper.getLevel();
        var state = CompanionState.get(level.getServer());
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        BlockPos origin = helper.absolutePos(new BlockPos(8, 3, 8));
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                level.setBlockAndUpdate(origin.offset(x, -1, z), Blocks.STONE.defaultBlockState());
                for (int y = 0; y <= 4; y++) level.setBlockAndUpdate(origin.offset(x, y, z), Blocks.AIR.defaultBlockState());
            }
        }
        player.setPos(Vec3.atBottomCenterOf(origin));
        CompanionManager.maintain(player, state);
        UUID firstId = state.get(player.getUUID());
        helper.assertTrue(firstId != null, "Player must get a companion");
        var first = (CompanionFoxEntity) level.getEntity(firstId);
        helper.assertTrue(first != null && first.getOwnerUuid().equals(player.getUUID()), "Spawned fox must belong to player");
        helper.assertTrue(first.getCustomName().getString().equals("我的伙伴"), "Companion name must match");
        CompanionManager.maintain(player, state);
        helper.assertTrue(firstId.equals(state.get(player.getUUID())), "Repeated maintenance must keep the same fox");
        var panel = new CompanionControlMenu(1, player.getInventory(), first);
        helper.assertTrue(panel.clickMenuButton(player, 0) && !panel.enabled(0), "Owner can disable underfoot behavior");
        helper.assertTrue(panel.clickMenuButton(player, 1) && !panel.enabled(1), "Owner can disable trigger behavior");
        ServerPlayer stranger = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        stranger.setPos(player.position());
        helper.assertTrue(!panel.clickMenuButton(stranger, 0) && !panel.enabled(0), "Other players cannot modify preferences");
        helper.assertTrue(!panel.clickMenuButton(player, 2), "Unknown option must be rejected");
        helper.assertTrue(CompanionState.CODEC.parse(NbtOps.INSTANCE, new net.minecraft.nbt.CompoundTag()).getOrThrow()
                .settings(player.getUUID()).crowdFeet(), "Legacy saves must get defaults");

        // Exercise the current 26.3 ValueInput/ValueOutput entity persistence path.
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        first.addAdditionalSaveData(output);
        var copy = MyCompanionMod.COMPANION_FOX.get().create(level, EntitySpawnReason.LOAD);
        copy.readAdditionalSaveData(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), output.buildResult()));
        helper.assertTrue(player.getUUID().equals(copy.getOwnerUuid()), "Owner must survive entity serialization");
        helper.assertTrue(!copy.isSleeping() && !copy.isSitting(), "Loaded companion must remain able to follow");

        var encoded = CompanionState.CODEC.encodeStart(NbtOps.INSTANCE, state).getOrThrow();
        var decoded = CompanionState.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        helper.assertTrue(firstId.equals(decoded.get(player.getUUID())), "Owner bindings must survive codec round trip");
        helper.assertTrue(!decoded.settings(player.getUUID()).crowdFeet() && !decoded.settings(player.getUUID()).triggerTraps(),
                "Preferences must survive serialization");
        UUID temporaryOwner = UUID.randomUUID();
        decoded.put(temporaryOwner, UUID.randomUUID());
        decoded.restore(temporaryOwner, null);
        helper.assertTrue(decoded.get(temporaryOwner) == null, "Rejected first spawn must remove its temporary binding");

        // Teleport the entity away without unloading it, then ask the manager to recall it.
        first.snapTo(player.position().add(0, 40, 0));
        CompanionManager.maintain(player, state);
        var recalled = (CompanionFoxEntity) level.getEntity(firstId);
        helper.assertTrue(recalled != null && recalled.distanceToSqr(player) <= 32.0, "Distant fox must be recalled near owner; current=" + state.get(player.getUUID()) + ", original=" + firstId + ", entity=" + recalled + ", pos=" + first.position());
        helper.assertTrue(firstId.equals(state.get(player.getUUID())), "Recall must preserve identity");

        // Keep separate owners independent.
        ServerPlayer second = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        second.setPos(player.position());
        CompanionManager.maintain(second, state);
        UUID secondId = state.get(second.getUUID());
        helper.assertTrue(secondId != null && !firstId.equals(secondId), "Players must have separate companions");

        recalled.discard();
        CompanionManager.maintain(player, state);
        UUID replacementId = state.get(player.getUUID());
        helper.assertTrue(replacementId != null && !replacementId.equals(firstId), "Missing fox must be replaced");
        helper.assertTrue(secondId.equals(state.get(second.getUUID())), "Replacement must not affect another owner");
        var replacement = (CompanionFoxEntity) level.getEntity(replacementId);
        helper.assertTrue(!replacement.isBehaviorEnabled(0) && !replacement.isBehaviorEnabled(1), "Settings survive replacement");
        helper.assertTrue(state.settings(second.getUUID()).crowdFeet(), "Settings remain independent per player");
        copy.setOwnerUuid(player.getUUID());
        copy.tick();
        helper.assertTrue(copy.isRemoved(), "Stale fox must remove itself");

        // A player in the air must wait instead of receiving an unsafe spawn.
        ServerPlayer airborne = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        airborne.setPos(player.position().add(0, 12, 0));
        CompanionManager.maintain(airborne, state);
        helper.assertTrue(state.get(airborne.getUUID()) == null, "No safe floor must postpone spawn");
        ServerPlayer spectator = (ServerPlayer) helper.makeMockServerPlayer(GameType.SPECTATOR);
        spectator.setPos(player.position());
        CompanionManager.maintain(spectator, state);
        helper.assertTrue(state.get(spectator.getUUID()) == null, "Spectators must not get a new companion");
        level.getEntity(replacementId).discard();
        level.getEntity(secondId).discard();
        helper.succeed();
    }
}
