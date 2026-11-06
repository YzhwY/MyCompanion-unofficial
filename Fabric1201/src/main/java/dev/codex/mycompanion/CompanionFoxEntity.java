package dev.codex.mycompanion;

import java.util.UUID;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;

public final class CompanionFoxEntity extends FoxEntity {
    private UUID ownerUuid;

    public CompanionFoxEntity(EntityType<? extends FoxEntity> type, World world) {
        super(type, world);
        setPersistent();
        setCanPickUpLoot(false);
        setInvulnerable(true);
        setCustomName(Text.literal("我的伙伴"));
        setCustomNameVisible(true);
    }

    @Override
    protected void initGoals() {
        // Initialize FoxEntity's private goals: its NBT reader expects them to exist.
        super.initGoals();
        goalSelector.clear(goal -> true);
        targetSelector.clear(goal -> true);
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new CrowdOwnerFeetGoal(this));
        goalSelector.add(2, new TriggerNearbyTrapsGoal(this));
        goalSelector.add(3, new FollowCompanionOwnerGoal(this));
        goalSelector.add(4, new LookAroundGoal(this));
    }

    public UUID getOwnerUuid() { return ownerUuid; }
    public void setOwnerUuid(UUID uuid) { ownerUuid = uuid; }

    public ServerPlayerEntity getOwner() {
        return ownerUuid == null || getServer() == null ? null
                : getServer().getPlayerManager().getPlayer(ownerUuid);
    }

    public boolean isBehaviorEnabled(int option) {
        return ownerUuid != null && getServer() != null && CompanionState.get(getServer()).settings(ownerUuid).enabled(option);
    }

    public boolean canPlayWith(ServerPlayerEntity owner) {
        return owner != null && owner.isAlive() && !owner.isSpectator() && owner.getWorld() == getWorld()
                && squaredDistanceTo(owner) <= 144 && !hasVehicle()
                && !(owner.currentScreenHandler instanceof CompanionControlHandler panel && panel.controls(this));
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (ownerUuid != null) nbt.putUuid("CompanionOwner", ownerUuid);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        nbt = nbt.copy();
        nbt.putBoolean("Sleeping", false);
        super.readCustomDataFromNbt(nbt);
        ownerUuid = nbt.containsUuid("CompanionOwner") ? nbt.getUuid("CompanionOwner") : null;
        // FoxEntity adds hunting targets again when reading NBT.
        targetSelector.clear(goal -> true);
        setSitting(false);
        setCrouching(false);
        setPersistent();
        setCanPickUpLoot(false);
        setInvulnerable(true);
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty,
            SpawnReason reason, EntityData data, NbtCompound nbt) {
        EntityData result = super.initialize(world, difficulty, reason, data, nbt);
        targetSelector.clear(goal -> true);
        return result;
    }

    // The vanilla flag alone excludes void, creative attacks and bypass damage.
    // Reject damage at the entrypoint as well; keep discard() available for lifecycle cleanup.
    @Override
    public boolean isInvulnerableTo(DamageSource source) { return true; }

    @Override
    public boolean damage(DamageSource source, float amount) { return false; }

    @Override
    public void tick() {
        if (!getWorld().isClient && getServer() != null) {
            // An unloaded former companion may reappear after its replacement was created.
            // Validate before it can act; keep exactly one authoritative UUID per owner.
            if (ownerUuid == null || !getUuid().equals(CompanionState.get(getServer()).get(ownerUuid))) {
                discard();
                return;
            }
        }
        super.tick();
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) { return false; }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (!player.getUuid().equals(ownerUuid)) {
            if (!getWorld().isClient) player.sendMessage(Text.translatable("message.my_companion.not_owner"), true);
            return ActionResult.SUCCESS;
        }
        if (!getWorld().isClient && isAlive()) {
            player.openHandledScreen(new net.minecraft.screen.SimpleNamedScreenHandlerFactory(
                    (id, inventory, viewer) -> new CompanionControlHandler(id, inventory, this),
                    Text.translatable("screen.my_companion.title")));
            getNavigation().stop();
        }
        return ActionResult.success(getWorld().isClient);
    }

    @Override
    public boolean canBeLeashedBy(PlayerEntity player) { return false; }
}
