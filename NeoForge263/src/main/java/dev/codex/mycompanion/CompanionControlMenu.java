package dev.codex.mycompanion;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public final class CompanionControlMenu extends AbstractContainerMenu {
    private final CompanionFoxEntity fox;
    private final ContainerData values;

    public CompanionControlMenu(int id, Inventory inventory) { this(id, null, new SimpleContainerData(2)); }

    public CompanionControlMenu(int id, Inventory inventory, CompanionFoxEntity fox) {
        this(id, fox, new ContainerData() {
            public int get(int index) { return fox.isBehaviorEnabled(index) ? 1 : 0; }
            public void set(int index, int value) { }
            public int getCount() { return 2; }
        });
    }

    private CompanionControlMenu(int id, CompanionFoxEntity fox, ContainerData values) {
        super(MyCompanionMod.CONTROL_PANEL.get(), id);
        this.fox = fox;
        this.values = values;
        addDataSlots(values);
    }

    public boolean enabled(int index) { return values.get(index) != 0; }
    public boolean controls(CompanionFoxEntity entity) { return fox == entity; }

    @Override
    public boolean stillValid(Player player) {
        return fox == null || fox.isAlive() && !fox.isRemoved() && player.isAlive() && !player.isSpectator()
                && fox.level() == player.level() && fox.distanceToSqr(player) <= 64
                && player.getUUID().equals(fox.getOwnerUuid())
                && player.level() instanceof net.minecraft.server.level.ServerLevel level
                && fox.getUUID().equals(CompanionState.get(level.getServer()).get(player.getUUID()));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (fox == null || !stillValid(player) || id < 0 || id > 1) return false;
        var level = (net.minecraft.server.level.ServerLevel) player.level();
        CompanionState.get(level.getServer()).toggle(player.getUUID(), id);
        fox.getNavigation().stop();
        broadcastChanges();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
}
