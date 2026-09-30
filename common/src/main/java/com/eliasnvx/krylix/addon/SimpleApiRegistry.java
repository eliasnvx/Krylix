package com.eliasnvx.krylix.addon;

import com.eliasnvx.krylix.api.registry.ApiRegistry;
import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Insertion-ordered registry; open until {@link #freeze()}, read-only (and lock-free to read) after. */
public final class SimpleApiRegistry<T> implements ApiRegistry<T> {
    private final Identifier id;
    private final Map<Identifier, T> entries = new LinkedHashMap<>();
    private volatile boolean frozen;
    private volatile List<T> values = List.of();

    public SimpleApiRegistry(Identifier id) {
        this.id = id;
    }

    @Override
    public Identifier id() {
        return id;
    }

    @Override
    public synchronized <V extends T> V register(Identifier entryId, V entry) {
        Objects.requireNonNull(entryId, "id");
        Objects.requireNonNull(entry, "entry");
        if (frozen) {
            throw new IllegalStateException("Registry " + id + " is frozen: register during KrylixAddon initialization");
        }
        if (entries.containsKey(entryId)) {
            throw new IllegalArgumentException("Duplicate id " + entryId + " in registry " + id);
        }
        entries.put(entryId, entry);
        values = List.copyOf(entries.values());
        return entry;
    }

    @Override
    public synchronized Optional<T> get(Identifier entryId) {
        return Optional.ofNullable(entries.get(entryId));
    }

    @Override
    public synchronized Set<Identifier> ids() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(entries.keySet()));
    }

    @Override
    public List<T> values() {
        return values;
    }

    @Override
    public boolean isFrozen() {
        return frozen;
    }

    public void freeze() {
        frozen = true;
    }
}
