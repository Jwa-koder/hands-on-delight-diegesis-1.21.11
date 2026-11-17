package com.pockyl.diegesis.mixin.client;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.ItemOwner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.pockyl.diegesis.impl.client.mesh.ItemShapes;
/** Every top-level item view passes through the model resolver in 1.21.11. */
@Mixin(ItemModelResolver.class)
abstract class ItemRendererMixin {
    @Inject(method="updateForTopItem",at=@At("RETURN"))
    private void diegesis$shapedItem(ItemStackRenderState state,ItemStack stack,ItemDisplayContext context,Level level,ItemOwner owner,int seed,CallbackInfo ci) {
        ItemShapes.resolve(stack,state,context);
    }
}
