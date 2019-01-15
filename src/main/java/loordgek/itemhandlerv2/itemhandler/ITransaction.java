package loordgek.itemhandlerv2.itemhandler;

import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public interface ITransaction {

    ITransaction INVALID = new ITransaction() {
        @Override
        @Nonnull
        public ItemStack getResult() {
            return ItemStack.EMPTY;
        }

        @Override
        @Nonnull
        public ItemStack getResultUnsafe() {
            return ItemStack.EMPTY;
        }

        @Override
        public int getResultAmount() {
            return 0;
        }

        @Override
        @Nonnull
        public ITransaction cancel() {
            return this;
        }

        @Override
        @Nonnull
        public ITransaction confirm() {
            return this;
        }

        @Override
        public boolean isValid() {
            return false;
        }

        @Nonnull
        @Override
        public Type getType() {
            return Type.INVALID;
        }
    };
    ITransaction FAILURE = new ITransaction() {
        @Override
        @Nonnull
        public ItemStack getResult() {
            return ItemStack.EMPTY;
        }

        @Nonnull
        @Override
        public ItemStack getResultUnsafe() {
            return ItemStack.EMPTY;
        }

        @Override
        public int getResultAmount() {
            return 0;
        }

        @Override
        @Nonnull
        public ITransaction cancel() {
            return this;
        }

        @Override
        @Nonnull
        public ITransaction confirm() {
            return this;
        }

        @Override
        public boolean isValid() {
            return false;
        }

        @Nonnull
        @Override
        public Type getType() {
            return Type.FAILURE;
        }
    };
    ITransaction UNDEFINED = new ITransaction() {
        @Override
        @Nonnull
        public ItemStack getResult() {
            return ItemStack.EMPTY;
        }

        @Nonnull
        @Override
        public ItemStack getResultUnsafe() {
            return ItemStack.EMPTY;
        }

        @Override
        public int getResultAmount() {
            return 0;
        }

        @Override
        @Nonnull
        public ITransaction cancel() {
            return this;
        }

        @Override
        @Nonnull
        public ITransaction confirm() {
            return this;
        }

        @Override
        public boolean isValid() {
            return false;
        }

        @Nonnull
        @Override
        public Type getType() {
            return Type.UNDEFINED;
        }
    };

    /**
     * Gets the resulting {@link ItemStack} of this transaction.
     * <p/>
     * When inserting, this is the leftover stack.<br/>
     * When extracting, this is the stack that was extracted.
     */
    @Nonnull
    ItemStack getResult();

    /**
     * Gets the resulting {@link ItemStack} of this transaction.
     * <p>
     * DO NOT MODIFY THE ItemStack
     * the size of the stack is not accurate use getResultAmount to get the amount
     * <p>
     * <p/>
     * When inserting, this is the leftover stack.<br/>
     * When extracting, this is the stack that was extracted.
     */
    @Nonnull
    ItemStack getResultUnsafe();

    /**
     * Gets the resulting amount of this transaction.
     * <p/>
     * When inserting, this is the leftover amount.<br/>
     * When extracting, this is the amount that was extracted.
     */
    int getResultAmount();

    /**
     * Cancels this transaction and invalidates it and all the ones issued after it.<br/>
     * If another transaction's cancellation has invalidated this one, an {@link IllegalStateException} will be thrown.
     */
    @Nonnull
    ITransaction cancel();

    /**
     * Confirms this transaction and invalidates it.<br/>
     * If another transaction was issued prior to this one and has not been completed yet, an {@link IllegalStateException} will be
     * thrown.<br/>
     * If another transaction's cancellation has invalidated this one, an {@link IllegalStateException} will be thrown.
     */
    @Nonnull
    ITransaction confirm();

    /**
     * Checks the validity of this transaction.
     */
    boolean isValid();

    /**
     * the Transaction type
     */
    @Nonnull
    Type getType();
}
