package com.pockyl.diegesis.mixin.client;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(ItemStackRenderState.class)
public interface ItemStackStateAccess {
    @Accessor("layers") ItemStackRenderState.LayerRenderState[] diegesis$layers();
    @Accessor("activeLayerCount") int diegesis$activeLayerCount();
    @Accessor("displayContext") void diegesis$displayContext(ItemDisplayContext context);
}
