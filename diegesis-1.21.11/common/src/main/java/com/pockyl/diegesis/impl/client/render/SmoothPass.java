package com.pockyl.diegesis.impl.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import com.pockyl.diegesis.api.client.NodePainter;

/**
 * Minecraft 1.21.11 renders GUI frames through extracted render states and no longer exposes the mutable GL framebuffers
 * used by the original supersampling pass. This port deliberately selects the upstream normal-scene fallback: returning
 * false makes SceneRenderer draw each painter immediately with its station. No painter is dropped, and its geometry and
 * hit-testing stay in the same world space. The experimental port does not yet supersample painters or force them in front
 * of intervening geometry. These methods remain so addon callers can release/flush without depending on render internals.
 */
public final class SmoothPass {
    private SmoothPass() {}
    static void begin(Matrix4f projection, Matrix4f view) { }
    static boolean defer(NodePainter painter, PoseStack poseStack, int light) { return false; }
    public static void draw() { /* Normal-scene fallback already drew every painter during world rendering. */ }
    public static void release() { /* The normal-scene path allocates no extra render targets. */ }
}
