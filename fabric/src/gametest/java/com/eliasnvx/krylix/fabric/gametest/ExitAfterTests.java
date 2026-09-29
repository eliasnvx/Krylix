package com.eliasnvx.krylix.fabric.gametest;

import net.fabricmc.api.ClientModInitializer;

/**
 * The Architectury dev transformer leaves two non-daemon thread pools behind, so after the client stops the JVM
 * never exits and the shutdown watchdog reports a crash. A failed client gametest already ends the game with a
 * crash (non-zero exit); reaching the end of the main thread means every test passed, so exit cleanly then.
 */
public final class ExitAfterTests implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Thread main = Thread.currentThread();
        Thread exit = new Thread(() -> {
            try {
                main.join();
            } catch (InterruptedException e) {
                return;
            }
            System.exit(0);
        }, "Krylix test exit");
        exit.setDaemon(true);
        exit.start();
    }
}
