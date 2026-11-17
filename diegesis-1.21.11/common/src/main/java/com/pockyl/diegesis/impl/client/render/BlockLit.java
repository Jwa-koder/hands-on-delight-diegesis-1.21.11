package com.pockyl.diegesis.impl.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import com.pockyl.diegesis.impl.client.model.SceneModel;
import java.util.*;

/** A scene's block geometry, with vanilla ambient occlusion for quarter-turn aligned parts. */
final class BlockLit {
    private static final float SQUARE=.9999F;
    private static final Map<SceneModel,BlockStateModel[]> TURNED=new WeakHashMap<>();
    private static final Direction[] SIDES={Direction.DOWN,Direction.UP,Direction.NORTH,Direction.SOUTH,Direction.WEST,Direction.EAST,null};
    private BlockLit() {}
    static void draw(SceneModel model,BlockState state,BlockPos pos,PoseStack poses,VertexConsumer consumer,Matrix4f toWorld) {
        Minecraft mc=Minecraft.getInstance();
        var level=mc.level;
        if(level==null)return;
        Matrix4f world=new Matrix4f(toWorld).mul(poses.last().pose());
        Vector3f x=world.transformDirection(new Vector3f(1,0,0)).normalize();
        Vector3f y=world.transformDirection(new Vector3f(0,1,0)).normalize();
        if(y.y>SQUARE && (Math.abs(x.x)>SQUARE || Math.abs(x.z)>SQUARE)) {
            int turns=Math.floorMod(Math.round((float)Math.atan2(-x.z,x.x)/Mth.HALF_PI),4);
            poses.pushPose();poses.translate(.5,0,.5);
            poses.last().pose().rotateY(-turns*Mth.HALF_PI);poses.last().normal().rotateY(-turns*Mth.HALF_PI);
            poses.translate(-.5,0,-.5);
            mc.getBlockRenderer().getModelRenderer().tesselateBlock(level,turned(model,turns).collectParts(RandomSource.create(42)),state,pos,poses,consumer,false,OverlayTexture.NO_OVERLAY);
            poses.popPose();return;
        }
        for(Direction side:SIDES)for(BakedQuad q:model.getQuads(state,side,RandomSource.create(42))) {
            Vector3f normal=world.transformDirection(new Vector3f(q.direction().step())).normalize();
            float shade=q.shade()?diffuse(normal):1;
            BlockPos lit=pos.relative(Direction.getApproximateNearest(normal.x,normal.y,normal.z));
            int light=LevelRenderer.getLightColor(level,level.getBlockState(lit).isSolidRender()?pos:lit);
            consumer.putBulkData(poses.last(),q,shade,shade,shade,1,light,OverlayTexture.NO_OVERLAY);
        }
    }
    private static float diffuse(Vector3f n) {return Math.min(1,n.x*n.x*.6F+n.y*n.y*(3+n.y)/4+n.z*n.z*.8F);}
    private static BlockStateModel turned(SceneModel model,int turns) {
        if(turns==0)return model;
        BlockStateModel[] all=TURNED.computeIfAbsent(model,k->new BlockStateModel[4]);
        if(all[turns]==null) {
            Matrix4f matrix=new Matrix4f().translate(.5F,0,.5F).rotateY(turns*Mth.HALF_PI).translate(-.5F,0,-.5F);
            List<BlockModelPart> parts=new ArrayList<>();
            for(BlockModelPart part:model.collectParts(RandomSource.create(42))) {
                Map<Direction,List<BakedQuad>> faces=new EnumMap<>(Direction.class);List<BakedQuad> unsided=new ArrayList<>();
                for(Direction side:SIDES)for(BakedQuad q:part.getQuads(side)) {
                    BakedQuad turn=SceneModel.transformed(q,matrix,new Vector3f());
                    if(side==null)unsided.add(turn);else faces.computeIfAbsent(turn.direction(),k->new ArrayList<>()).add(turn);
                }
                parts.add(new BlockModelPart() {
                    public List<BakedQuad> getQuads(Direction side){return side==null?unsided:faces.getOrDefault(side,List.of());}
                    public boolean useAmbientOcclusion(){return part.useAmbientOcclusion();}
                    public TextureAtlasSprite particleIcon(){return part.particleIcon();}
                });
            }
            all[turns]=new BlockStateModel() {
                public void collectParts(RandomSource random,List<BlockModelPart> target){target.addAll(parts);}
                public TextureAtlasSprite particleIcon(){return model.particleIcon();}
            };
        }
        return all[turns];
    }
}
