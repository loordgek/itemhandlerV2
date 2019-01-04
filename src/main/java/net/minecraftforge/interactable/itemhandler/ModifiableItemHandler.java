package net.minecraftforge.interactable.itemhandler;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.item.ItemStack;
import net.minecraftforge.interactable.ModifiableInteractable;
import net.minecraftforge.interactable.api.InteractableOperationResult;
import net.minecraftforge.interactable.api.*;
import net.minecraftforge.interactable.itemhandler.api.IItemHandlerTransaction;
import net.minecraftforge.interactable.itemhandler.api.IModifiableItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.Collection;

/**
 * A default implementation of the {@link IModifiableItemHandler} interface.
 * Comes with a default {@link IItemHandlerTransaction} that allows modifications to all slots.
 *
 * If a different behaviour is required during transactions, extend {@link ItemHandlerTransaction}
 * and override the {@link #beginTransaction()} method.
 */
public class ModifiableItemHandler extends ModifiableInteractable<ItemStack> implements IModifiableItemHandler {

    /**
     * The current transaction.
     */
    private IInteractableTransaction<ItemStack> activeTransaction;

    /**
     * Creates a default handler with the given size.
     * All slots are modifyable.
     *
     * @param size The size of the interactable.
     */
    public ModifiableItemHandler(int size) {
        super(size);
    }

    /**
     * Creates a default handler with the given array.
     * All slots are modifyable.
     *
     * @param iterable The iterable.
     */
    public ModifiableItemHandler(ItemStack... iterable) {
        super(iterable);
    }

    /**
     * Creates a default handler with the collection as delegate.
     * All slots are modifyable.
     *
     * @param iterable The iterable.
     */
    public ModifiableItemHandler(Collection<ItemStack> iterable) {
        super(iterable);
    }

    /**
     * Method used to build a new transaction.
     * Can be overriden by subclasses to return different transactions with different behaviours.
     *
     * @return The new transaction, about to become the active transaction.
     */
    protected ItemHandlerTransaction buildNewTransaction()
    {
        return new ItemHandlerTransaction(this);
    }

    /**
     * Base implementation of the {@link IItemHandlerTransaction}.
     *
     * This is a internal class of {@link ModifiableItemHandler} so that the underlying datastorage arrays
     * are accessible for the class, while not being accessible on the public api surface.
     *
     * If anybody has a better solution for this. Feel free to comment and/or adapt.
     */
    public class ItemHandlerTransaction extends AbstractTransaction<ItemStack> implements IItemHandlerTransaction {

        private final ModifiableItemHandler itemHandler;
        private Int2ObjectMap<ItemStack> pendingStacks = new Int2ObjectOpenHashMap<>();

        public ItemHandlerTransaction(ModifiableItemHandler itemHandler) {
            super(itemHandler);
            this.itemHandler = itemHandler;
        }

        @Override
        public ItemStack get(int slot) {
            return pendingStacks.containsKey(slot) ? pendingStacks.get(slot) : super.get(slot);
        }

        @Override
        public void commit() throws TransactionNotValidException {
            for (Int2ObjectMap.Entry<ItemStack> objectSet : pendingStacks.int2ObjectEntrySet()){
                interactable.set(objectSet.getIntKey(), objectSet.getValue());
            }
            super.commit();
        }

        @Override
        public void cancel() {
            pendingStacks.clear();
            super.cancel();
        }

        @Override
        public IInteractableOperationResult<ItemStack> insert(int slot, ItemStack toInsert) {
            //Empty stacks can not be inserted by default. They are an invalid call to this method.
            if (toInsert.isEmpty() || slot < 0 || slot >= size())
                return InteractableOperationResult.invalid();

            final ItemStack stackInSlot = get(slot);

            //None stackable stacks are conflicting
            if (!ItemHandlerHelper.canItemStacksStack(stackInSlot, toInsert))
                return InteractableOperationResult.conflicting();

            final ItemStack insertedStack = stackInSlot.copy();
            insertedStack.setCount(Math.min(toInsert.getMaxStackSize(), (stackInSlot.getCount() + toInsert.getCount())));

            ItemStack leftOver = toInsert.copy();
            leftOver.setCount(toInsert.getCount() - insertedStack.getCount());
            if (leftOver.getCount() <= 0)
                leftOver = ItemStack.EMPTY;

            if (leftOver.getCount() == toInsert.getCount())
                return InteractableOperationResult.failed();

            this.pendingStacks.put(slot, insertedStack);
            super.onSlotInteracted(slot);

            return InteractableOperationResult.success(leftOver, super.get(slot));
        }

        @Override
        public IInteractableOperationResult<ItemStack> insert(final ItemStack toInsert) {
            //Inserting an empty stack is invalid.
            if (toInsert.isEmpty())
                return InteractableOperationResult.invalid();

            boolean wasConflicted = false;
            ItemStack workingStack = toInsert.copy();
            for (int i = 0; i < size(); i++) {
                final IInteractableOperationResult<ItemStack> insertionAttemptResult = this.insert(i, workingStack);
                if (insertionAttemptResult.wasSuccessful()) {
                    workingStack = insertionAttemptResult.getPrimary();
                }
                else if (insertionAttemptResult.getStatus().isConflicting())
                {
                    wasConflicted = true;
                    if (insertionAttemptResult.getPrimary() != ItemStack.EMPTY)
                    {
                        workingStack = insertionAttemptResult.getPrimary();
                    }
                }

                if (workingStack.isEmpty())
                    return InteractableOperationResult.success(ItemStack.EMPTY, null);
            }

            if (workingStack.getCount() == toInsert.getCount())
                return InteractableOperationResult.failed();

            if (wasConflicted)
                return InteractableOperationResult.conflicting(workingStack);

            return InteractableOperationResult.success(workingStack, null);
        }

        @Override
        public IInteractableOperationResult<ItemStack> extract(int slot, int amount) {
            //Extracting <= 0 is invalid by default for this method.
            if (amount <= 0 || slot < 0 || slot >= size())
                return InteractableOperationResult.invalid();

            final ItemStack stack = get(slot);
            if (stack.isEmpty())
                return InteractableOperationResult.failed();

            int toExtract = Math.min(stack.getCount(), amount);

            if (toExtract == 0){
                this.pendingStacks.put(slot, ItemStack.EMPTY);
                return InteractableOperationResult.success(super.get(slot), ItemStack.EMPTY);
            }

            final ItemStack extracted = stack.splitStack(toExtract);

            ItemStack remaining = stack.splitStack(stack.getCount() - toExtract);

            this.pendingStacks.put(slot, remaining);
            super.onSlotInteracted(slot);

            return InteractableOperationResult.success(extracted, remaining);
        }

        @Override
        public final IModifiableInteractable<ItemStack> getInteractable() {
            return itemHandler;
        }
    }
}
