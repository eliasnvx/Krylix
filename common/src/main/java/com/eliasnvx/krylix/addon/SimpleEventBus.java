package com.eliasnvx.krylix.addon;

import com.eliasnvx.krylix.Krylix;
import com.eliasnvx.krylix.api.event.CancellableEvent;
import com.eliasnvx.krylix.api.event.EventPriority;
import com.eliasnvx.krylix.api.event.KrylixEvent;
import com.eliasnvx.krylix.api.event.KrylixEventBus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Listeners by exact event class, sorted by priority (stable within one priority). */
public final class SimpleEventBus implements KrylixEventBus {
    private record Listener(EventPriority priority, Consumer<Object> action) {
    }

    /** Copy-on-write lists: registration is rare (addon init), posting happens every kill. */
    private final Map<Class<?>, List<Listener>> listeners = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <E extends KrylixEvent> void addListener(Class<E> type, EventPriority priority, Consumer<? super E> listener) {
        Listener added = new Listener(priority, event -> ((Consumer<Object>) listener).accept(event));
        listeners.compute(type, (key, current) -> {
            List<Listener> next = current == null ? new ArrayList<>() : new ArrayList<>(current);
            next.add(added);
            next.sort(Comparator.comparing(Listener::priority)); // List.sort is stable
            return List.copyOf(next);
        });
    }

    @Override
    public <E extends KrylixEvent> E post(E event) {
        List<Listener> list = listeners.get(event.getClass());
        if (list == null) {
            return event;
        }
        for (Listener listener : list) {
            if (event instanceof CancellableEvent cancellable && cancellable.isCancelled()) {
                break;
            }
            try {
                listener.action().accept(event);
            } catch (Throwable t) {
                Krylix.LOGGER.error("A Krylix {} listener failed", event.getClass().getSimpleName(), t);
            }
        }
        return event;
    }

    /** Whether anyone listens: lets callers skip building an event nobody reads. */
    public boolean hasListeners(Class<? extends KrylixEvent> type) {
        return listeners.containsKey(type);
    }
}
