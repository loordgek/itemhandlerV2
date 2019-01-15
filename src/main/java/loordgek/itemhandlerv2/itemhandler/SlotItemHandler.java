package loordgek.itemhandlerv2.itemhandler;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class SlotItemHandler extends Slot {
    private static IInventory emptyInventory = new InventoryBasic("[Null]", true, 0);
    private final IItemHandler handler;

    public SlotItemHandler(IItemHandler handler, int index, int xPosition, int yPosition) {
        super(emptyInventory, index, xPosition, yPosition);
        this.handler = handler;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return getItemHandler().insert(getSlotIndex(), stack).cancel().getType().isInvalid();
    }

    @Override
    @Nonnull
    public ItemStack getStack() {
        return getItemHandler().getStackInSlot(getSlotIndex());
    }

    @Override
    public void putStack(@Nonnull ItemStack stack) {
        getItemHandler().setStack(getSlotIndex(), stack).confirm();
    }

    @Override
    public int getSlotStackLimit() {
        return getItemHandler().getSlotLimit(getSlotIndex());
    }

    @Override
    @Nonnull
    public ItemStack decrStackSize(int amount) {
        return getItemHandler().extract(getSlotIndex(), amount).confirm().getResult();
    }

    @Override
    public boolean isHere(@Nonnull IInventory inv, int slotIn) {
        return false;
    }

    @Override
    public boolean canTakeStack(@Nonnull EntityPlayer playerIn) {
        return getItemHandler().extract(getSlotIndex(), 1).cancel().getType().isInvalid();
    }

    @Override
    public boolean isSameInventory(Slot other) {
        return other instanceof SlotItemHandler && ((SlotItemHandler) other).getItemHandler() == this.getItemHandler();
    }

    private IItemHandler getItemHandler() {
        return handler;
    }
}
