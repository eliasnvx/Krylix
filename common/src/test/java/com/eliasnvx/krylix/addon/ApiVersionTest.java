package com.eliasnvx.krylix.addon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiVersionTest {
    /** The version addons see at runtime is the one the api artifact is published with. */
    @Test
    void matchesGradleProperties() {
        assertEquals(System.getProperty("krylix.apiVersion"), KrylixApiImpl.API_VERSION,
            "update KrylixApiImpl.API_VERSION together with api_version in gradle.properties");
    }
}
