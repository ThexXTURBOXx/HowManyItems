package net.glasslauncher.hmifabric;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;


public class ContainerRecipeViewer extends ScreenHandler {

    public ContainerRecipeViewer(InventoryRecipeViewer iinventory) {
        //setting the windowId to -1 prevents server registering recipe clicks as inventory clicks
        this.syncId = -1;
        inv = iinventory;
        resetSlots();
    }

    public void resetSlots() {
        super.slots.clear();
        count = 0;
    }

    // Not an override. Custom method.
    public void addSlot(int i, int j) {
        addSlot(new Slot(inv, count++, i, j));
    }

    @Override
    public boolean canUse(PlayerEntity entityplayer) {
        return inv.canPlayerUse(entityplayer);
    }

    private int count;
    private InventoryRecipeViewer inv;
}
