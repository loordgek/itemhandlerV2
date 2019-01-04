package net.minecraftforge.interactable;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.interactable.api.*;
import net.minecraftforge.interactable.api.observer.IObserverWatchDog;
import net.minecraftforge.interactable.api.observer.ObserveableWatchDog;
import net.minecraftforge.util.ListWithFixedSize;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Abstract modifiable interactable implementation that handles observables as well as the active transaction.
 *
 * @param <T> The type contained in the interactable.
 */
public abstract class ModifiableInteractable<T>  extends Interactable<T> implements IModifiableInteractable<T> {
    private IInteractableTransaction<T> activeTransaction;
    private Map<ResourceLocation, BiConsumer<IModifiableInteractable<T>, Set<Integer>>> observers = new HashMap<>();

    public ModifiableInteractable(int size) {
        super(size);
    }

    public ModifiableInteractable(T... iterable) {
        super(iterable);
    }

    public ModifiableInteractable(Collection<T> iterable) {
        super(iterable);
    }

    @Override
    public final IInteractableTransaction<T> beginTransaction() {
        this.activeTransaction = buildNewTransaction();
        return activeTransaction;
    }

    /**
     * Method used to build a new transaction.
     * Can be overriden by subclasses to return different transactions with different behaviours.
     *
     * @return The new transaction, about to become the active transaction.
     */
    protected abstract AbstractTransaction<T> buildNewTransaction();

    @Override
    public final boolean isActiveTransaction(IInteractableTransaction<T> transactionToCheck) {
        return activeTransaction == transactionToCheck;
    }

    @Override
    public final IObserverWatchDog<T> openObserver(ResourceLocation id, BiConsumer<IModifiableInteractable<T>, Set<Integer>> callback) throws IllegalArgumentException {
        if (observers.containsKey(id))
            throw new IllegalArgumentException(String.format("Observer with ID: %s is already registered.", id));

        observers.put(id, callback);

        return new ObserveableWatchDog<>(this, id, callback);
    }

    @Override
    public final void closeObserver(ResourceLocation id) throws IllegalArgumentException {
        if (!observers.containsKey(id))
            throw new IllegalArgumentException(String.format("Observer with ID: %s is not registered", id));

        observers.remove(id);
    }

    /**
     * An abstract transaction implementation that handles the modified slots as well as
     * commit and cancel operations. Fires the changed event when committed.
     *
     * Implementers need to call {@link #onSlotInteracted(int)} to indicate that a slot
     * has been interacted with and that it has to be passed on when the changed event is fired.
     *
     * @param <E> The type contained in the transaction.
     */
    public static abstract class AbstractTransaction<E> extends Interactable<E> implements IInteractableTransaction<E>
    {
        private final ModifiableInteractable<E> modifiableInteractable;
        private final Set<Integer> modifiedSlots = new HashSet<>();

        protected AbstractTransaction(ModifiableInteractable<E> modifiableInteractable) {
            super(modifiableInteractable.interactable);
            this.modifiableInteractable = modifiableInteractable;
        }

        @Override
        public void cancel() {
            if (modifiableInteractable.isActiveTransaction(this))
                modifiableInteractable.activeTransaction = null;
        }

        @Override
        public void commit() throws TransactionNotValidException {
            if (!modifiableInteractable.isActiveTransaction(this))
                throw new TransactionNotValidException(modifiableInteractable, this);

            modifiableInteractable.interactable = new ListWithFixedSize<>(interactable);

            //Trigger observables.
            for (final BiConsumer<IModifiableInteractable<E>, Set<Integer>> callback :
                    modifiableInteractable.observers.values()) {
                callback.accept(modifiableInteractable, modifiedSlots);
            }

            this.modifiedSlots.clear();
        }

        /**
         * Needs to be called by {@link #insert(int, Object)}, {@link #insert(Object)},
         * {@link #extract(int, int)}, {@link #extractFirstMatching(IInteractableSearchHandler)} and {@link #extractFirstMatching(IInteractableSearchHandler, int)}
         * to ensure that the observables get the right information about which slots might have changed.
         *
         * @param slotIndex The slot that has been interacted with.
         */
        protected final void onSlotInteracted(int slotIndex)
        {
            modifiedSlots.add(slotIndex);
        }

        @Override
        public IModifiableInteractable<E> getInteractable() {
            return modifiableInteractable;
        }
    }
}
