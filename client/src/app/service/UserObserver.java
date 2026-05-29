package app.service;

/**
 * Interface for components that want to be notified when users are updated or registered.
 */
public interface UserObserver {
    void onUsersUpdated();
}
