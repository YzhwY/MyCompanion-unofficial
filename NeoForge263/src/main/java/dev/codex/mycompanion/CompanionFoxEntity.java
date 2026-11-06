package dev.codex.mycompanion;

import java.util.UUID;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class CompanionFoxEntity extends Fox {
    private UUID ownerUuid;

    public CompanionFoxEntity(EntityType<? extends Fox> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setCanPickUpLoot(false);
        setPermanentlyInvulnerable(true);
        setCustomName(Component.literal("我的伙伴"));
        setCustomNameVisible(true);
    }

    @Override
    protected void registerGoals() {
        // Fox's save reader expects its private hunting goals to have been initialized.
        super.registerGoals();
        goalSelector.removeAllGoals(goal -> true);
        targetSelector.removeAllGoals(goal -> true);
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new CrowdOwnerFeetGoal(this));
        goalSelector.addGoal(2, new TriggerNearbyTrapsGoal(this));
        goalSelector.addGoal(3, new FollowCompanionOwnerGoal(this));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    public UUID getOwnerUuid() { return ownerUuid; }
    public void setOwnerUuid(UUID ownerUuid) { this.ownerUuid = ownerUuid; }

    public ServerPlayer getOwner() {
        return ownerUuid != null && level() instanceof ServerLevel level
                ? level.getServer().getPlayerList().getPlayer(ownerUuid) : null;
    }

    public boolean isBehaviorEnabled(int option) {
        return ownerUuid != null && level() instanceof ServerLevel level
                && CompanionState.get(level.getServer()).settings(ownerUuid).enabled(option);
    }

    public boolean canPlayWith(ServerPlayer owner) {
        return owner != null && owner.isAlive() && !owner.isSpectator() && owner.level() == level()
                && distanceToSqr(owner) <= 144 && !isPassenger()
                && !(owner.containerMenu instanceof CompanionControlMenu panel && panel.controls(this));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (ownerUuid != null) output.store("CompanionOwner", UUIDUtil.CODEC, ownerUuid);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        ownerUuid = input.read("CompanionOwner", UUIDUtil.CODEC).orElse(null);
        targetSelector.removeAllGoals(goal -> true);
        setSitting(false);
        setIsCrouching(false);
        setPersistenceRequired();
        setCanPickUpLoot(false);
        setPermanentlyInvulnerable(true);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            EntitySpawnReason reason, SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        targetSelector.removeAllGoals(goal -> true);
        return result;
    }

    // The vanilla flag alone excludes void, creative attacks and bypass damage.
    // Reject damage at the entrypoint as well; keep discard() available for lifecycle cleanup.
    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) { return true; }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) { return false; }

    @Override
    public void tick() {
        if (level() instanceof ServerLevel level && (ownerUuid == null
                || !getUUID().equals(CompanionState.get(level.getServer()).get(ownerUuid)))) {
            discard();
            return;
        }
        super.tick();
    }

    @Override
    public boolean isSleeping() { return false; }

    @Override
    public boolean isFood(ItemStack stack) { return false; }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!player.getUUID().equals(ownerUuid)) {
            if (player instanceof ServerPlayer serverPlayer) serverPlayer.sendSystemMessage(Component.translatable("message.my_companion.not_owner"), true);
            return InteractionResult.SUCCESS;
        }
        if (!level().isClientSide() && isAlive()) {
            player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                    (id, inventory, viewer) -> new CompanionControlMenu(id, inventory, this),
                    Component.translatable("screen.my_companion.title")));
            getNavigation().stop();
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean canBeLeashed() { return false; }
}
