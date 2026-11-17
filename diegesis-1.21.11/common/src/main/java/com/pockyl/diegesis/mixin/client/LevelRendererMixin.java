package com.pockyl.diegesis.mixin.client;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.pockyl.diegesis.impl.client.render.ClearView;

/** Establish the view-space clear cone before level extraction/rendering. */
@Mixin(LevelRenderer.class)
abstract class LevelRendererMixin {
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void diegesis$beginClearView(GraphicsResourceAllocator allocator, DeltaTracker delta, boolean outline, Camera camera,
            Matrix4f view, Matrix4f projection, Matrix4f cullingProjection, GpuBufferSlice fog, Vector4f fogColor,
            boolean renderSky, CallbackInfo callback) {
        com.pockyl.diegesis.impl.client.render.SceneRenderer.captureMatrices(view, projection);
        ClearView.begin(view, camera);
    }
    @Inject(method = "extractVisibleEntities", at = @At("RETURN"))
    private void diegesis$outlineTarget(Camera camera, net.minecraft.client.renderer.culling.Frustum frustum, DeltaTracker delta,
            net.minecraft.client.renderer.state.LevelRenderState state, CallbackInfo callback) {
        // Request the vanilla outline pass before the world draw callback submits a hovered station's silhouette.
        if (com.pockyl.diegesis.impl.client.focus.FocusController.get().isInteractive()) state.haveGlowingEntities = true;
    }
}
