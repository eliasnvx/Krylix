package com.eliasnvx.krylix.fabric.gametest;

import com.eliasnvx.krylix.client.HealthIndicator;
import com.eliasnvx.krylix.config.HealthBarStyle;
import com.eliasnvx.krylix.config.KrylixConfig;
import com.eliasnvx.krylix.config.ModConfig;
import me.shedaniel.autoconfig.AutoConfigClient;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

/**
 * The health plate goes to the entity under the crosshair and nowhere else, and never reveals what the game hides:
 * invisible entities and nametags hidden by a scoreboard team get no plate. Also opens the config screen.
 * Screenshots ({@code check_*}) are for a human look at the other nametags; the assertions are the test.
 */
public final class HealthPlateClientTest implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        context.getInput().resizeWindow(1280, 720);
        context.runOnClient(mc -> {
            mc.options.tutorialStep = TutorialSteps.NONE;
            mc.options.guiScale().set(2);
        });
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getConnection().waitForChunksRender();
            cmd(world, "gamerule spawn_mobs false");
            cmd(world, "time set 6000");
            // Target in front, a named pig beside it whose own nametag must stay its own
            cmd(world, "summon minecraft:husk ~ ~ ~2.6 {NoAI:1b,PersistenceRequired:1b,Tags:[\"t\"],Rotation:[180f,0f]}");
            cmd(world, "summon minecraft:pig ~1.6 ~ ~3 {NoAI:1b,PersistenceRequired:1b,CustomName:\"Bob\",CustomNameVisible:1b}");
            cmd(world, "execute as @p at @s anchored eyes run tp @s ~ ~ ~ facing entity @e[tag=t,limit=1] eyes");
            context.waitTicks(10);
            expectTarget(context, "husk");
            expectDrawn(context, "husk");
            context.takeScreenshot("check_plates");

            // Every bar style builds and draws (screenshots for a look)
            for (HealthBarStyle style : HealthBarStyle.values()) {
                context.runOnClient(mc -> KrylixConfig.get().healthBarStyle = style);
                expectDrawn(context, style.name());
                context.takeScreenshot("check_style_" + style.name().toLowerCase(java.util.Locale.ROOT));
            }
            context.runOnClient(mc -> KrylixConfig.get().healthBarStyle = HealthBarStyle.HEARTS);

            // F1 hides every name, so the plate too
            context.runOnClient(mc -> mc.gui.hud.toggle());
            context.waitTicks(3);
            expectTarget(context, null);
            context.runOnClient(mc -> mc.gui.hud.toggle());
            context.waitTicks(3);
            expectTarget(context, "husk");

            // Servers hide names with the name tag distance attribute: the plate follows
            cmd(world, "attribute @e[tag=t,limit=1] minecraft:name_tag_distance base set 1");
            context.waitTicks(5);
            expectTarget(context, null);
            cmd(world, "attribute @e[tag=t,limit=1] minecraft:name_tag_distance base reset");
            context.waitTicks(5);
            expectTarget(context, "husk");

            // Invisible: no plate
            cmd(world, "effect give @e[tag=t] minecraft:invisibility 60 0 true");
            context.waitTicks(5);
            expectTarget(context, null);
            cmd(world, "effect clear @e[tag=t]");
            context.waitTicks(5);
            expectTarget(context, "husk");

            // A team that hides its nametags: no plate either
            cmd(world, "team add krylix_hidden");
            cmd(world, "team modify krylix_hidden nametagVisibility never");
            cmd(world, "team join krylix_hidden @e[tag=t]");
            context.waitTicks(5);
            expectTarget(context, null);
            cmd(world, "team leave @e[tag=t]");
            context.waitTicks(5);
            expectTarget(context, "husk");

            // Players' renderer (a mannequin uses the same one) must get the plate too, not the plain nametag
            cmd(world, "kill @e[tag=t]");
            cmd(world, "summon minecraft:mannequin ~ ~ ~2.6 {Tags:[\"m\"],Rotation:[180f,0f]}");
            cmd(world, "execute as @p at @s anchored eyes run tp @s ~ ~ ~ facing entity @e[tag=m,limit=1] eyes");
            context.waitTicks(10);
            expectTarget(context, "mannequin");
            expectDrawn(context, "mannequin");
            cmd(world, "kill @e[tag=m]");

            // A plain armor stand is decoration: no plate
            cmd(world, "summon minecraft:armor_stand ~ ~ ~2.6 {Tags:[\"s\"],Rotation:[180f,0f]}");
            cmd(world, "execute as @p at @s anchored eyes run tp @s ~ ~ ~ facing entity @e[tag=s,limit=1] eyes");
            context.waitTicks(10);
            expectTarget(context, null);
            cmd(world, "kill @e[tag=s]");

            // The config screen builds and opens (the Mod Menu / mod list button uses the same factory)
            context.setScreen(() -> AutoConfigClient.getConfigScreen(ModConfig.class, null).get());
            context.waitTicks(10);
            context.takeScreenshot("check_config");
            context.setScreen(() -> null);
        }
    }

    private static void cmd(TestSingleplayerContext world, String command) {
        world.getServer().runCommand(command);
    }

    /** The plate was really drawn over the target in the next frames, not only picked. */
    private static void expectDrawn(ClientGameTestContext context, String what) {
        int before = context.computeOnClient(mc -> HealthIndicator.platesDrawn());
        context.waitTicks(3);
        int after = context.computeOnClient(mc -> HealthIndicator.platesDrawn());
        if (after <= before) {
            throw new AssertionError("no health plate drawn for " + what);
        }
    }

    private static void expectTarget(ClientGameTestContext context, String type) {
        String actual = context.computeOnClient(mc -> {
            LivingEntity target = HealthIndicator.getCurrentTarget();
            return target == null ? null : EntityType.getKey(target.getType()).getPath();
        });
        if (type == null ? actual != null : !type.equals(actual)) {
            throw new AssertionError("health plate target: expected " + type + ", got " + actual);
        }
    }
}
