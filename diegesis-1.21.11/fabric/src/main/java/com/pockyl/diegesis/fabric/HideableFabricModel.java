package com.pockyl.diegesis.fabric;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBlockStateModel;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import com.pockyl.diegesis.impl.client.HiddenBlocks;
import java.util.List;
import java.util.function.Predicate;
/** Position-aware hiding while retaining the complete block model for scene copies and other consumers. */
final class HideableFabricModel implements BlockStateModel,FabricBlockStateModel {
    private final BlockStateModel wrapped;
    HideableFabricModel(BlockStateModel model){wrapped=model;}
    public void collectParts(RandomSource random,List<BlockModelPart> parts){wrapped.collectParts(random,parts);}
    public TextureAtlasSprite particleIcon(){return wrapped.particleIcon();}
    @Override public void emitQuads(QuadEmitter emitter,BlockAndTintGetter level,BlockPos pos,BlockState state,RandomSource random,Predicate<Direction> cullTest) {
        if(!HiddenBlocks.isHidden(pos)) {
            HiddenBlocks.built(pos);wrapped.emitQuads(emitter,level,pos,state,random,cullTest);
        }
    }
    @Override public Object createGeometryKey(BlockAndTintGetter level,BlockPos pos,BlockState state,RandomSource random) {
        Object key=wrapped.createGeometryKey(level,pos,state,random);
        return key==null?null:new GeometryKey(key,HiddenBlocks.isHidden(pos));
    }
    @Override public TextureAtlasSprite particleSprite(BlockAndTintGetter level,BlockPos pos,BlockState state){return wrapped.particleSprite(level,pos,state);}
    private record GeometryKey(Object original,boolean hidden){}
}
