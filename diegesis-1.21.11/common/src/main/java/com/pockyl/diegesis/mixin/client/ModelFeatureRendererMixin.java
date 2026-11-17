package com.pockyl.diegesis.mixin.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import com.pockyl.diegesis.impl.client.render.ClearView;

/** Entity models now render from submissions rather than LevelRenderer.renderEntity. */
@Mixin(ModelFeatureRenderer.class)
abstract class ModelFeatureRendererMixin {
    @ModifyArg(method = {"renderBatch", "renderTranslucents"}, at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;getBuffer(Lnet/minecraft/client/renderer/rendertype/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"), index = 0)
    private RenderType diegesis$ghostType(RenderType type) { return ClearView.entityType(type); }

    @ModifyVariable(method = "renderModel", at = @At("HEAD"), argsOnly = true)
    private VertexConsumer diegesis$fadeVertices(VertexConsumer vertices, SubmitNodeStorage.ModelSubmit<?> submit, RenderType type,
            VertexConsumer original, OutlineBufferSource outlines, MultiBufferSource.BufferSource crumbling) {
        return ClearView.entityVertices(vertices, type);
    }
}
