package com.eliasnvx.krylix.api.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;

import java.util.List;

/**
 * The face Krylix draws for a mob: one or more regions of entity textures, drawn on top of each other and scaled into a
 * square (the enderman's eyes are a second layer).
 *
 * <p>The same format is read from resource packs, one file per entity type at
 * {@code assets/<namespace>/krylix/heads/<path>.json} for entity {@code <namespace>:<path>}:
 * <pre>{@code
 * {
 *   "layers": [
 *     { "texture": "mymod:textures/entity/goblin.png", "u": 8, "v": 8, "width": 8, "height": 8,
 *       "texture_width": 64, "texture_height": 64 }
 *   ]
 * }}</pre>
 * {@code width}/{@code height} default to 8 and {@code texture_width}/{@code texture_height} to 64.
 *
 * @param layers drawn first to last
 */
public record MobHead(List<Layer> layers) {
    public static final Codec<MobHead> CODEC = RecordCodecBuilder.create(i -> i.group(
        Layer.CODEC.listOf(1, 8).fieldOf("layers").forGetter(MobHead::layers)
    ).apply(i, MobHead::new));

    public MobHead {
        layers = List.copyOf(layers);
        if (layers.isEmpty()) {
            throw new IllegalArgumentException("a mob head needs at least one layer");
        }
    }

    /**
     * A single-layer head.
     *
     * @param texture       the texture, for example {@code mymod:textures/entity/goblin.png}
     * @param u             left edge of the face in the texture, in pixels
     * @param v             top edge of the face in the texture, in pixels
     * @param width         face width in pixels
     * @param height        face height in pixels
     * @param textureWidth  the texture's width in pixels
     * @param textureHeight the texture's height in pixels
     * @return the head
     */
    public static MobHead of(Identifier texture, int u, int v, int width, int height, int textureWidth, int textureHeight) {
        return new MobHead(List.of(new Layer(texture, u, v, width, height, textureWidth, textureHeight)));
    }

    /**
     * One region of a texture.
     *
     * @param texture       the texture
     * @param u             left edge, in pixels
     * @param v             top edge, in pixels
     * @param width         region width, in pixels
     * @param height        region height, in pixels
     * @param textureWidth  the texture's width, in pixels
     * @param textureHeight the texture's height, in pixels
     */
    public record Layer(Identifier texture, int u, int v, int width, int height, int textureWidth, int textureHeight) {
        public static final Codec<Layer> CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("texture").forGetter(Layer::texture),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("u").forGetter(Layer::u),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("v").forGetter(Layer::v),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("width", 8).forGetter(Layer::width),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("height", 8).forGetter(Layer::height),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("texture_width", 64).forGetter(Layer::textureWidth),
            ExtraCodecs.POSITIVE_INT.optionalFieldOf("texture_height", 64).forGetter(Layer::textureHeight)
        ).apply(i, Layer::new));
    }
}
