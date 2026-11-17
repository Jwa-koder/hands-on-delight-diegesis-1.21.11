package com.pockyl.handson_delight.mixin;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** The per-layer item tint colours replaced Minecraft's ItemColors registry after 1.21.1. */
@Mixin(ItemStackRenderState.LayerRenderState.class)
public interface MinecraftAccessor {
    @Accessor("tintLayers") int[] handson$tintLayers();
}
