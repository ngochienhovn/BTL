package app.service;

/**
 * Interface for components that want to be notified when items are updated, added, or deleted.
 */
public interface ItemObserver {
    void onItemsUpdated();
}
