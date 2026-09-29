package com.eliasnvx.krylix.addon;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleApiRegistryTest {
    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("test", path);
    }

    @Test
    void keepsRegistrationOrder() {
        SimpleApiRegistry<String> registry = new SimpleApiRegistry<>(id("registry"));
        registry.register(id("b"), "B");
        registry.register(id("a"), "A");
        assertEquals(List.of("B", "A"), registry.values());
        assertEquals(List.of(id("b"), id("a")), List.copyOf(registry.ids()));
        assertEquals("A", registry.get(id("a")).orElseThrow());
        assertTrue(registry.get(id("missing")).isEmpty());
    }

    @Test
    void rejectsDuplicatesAndLateRegistration() {
        SimpleApiRegistry<String> registry = new SimpleApiRegistry<>(id("registry"));
        registry.register(id("a"), "A");
        assertThrows(IllegalArgumentException.class, () -> registry.register(id("a"), "again"));
        registry.freeze();
        assertTrue(registry.isFrozen());
        assertThrows(IllegalStateException.class, () -> registry.register(id("late"), "late"));
    }
}
