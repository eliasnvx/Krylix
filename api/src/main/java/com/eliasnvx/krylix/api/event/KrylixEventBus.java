package com.eliasnvx.krylix.api.event;

import java.util.function.Consumer;

/**
 * Loader-independent event bus. Listeners run on the thread that posts the event (see each event). A listener that
 * throws is logged and skipped; the rest still run.
 */
public interface KrylixEventBus {
    /**
     * Registers a listener with {@link EventPriority#NORMAL} priority.
     *
     * @param type     the exact event class
     * @param listener the listener
     * @param <E>      event type
     */
    default <E extends KrylixEvent> void addListener(Class<E> type, Consumer<? super E> listener) {
        addListener(type, EventPriority.NORMAL, listener);
    }

    /**
     * Registers a listener.
     *
     * @param type     the exact event class
     * @param priority call order relative to other listeners
     * @param listener the listener
     * @param <E>      event type
     */
    <E extends KrylixEvent> void addListener(Class<E> type, EventPriority priority, Consumer<? super E> listener);

    /**
     * Posts an event to the listeners of its exact class. Addons may post their own event types too.
     *
     * @param event the event
     * @param <E>   event type
     * @return the same event, to read results such as cancellation
     */
    <E extends KrylixEvent> E post(E event);
}
