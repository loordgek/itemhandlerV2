package loordgek.itemhandlerv2.itemhandler;

public enum TransactionType {

    /**
     * The inventory transaction succeeded.
     */
    SUCCESS,

    /**
     * When inserting, the inventory is full.
     * When extracting, the inventory is empty.
     */
    FAILURE,

    /**
     * When inserting, the stack is not can not be inserted.
     * When extracting, the stack can not be extracted or the filter did not match.
     */
    INVALID,

    /**
     * the transaction was cancelled by a third party
     */
    CANCELLED,

    /**
     * something else :)
     */
    UNDEFINED;

    boolean isSuccess() {
        return this == SUCCESS;
    }

    boolean isFailure() {
        return this == FAILURE;
    }

    boolean isInvalid() {
        return this == INVALID;
    }

    boolean isCancelled() {
        return this == CANCELLED;
    }

}
