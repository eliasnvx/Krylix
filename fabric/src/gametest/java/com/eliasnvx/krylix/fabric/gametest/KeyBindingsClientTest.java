package com.eliasnvx.krylix.fabric.gametest;

import com.eliasnvx.krylix.client.KrylixKeyBindings;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;

import java.util.ArrayList;
import java.util.List;

/**
 * Krylix's keys: registered in their own category, and no default key taken by a vanilla action (in 26.3 L opened
 * Advancements too, and O never reached mods because the Friends overlay takes it first).
 */
public final class KeyBindingsClientTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        List<String> problems = context.computeOnClient(mc -> {
            List<String> found = new ArrayList<>();
            List<KeyMapping> krylix = new ArrayList<>();
            for (KeyMapping key : mc.options.keyMappings) {
                if (key.getName().startsWith("key.krylix.")) {
                    krylix.add(key);
                    if (!key.getCategory().id().equals(KrylixKeyBindings.CATEGORY_ID)) {
                        found.add(key.getName() + " is in category " + key.getCategory().id());
                    }
                }
            }
            if (krylix.size() != 4) {
                found.add("expected 4 Krylix keys, found " + krylix.size());
            }
            for (KeyMapping mine : krylix) {
                for (KeyMapping other : mc.options.keyMappings) {
                    boolean vanilla = other.getCategory().id().getNamespace().equals("minecraft");
                    boolean debug = other.getCategory() == KeyMapping.Category.DEBUG;
                    if (vanilla && !debug && other.getDefaultKey().equals(mine.getDefaultKey())) {
                        found.add(mine.getName() + " shares its default key with " + other.getName());
                    }
                }
            }
            return found;
        });
        if (!problems.isEmpty()) {
            throw new AssertionError(String.join("; ", problems));
        }
        // The Controls screen lists and sorts the category without errors
        context.setScreen(() -> new KeyBindsScreen(null, context.computeOnClient(mc -> mc.options)));
        context.waitTicks(5);
        context.takeScreenshot("check_keybinds");
        context.setScreen(() -> null);
    }
}
