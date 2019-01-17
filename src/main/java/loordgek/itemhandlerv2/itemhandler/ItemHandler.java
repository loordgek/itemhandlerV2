package loordgek.itemhandlerv2.itemhandler;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import loordgek.itemhandlerv2.filter.IStackFilter;
import loordgek.itemhandlerv2.observer.IItemHandlerObserver;
import loordgek.itemhandlerv2.observer.IItemHandlerObserverble;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ItemHandler implements IItemHandler, IItemHandlerObserverble {
    private final List<IItemHandlerObserver> observers = new ArrayList<>();
    private final NonNullList<ItemStack> stacks;
    @Nullable
    private Transaction activeTransaction = null;

    public ItemHandler(int size) {
        this.stacks = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    @Override
    public int size() {
        return stacks.size();
    }

    protected int getStackLimit(int slot, ItemStack stack) {
        return Math.min(getSlotLimit(slot), stack.getMaxStackSize());
    }

    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot) {
        return stacks.get(slot);
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64;
    }

    @Override
    public ITransaction setStack(int slot, ItemStack stack) {
        if (stack.isEmpty() && getStackInSlot(slot).isEmpty())
            return ITransaction.UNDEFINED;
        int limit = Math.min(stack.getCount(), getStackLimit(slot, stack));


        Transaction transaction = new Transaction(stack, getSlotLimit(slot) - limit);
        transaction.addSlot(slot);
        transaction.addTask(() -> {
            if (stack.isEmpty())
                stacks.set(slot, ItemStack.EMPTY);
            else stacks.set(slot, ItemHandlerHelper.copyStackWithSize(stack, limit));
        });
        return transaction;
    }

    @Override
    public ITransaction insert(int slot, ItemStack stack) {
        if (stack.isEmpty())
            return ITransaction.UNDEFINED;

        ItemStack stackInSlot = getStackInSlot(slot);

        int limit = getStackLimit(slot, stack);

        if (!stackInSlot.isEmpty()) {
            if (!ItemHandlerHelper.canItemStacksStack(stack, stackInSlot))
                return ITransaction.INVALID;

            limit -= stackInSlot.getCount();
        }
        if (!ItemHandlerHelper.canItemStacksStack(stackInSlot, stack))
            return ITransaction.INVALID;

        if (limit == 0)
            return ITransaction.FAILURE;
        Transaction transaction = new Transaction(stack, stackInSlot.getCount() - limit);
        int finalLimit = limit;
        transaction.addTask(() -> {
            if (stackInSlot.isEmpty()) {
                stacks.set(slot, ItemHandlerHelper.copyStackWithSize(stack, finalLimit));
            } else stackInSlot.grow(finalLimit);
        });
        transaction.addSlot(slot);
        return transaction;
    }

    @Override
    public ITransaction insert(ItemStack stack) {
        if (stack.isEmpty())
            return ITransaction.INVALID;

        Transaction transaction = new Transaction(stack, 0);
        int remainder = stack.getCount();
        for (int i = 0; i < size(); i++) {
            if (remainder == 0)
                break;
            int slot = i;
            ItemStack stackInSlot = getStackInSlot(slot);
            if (stackInSlot.isEmpty()) {
                int limit = Math.min(remainder, getStackLimit(slot, stack));
                remainder -= limit;
                transaction.addTask(() -> stacks.set(slot, ItemHandlerHelper.copyStackWithSize(stack, limit)));
                transaction.addSlot(slot);
            } else if (ItemHandlerHelper.canItemStacksStack(stackInSlot, stack)) {
                int limit = Math.min(remainder, Math.min(stackInSlot.getCount(), getStackLimit(slot, stack)));
                remainder -= limit;
                transaction.addTask(() -> stackInSlot.grow(limit));
                transaction.addSlot(slot);
            }

        }
        if (remainder == 0)
            transaction.result = ItemStack.EMPTY;
        if (remainder == stack.getCount())
            return ITransaction.FAILURE;
        transaction.growResult(remainder);

        return transaction;
    }

    @Override
    public ITransaction extract(int slot, int amount) {
        if (amount == 0) {
            return ITransaction.UNDEFINED;
        }
        ItemStack stackInSlot = getStackInSlot(slot);
        if (stackInSlot.isEmpty())
            return ITransaction.FAILURE;

        int toExtract = Math.min(stackInSlot.getCount(), amount);

        Transaction transaction = new Transaction(stackInSlot, toExtract);
        activeTransaction = transaction;
        transaction.addSlot(slot);
        transaction.addTask(() -> {
            if (toExtract == amount)
                stacks.set(slot, ItemStack.EMPTY);
            else stackInSlot.shrink(toExtract);
        });

        return transaction;
    }

    @Override
    public ITransaction extract(IStackFilter filter, int amount) {
        if (amount == 0) {
            return ITransaction.UNDEFINED;
        }
        Transaction transaction = new Transaction(ItemStack.EMPTY, 0);
        activeTransaction = transaction;
        boolean foundStack = false;
        int amountLeft = amount;
        for (int i = 0; i < size(); i++) {
            int slot = i;
            ItemStack stackInSlot = getStackInSlot(slot);
            if (!stackInSlot.isEmpty() && filter.test(stackInSlot)) {
                if (transaction.result.isEmpty()) {
                    transaction.result = stackInSlot;
                } else if (!ItemHandlerHelper.canItemStacksStack(transaction.result, stackInSlot))
                    continue;
                foundStack = true;
                int toExtract = Math.min(stackInSlot.getCount(), amountLeft);
                amountLeft =- toExtract;
                transaction.growResult(toExtract);
                transaction.addSlot(slot);
                transaction.addTask(() -> {
                    if (toExtract == amount)
                        stacks.set(slot, ItemStack.EMPTY);
                    else stackInSlot.shrink(toExtract);
                });
            }
        }
        if (foundStack)
            return transaction;
        return ITransaction.FAILURE;
    }

    @Override
    public void addObserver(IItemHandlerObserver observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(IItemHandlerObserver observer) {
        observers.remove(observer);
    }

    private interface Executor {
        void execute();
    }

    private class Transaction implements ITransaction {
        private final IntSet slots = new IntOpenHashSet();
        private final List<Executor> tasks = new ArrayList<>();
        private ItemStack result;
        private ItemStack resultCopy = ItemStack.EMPTY;
        private int resultAmount;

        private Transaction(ItemStack result, int resultAmount) {
            this.result = result;
            this.resultAmount = resultAmount;
        }

        void addTask(Executor executor) {
            tasks.add(executor);
        }

        void addSlot(int slot) {
            slots.add(slot);
        }

        void growResult(int resultAmount) {
            this.resultAmount += resultAmount;
        }

        @Override
        @Nonnull
        public ItemStack getResult() {
            if (result.isEmpty())
                return result;
            if (resultCopy.isEmpty())
                resultCopy = ItemHandlerHelper.copyStackWithSize(result, resultAmount);
            return resultCopy;
        }

        @Override
        @Nonnull
        public ItemStack getResultUnsafe() {
            return result;
        }

        @Override
        public int getResultAmount() {
            return resultAmount;
        }

        @Override
        @Nonnull
        public ITransaction cancel() {
            tasks.clear();
            slots.clear();
            activeTransaction = null;
            return this;
        }

        @Override
        @Nonnull
        public ITransaction confirm() {
            tasks.forEach(Executor::execute);
            observers.forEach(observer -> observer.onInventoryChanged(ItemHandler.this, slots));
            activeTransaction = null;
            return this;
        }

        @Override
        public boolean isValid() {
            return activeTransaction == this;
        }

        @Nonnull
        @Override
        public TransactionType getType() {
            return TransactionType.SUCCESS;
        }
    }
}
