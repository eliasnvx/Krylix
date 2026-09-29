package com.eliasnvx.krylix.api.registry;

import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A registry of addon extensions, keyed by id and kept in registration order.
 *
 * <p>Registration is open during addon initialization and closed ({@link #isFrozen() frozen}) afterwards.
 *
 * @param <T> entry type
 */
public interface ApiRegistry<T> {
    /** @return this registry's id, for example {@code krylix:health_provider} */
    Identifier id();

    /**
     * Registers an entry.
     *
     * @param id    unique id, in your mod's namespace
     * @param entry the entry
     * @param <V>   entry subtype
     * @return {@code entry}, for static field initialization
     * @throws IllegalStateException    once the registry is frozen
     * @throws IllegalArgumentException if the id is already taken
     */
    <V extends T> V register(Identifier id, V entry);

    /**
     * @param id the id
     * @return the entry, or empty if nothing is registered under it
     */
    Optional<T> get(Identifier id);

    /** @return the ids, in registration order */
    Set<Identifier> ids();

    /** @return the entries, in registration order */
    List<T> values();

    /** @return whether registration is closed */
    boolean isFrozen();
}
