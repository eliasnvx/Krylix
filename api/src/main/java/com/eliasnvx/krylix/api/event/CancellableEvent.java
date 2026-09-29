package com.eliasnvx.krylix.api.event;

/**
 * An event whose action can be prevented. Once cancelled, listeners of lower priority are not called.
 */
public interface CancellableEvent extends KrylixEvent {
    /** @return whether a listener cancelled the event */
    boolean isCancelled();

    /** Cancels the event. It cannot be un-cancelled. */
    void cancel();
}
