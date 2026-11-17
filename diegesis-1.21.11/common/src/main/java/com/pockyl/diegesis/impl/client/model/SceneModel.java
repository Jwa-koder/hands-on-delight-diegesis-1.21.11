package com.pockyl.diegesis.impl.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.*;

/** Scene geometry independent of the block/item model split introduced after 1.21.1. */
public final class SceneModel implements BlockStateModel {
    private static final Map<BlockStateModel, SceneModel> BLOCKS = Collections.synchronizedMap(new WeakHashMap<>());
    private record ItemKey(net.minecraft.world.item.Item item, net.minecraft.core.component.DataComponentPatch data, ItemDisplayContext context, Object modelIdentity) {}
    private static final Map<ItemKey,SceneModel> ITEMS=Collections.synchronizedMap(new LinkedHashMap<>(256,.75F,true) {
        @Override protected boolean removeEldestEntry(Map.Entry<ItemKey,SceneModel> entry){return size()>256;}
    });
    public static void clearCache(){BLOCKS.clear();ITEMS.clear();}
    private final BlockStateModel block;
    private final ItemStackRenderState item;
    private final ItemDisplayContext context;
    private final List<BakedQuad> itemQuads;
    private final TextureAtlasSprite particle;
    private final boolean custom;
    private SceneModel(BlockStateModel block) {
        this.block = block; this.item = null; this.context = ItemDisplayContext.NONE;
        this.itemQuads = List.of(); this.particle = block.particleIcon(); this.custom = false;
    }
    private SceneModel(ItemStackRenderState item, ItemDisplayContext context) {
        this.block = null; this.item = item; this.context = context;
        this.particle = item.pickParticleIcon(RandomSource.create(42));
        SubmitNodeStorage capture = new SubmitNodeStorage();
        item.submit(new PoseStack(), capture, 0, 0, 0);
        List<BakedQuad> quads = new ArrayList<>();
        for (var batch : capture.getSubmitsPerOrder().values()) {
            for (var submit : batch.getItemSubmits()) {
                for (BakedQuad quad : submit.quads()) {
                    quads.add(transformed(quad, submit.pose().pose(), new Vector3f(.5F)));
                }
            }
        }
        this.itemQuads = List.copyOf(quads);
        this.custom = itemQuads.isEmpty() && !item.isEmpty();
    }
    public static SceneModel block(BlockStateModel model) {
        return model instanceof SceneModel scene ? scene : BLOCKS.computeIfAbsent(model, SceneModel::new);
    }
    public static SceneModel item(ItemStack stack, ClientLevel level, ItemDisplayContext context) {
        net.minecraft.client.renderer.item.TrackingItemStackRenderState state = new net.minecraft.client.renderer.item.TrackingItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForTopItem(state, stack, context, level, null, 0);
        if(state.isAnimated())return new SceneModel(state,context);
        ItemKey key=new ItemKey(stack.getItem(),stack.getComponentsPatch(),context,state.getModelIdentity());
        return ITEMS.computeIfAbsent(key,k->new SceneModel(state,context));
    }
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random) {
        if (block == null) return side == null ? itemQuads : List.of();
        List<BakedQuad> quads = new ArrayList<>();
        for (BlockModelPart part : block.collectParts(random)) quads.addAll(part.getQuads(side));
        return quads;
    }
    @Override public void collectParts(RandomSource random, List<BlockModelPart> parts) {
        if (block != null) block.collectParts(random, parts);
        else parts.add(new BlockModelPart() {
            public List<BakedQuad> getQuads(Direction side) { return side == null ? itemQuads : List.of(); }
            public boolean useAmbientOcclusion() { return false; }
            public TextureAtlasSprite particleIcon() { return particle; }
        });
    }
    @Override public TextureAtlasSprite particleIcon() { return particle; }
    public boolean isGui3d() { return block != null || item.usesBlockLight(); }
    public boolean usesBlockLight() { return block != null || item.usesBlockLight(); }
    public boolean isCustomRenderer() { return custom; }
    public boolean useAmbientOcclusion() {
        return block != null && block.collectParts(RandomSource.create(42)).stream().anyMatch(BlockModelPart::useAmbientOcclusion);
    }
    public TextureAtlasSprite getParticleIcon() { return particle; }
    /** Item vertices have already received the chosen display transform during extraction. */
    public ItemTransforms getTransforms() { return ItemTransforms.NO_TRANSFORMS; }
    public static SubmitNodeCollector immediate(MultiBufferSource buffers) { return new ImmediateItems(buffers); }
    public void render(PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (item != null) item.submit(pose, new ImmediateItems(buffers), light, overlay, 0);
        else net.minecraft.client.renderer.block.ModelBlockRenderer.renderModel(pose.last(), buffers.getBuffer(net.minecraft.client.renderer.Sheets.cutoutBlockSheet()), block, 1, 1, 1, light, overlay);
    }
    public void visitExtents(java.util.function.Consumer<org.joml.Vector3fc> consumer) {
        if (item != null) item.visitExtents(consumer);
        else for (Direction side : new Direction[]{null,Direction.DOWN,Direction.UP,Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST})
            for (BakedQuad q : getQuads(null, side, RandomSource.create(42))) for (int i=0;i<4;i++) consumer.accept(q.position(i));
    }
    public static BakedQuad transformed(BakedQuad q, Matrix4f matrix, Vector3f offset) {
        Vector3f[] p = new Vector3f[4];
        for (int i=0;i<4;i++) p[i]=matrix.transformPosition(new Vector3f(q.position(i))).add(offset);
        Vector3f n=matrix.transformDirection(new Vector3f(q.direction().step())).normalize();
        return new BakedQuad(p[0],p[1],p[2],p[3],q.packedUV0(),q.packedUV1(),q.packedUV2(),q.packedUV3(),q.tintIndex(),Direction.getApproximateNearest(n.x,n.y,n.z),q.sprite(),q.shade(),q.lightEmission());
    }
    /** Executes the new item submission API into the scene's own vertex consumers, including special item models. */
    private static final class ImmediateItems extends SubmitNodeStorage {
        private final MultiBufferSource buffers;
        ImmediateItems(MultiBufferSource buffers) { this.buffers=buffers; }
        @Override public void submitItem(PoseStack pose, ItemDisplayContext context, int light, int overlay, int outline, int[] tints, List<BakedQuad> quads, RenderType type, ItemStackRenderState.FoilType foil) {
            ItemRenderer.renderItem(context,pose,buffers,light,overlay,tints,quads,type,foil);
        }
        @Override public <S> void submitModel(net.minecraft.client.model.Model<? super S> model, S state, PoseStack pose, RenderType type, int light, int overlay, int color, TextureAtlasSprite sprite, int outline, net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay crumbling) {
            model.setupAnim(state);
            VertexConsumer consumer=buffers.getBuffer(type);
            if (sprite!=null) consumer=sprite.wrap(consumer);
            model.renderToBuffer(pose,consumer,light,overlay,color);
        }
        @Override public void submitModelPart(net.minecraft.client.model.geom.ModelPart part, PoseStack pose, RenderType type, int light, int overlay, TextureAtlasSprite sprite, boolean sheeted, boolean foil, int color, net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay crumbling, int outline) {
            VertexConsumer consumer=ItemRenderer.getFoilBuffer(buffers,type,sheeted,foil);
            if (sprite!=null) consumer=sprite.wrap(consumer);
            part.render(pose,consumer,light,overlay,color);
        }
        @Override public void submitBlock(PoseStack pose, BlockState state, int light, int overlay, int outline) {
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state,pose,buffers,light,overlay);
        }
        @Override public void submitBlockModel(PoseStack pose, RenderType type, BlockStateModel model, float r,float g,float b,int light,int overlay,int outline) {
            net.minecraft.client.renderer.block.ModelBlockRenderer.renderModel(pose.last(),buffers.getBuffer(type),model,r,g,b,light,overlay);
        }
        @Override public void submitCustomGeometry(PoseStack pose, RenderType type, SubmitNodeCollector.CustomGeometryRenderer renderer) {
            renderer.render(pose.last(),buffers.getBuffer(type));
        }
    }
}
