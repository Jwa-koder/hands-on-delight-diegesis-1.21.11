package com.pockyl.diegesis.mixin.client;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.pockyl.diegesis.impl.client.HiddenBlocks;

/**
 * {@link BlockFaceMixin} for Sodium on NeoForge, whose mesh builder decides on its own which faces a neighbour hides. A Fabric
 * jar needs none: its {@code SodiumWorldSliceMixin} makes a hidden block air for Sodium. The descriptor names Mojang's classes;
 * where they are not (a Fabric jar) nothing is applied.
 */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockOcclusionCache", remap = false)
abstract class SodiumOcclusionMixin {
    @Inject(method = "shouldDrawSide(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/BlockGetter;"
            + "Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void diegesis$showFaceByHiddenBlock(BlockState state, BlockGetter level, BlockPos pos, Direction face,
                                                CallbackInfoReturnable<Boolean> cir) {
        if (HiddenBlocks.isHidden(pos.getX() + face.getStepX(), pos.getY() + face.getStepY(), pos.getZ() + face.getStepZ())) {
            cir.setReturnValue(true);
        }
    }
}
