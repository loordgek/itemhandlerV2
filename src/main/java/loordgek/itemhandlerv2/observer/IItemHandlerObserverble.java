package loordgek.itemhandlerv2.observer;

public interface IItemHandlerObserverble {

    void addObserver(IItemHandlerObserver observer);

    void removeObserver(IItemHandlerObserver observer);
}
