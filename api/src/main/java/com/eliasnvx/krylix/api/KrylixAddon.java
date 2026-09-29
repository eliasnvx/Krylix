package com.eliasnvx.krylix.api;

/**
 * Entry point of a Krylix addon.
 *
 * <p>Implementations need a public no-argument constructor. They are discovered per loader:
 * <ul>
 *     <li><b>Fabric:</b> list the class under the {@value #FABRIC_ENTRYPOINT} entrypoint in {@code fabric.mod.json}:
 *     <pre>{@code "entrypoints": { "krylix": [ "com.example.MyKrylixAddon" ] }}</pre></li>
 *     <li><b>NeoForge:</b> annotate the class with {@link RegisterKrylixAddon}.</li>
 * </ul>
 * A multi-loader addon does both on the same class.
 *
 * <p>Each addon is created once. An addon that throws is logged and skipped; the others still load. Krylix is an
 * optional dependency for most addons: declare it as {@code suggests}/{@code optional} and the entrypoint simply never
 * runs without it.
 */
public interface KrylixAddon {
    /** Name of the Fabric entrypoint used to discover addons: {@value}. */
    String FABRIC_ENTRYPOINT = "krylix";

    /**
     * Called once on both physical sides, after Krylix itself has initialized. Register event listeners here.
     *
     * @param api the API
     */
    void onInitialize(KrylixApi api);

    /**
     * Called once on the physical client, after {@link #onInitialize}. Register mob heads, health providers and
     * client event listeners here.
     *
     * @param api the client API
     */
    default void onInitializeClient(KrylixClientApi api) {
    }
}
