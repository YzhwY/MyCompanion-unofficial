package dev.codex.mycompanion;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;

/** Uses vanilla menu-button packets; the server always checks ownership and proximity. */
public final class CompanionControlHandler extends ScreenHandler {
    private final CompanionFoxEntity fox;
    private final PropertyDelegate values;

    public CompanionControlHandler(int syncId, PlayerInventory inventory) {
        this(syncId, null, new ArrayPropertyDelegate(2));
    }

    public CompanionControlHandler(int syncId, PlayerInventory inventory, CompanionFoxEntity fox) {
        this(syncId, fox, new PropertyDelegate() {
            public int get(int index) { return fox.isBehaviorEnabled(index) ? 1 : 0; }
            public void set(int index, int value) { }
            public int size() { return 2; }
        });
    }

    private CompanionControlHandler(int syncId, CompanionFoxEntity fox, PropertyDelegate values) {
        super(MyCompanionMod.CONTROL_PANEL, syncId);
        this.fox = fox;
        this.values = values;
        addProperties(values);
    }

    public boolean enabled(int index) { return values.get(index) != 0; }
    public boolean controls(CompanionFoxEntity entity) { return fox == entity; }

    @Override
    public boolean canUse(PlayerEntity player) {
        return fox == null || fox.isAlive() && !fox.isRemoved() && player.isAlive() && !player.isSpectator()
                && fox.getWorld() == player.getWorld() && fox.squaredDistanceTo(player) <= 64
                && player.getUuid().equals(fox.getOwnerUuid())
                && fox.getUuid().equals(CompanionState.get(player.getServer()).get(player.getUuid()));
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (fox == null || !canUse(player) || id < 0 || id > 1) return false;
        CompanionState.get(player.getServer()).toggle(player.getUuid(), id);
        fox.getNavigation().stop();
        sendContentUpdates();
        return true;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) { return ItemStack.EMPTY; }
}
