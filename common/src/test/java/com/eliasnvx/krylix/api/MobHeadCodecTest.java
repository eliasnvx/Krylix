package com.eliasnvx.krylix.api;

import com.eliasnvx.krylix.api.client.MobHead;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobHeadCodecTest {
    @Test
    void fillsDefaults() {
        MobHead head = MobHead.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(
            "{\"layers\":[{\"texture\":\"mymod:textures/entity/goblin.png\",\"u\":8,\"v\":8}]}")).getOrThrow();
        assertEquals(MobHead.of(Identifier.parse("mymod:textures/entity/goblin.png"), 8, 8, 8, 8, 64, 64), head);
    }

    @Test
    void rejectsAHeadWithoutLayers() {
        assertTrue(MobHead.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{\"layers\":[]}")).isError());
    }

    /** Every head Krylix ships must parse: a typo would silently drop that mob's icon. */
    @Test
    void everyBuiltInHeadParses() throws IOException, URISyntaxException {
        Path dir = Path.of(MobHeadCodecTest.class.getResource("/assets/minecraft/krylix/heads").toURI());
        List<Path> files;
        try (Stream<Path> stream = Files.list(dir)) {
            files = stream.filter(p -> p.toString().endsWith(".json")).toList();
        }
        assertTrue(files.size() >= 40, "found only " + files.size() + " heads");
        for (Path file : files) {
            try (InputStream in = Files.newInputStream(file)) {
                var json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                assertTrue(MobHead.CODEC.parse(JsonOps.INSTANCE, json).isSuccess(), file.getFileName() + " does not parse");
            }
        }
    }
}
