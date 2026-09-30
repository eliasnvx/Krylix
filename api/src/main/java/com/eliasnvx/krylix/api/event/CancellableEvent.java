package com.eliasnvx.krylix.api.event;

/**
 * An event whose action can be prevented. Once a listener cancels it, the remaining listeners are not called, whatever
 * their priority, and there is no way to un-cancel it.
 */
public interface CancellableEvent extends KrylixEvent {
    /** @return whether a listener cancelled the event */
    boolean isCancelled();

    /** Cancels the event. It cannot be un-cancelled. */
    void cancel();
}
