package com.eliasnvx.krylix.api.event;

/**
 * An event posted on the {@link KrylixEventBus}. Events are dispatched by their exact class: a listener for a
 * superclass does not receive subclasses.
 */
public interface KrylixEvent {
}
