package com.pockyl.diegesis.impl.client.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.platform.DestFactor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import java.util.HashMap;
import java.util.Map;

/** Explicit GPU pipelines replace the former mutable GL render-state shards. */
final class DiegesisRenderTypes {
    private static final RenderPipeline LIGHT_POOL = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.ENTITY_EMISSIVE_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("diegesis", "light_pool")).withShaderDefine("NO_OVERLAY").withShaderDefine("NO_CARDINAL_LIGHTING").withCull(false).withDepthWrite(false)
            .withBlend(new BlendFunction(SourceFactor.DST_COLOR, DestFactor.ONE)).withDepthBias(-1.0F,-10.0F).build());
    private static final RenderPipeline GHOST = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("diegesis", "ghost")).withSampler("Sampler1").withShaderDefine("PER_FACE_LIGHTING").withShaderDefine("ALPHA_CUTOUT",0.1F).withCull(true).withDepthWrite(false).withBlend(BlendFunction.TRANSLUCENT).build());
    private static final RenderPipeline ITEM = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("diegesis", "item")).withSampler("Sampler1").withShaderDefine("PER_FACE_LIGHTING").withShaderDefine("ALPHA_CUTOUT",0.1F).withCull(true).withDepthWrite(true).withBlend(BlendFunction.TRANSLUCENT).build());
    private static final RenderPipeline CLEAR = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("diegesis", "clear")).withSampler("Sampler1").withShaderDefine("PER_FACE_LIGHTING").withShaderDefine("ALPHA_CUTOUT",0.1F).withCull(true).withDepthWrite(false).withBlend(BlendFunction.TRANSLUCENT)
            .withDepthBias(-1.0F,-10.0F).build());
    private static final RenderPipeline SEEN_OUTLINE_CULL = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.OUTLINE_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("diegesis", "seen_outline_cull")).withCull(true).withDepthWrite(false)
            .withDepthTestFunction(com.mojang.blaze3d.platform.DepthTestFunction.LEQUAL_DEPTH_TEST).build());
    private static final RenderPipeline SEEN_OUTLINE_NO_CULL = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.OUTLINE_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath("diegesis", "seen_outline_no_cull")).withCull(false).withDepthWrite(false)
            .withDepthTestFunction(com.mojang.blaze3d.platform.DepthTestFunction.LEQUAL_DEPTH_TEST).build());
    private static final Map<RenderType,RenderType> SEEN_OUTLINES = new HashMap<>();

    private static final Map<Identifier, RenderType> ITEM_SHEETS = new HashMap<>();
    private static final Map<Identifier, RenderType> CLEAR_SHEETS = new HashMap<>();
    private static final Map<Identifier, RenderType> POOLS = new HashMap<>();
    private static final Map<Identifier, RenderType> GHOSTS = new HashMap<>();
    private static final RenderType ITEMS = of("diegesis_item", ITEM, TextureAtlas.LOCATION_BLOCKS, true, true, false);
    private static final RenderType CLEARS = of("diegesis_clear", CLEAR, TextureAtlas.LOCATION_BLOCKS, true, true, true);

    private DiegesisRenderTypes() {}
    private static RenderType of(String name, RenderPipeline pipeline, Identifier texture, boolean sort, boolean lit, boolean offset) {
        RenderSetup.RenderSetupBuilder setup = RenderSetup.builder(pipeline).withTexture("Sampler0", texture).bufferSize(256);
        if (sort) setup.sortOnUpload();
        if (lit) setup.useLightmap().useOverlay();
        setup.setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE);
        return RenderType.create(name, setup.createRenderSetup());
    }
    static RenderType lightPool(Identifier texture) {
        return POOLS.computeIfAbsent(texture, id -> of("diegesis_light_pool", LIGHT_POOL,id,false,false,true));
    }
    static RenderType ghostEntity(Identifier texture) {
        return GHOSTS.computeIfAbsent(texture, id -> of("diegesis_ghost",GHOST,id,true,true,false));
    }
    /** The outline target's depth comes from the main view; unlike the normal glowing silhouette this tests it. */
    static RenderType seenOutline(RenderType outline) {
        return SEEN_OUTLINES.computeIfAbsent(outline, key -> {
            var binding = key.state.textures.get("Sampler0");
            if (binding == null) return key;
            var pipeline = key.pipeline().isCull() ? SEEN_OUTLINE_CULL : SEEN_OUTLINE_NO_CULL;
            return RenderType.create("diegesis_seen_outline", RenderSetup.builder(pipeline)
                    .withTexture("Sampler0", binding.location()).setOutputTarget(net.minecraft.client.renderer.rendertype.OutputTarget.OUTLINE_TARGET)
                    .setOutline(RenderSetup.OutlineProperty.IS_OUTLINE).bufferSize(256).createRenderSetup());
        });
    }
    static RenderType itemSheet() { return ITEMS; }
    static RenderType itemSheet(Identifier atlas) {
        return atlas.equals(TextureAtlas.LOCATION_BLOCKS) ? ITEMS : ITEM_SHEETS.computeIfAbsent(atlas,
                id -> of("diegesis_item_" + id, ITEM, id, true, true, false));
    }
    static RenderType clearBlockSheet() { return CLEARS; }
    static RenderType clearSheet(Identifier atlas) {
        return atlas.equals(TextureAtlas.LOCATION_BLOCKS) ? CLEARS : CLEAR_SHEETS.computeIfAbsent(atlas,
                id -> of("diegesis_clear_" + id, CLEAR, id, true, true, true));
    }
}
