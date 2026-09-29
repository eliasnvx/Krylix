package com.eliasnvx.krylix.addon;

import com.eliasnvx.krylix.api.event.CancellableEvent;
import com.eliasnvx.krylix.api.event.EventPriority;
import com.eliasnvx.krylix.api.event.KrylixEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleEventBusTest {
    record Ping(List<String> log) implements KrylixEvent {
    }

    static final class Stoppable implements CancellableEvent {
        final List<String> log = new ArrayList<>();
        boolean cancelled;

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void cancel() {
            cancelled = true;
        }
    }

    @Test
    void callsListenersByPriorityThenRegistrationOrder() {
        SimpleEventBus bus = new SimpleEventBus();
        bus.addListener(Ping.class, EventPriority.LOW, e -> e.log().add("low"));
        bus.addListener(Ping.class, e -> e.log().add("normal-1"));
        bus.addListener(Ping.class, EventPriority.HIGHEST, e -> e.log().add("highest"));
        bus.addListener(Ping.class, e -> e.log().add("normal-2"));

        Ping ping = new Ping(new ArrayList<>());
        assertSame(ping, bus.post(ping));
        assertEquals(List.of("highest", "normal-1", "normal-2", "low"), ping.log());
    }

    @Test
    void cancellingStopsLowerPriorities() {
        SimpleEventBus bus = new SimpleEventBus();
        bus.addListener(Stoppable.class, EventPriority.HIGH, e -> {
            e.log.add("high");
            e.cancel();
        });
        bus.addListener(Stoppable.class, e -> e.log.add("normal"));

        Stoppable event = bus.post(new Stoppable());
        assertTrue(event.isCancelled());
        assertEquals(List.of("high"), event.log);
    }

    @Test
    void aThrowingListenerDoesNotStopTheOthers() {
        SimpleEventBus bus = new SimpleEventBus();
        bus.addListener(Ping.class, e -> {
            throw new IllegalStateException("addon bug");
        });
        bus.addListener(Ping.class, e -> e.log().add("still called"));

        Ping ping = bus.post(new Ping(new ArrayList<>()));
        assertEquals(List.of("still called"), ping.log());
    }

    @Test
    void dispatchesByExactClassOnly() {
        SimpleEventBus bus = new SimpleEventBus();
        assertFalse(bus.hasListeners(Ping.class));
        bus.addListener(Stoppable.class, e -> e.log.add("x"));
        assertFalse(bus.hasListeners(Ping.class));
        assertTrue(bus.hasListeners(Stoppable.class));
        Ping ping = bus.post(new Ping(new ArrayList<>()));
        assertTrue(ping.log().isEmpty());
    }
}
