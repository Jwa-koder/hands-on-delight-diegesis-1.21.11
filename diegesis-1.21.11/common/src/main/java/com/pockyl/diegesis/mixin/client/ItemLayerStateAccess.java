package com.pockyl.diegesis.mixin.client;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.block.model.ItemTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(ItemStackRenderState.LayerRenderState.class)
public interface ItemLayerStateAccess {
    @Accessor("transform") ItemTransform diegesis$transform();
}
