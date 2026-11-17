package com.pockyl.diegesis.api.scene;

import com.mojang.serialization.Codec;

import com.pockyl.diegesis.api.scene.SlotBinding.ItemDisplay;
import com.pockyl.diegesis.impl.codec.StrictCodecs;

/**
 * Makes a node show a result the station computes (a crafting result): clicking it takes the result.
 *
 * @param output  the name of the station output
 * @param display how the result is shown
 * @param size    the size of the shown item in pixels; also the size of the click zone
 * @param countFacing which way its count faces: lying at the near right corner of its place ({@code up}, as before), or standing
 *                at the item's foot facing the block's front ({@code front}), which reads from a camera looking down at a slant
 */
public record OutputBinding(String output, ItemDisplay display, float size, SlotBinding.CountFacing countFacing) {
    public static final Codec<OutputBinding> CODEC = StrictCodecs.record(instance -> instance.group(
            Codec.STRING.fieldOf("output").forGetter(OutputBinding::output),
            StrictCodecs.optional(ItemDisplay.CODEC, "display", ItemDisplay.FLOATING).forGetter(OutputBinding::display),
            StrictCodecs.optional(Codec.floatRange(1.0F, 64.0F), "size", 8.0F).forGetter(OutputBinding::size),
            StrictCodecs.optional(SlotBinding.CountFacing.CODEC, "count_facing", SlotBinding.CountFacing.UP).forGetter(OutputBinding::countFacing)
    ).apply(instance, OutputBinding::new));

    /** As before {@code count_facing} existed: the count lies at the corner. */
    public OutputBinding(String output, ItemDisplay display, float size) {
        this(output, display, size, SlotBinding.CountFacing.UP);
    }

    public static OutputBinding of(String output) {
        return new OutputBinding(output, ItemDisplay.FLOATING, 8.0F);
    }
}
