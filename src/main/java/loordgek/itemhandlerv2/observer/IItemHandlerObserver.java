package loordgek.itemhandlerv2.observer;

import it.unimi.dsi.fastutil.ints.IntSet;
import loordgek.itemhandlerv2.itemhandler.IItemHandler;

@FunctionalInterface
public interface IItemHandlerObserver {

    void onInventoryChanged(IItemHandler handler, IntSet slots);

}
