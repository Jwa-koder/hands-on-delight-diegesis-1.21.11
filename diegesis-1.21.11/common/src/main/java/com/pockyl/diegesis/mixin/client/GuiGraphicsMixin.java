package com.pockyl.diegesis.mixin.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.pockyl.diegesis.impl.client.hud.HudSlide;

/** Measures HUD drawing in the GUI's extraction phase, before the pose is copied into its render state. */
@Mixin(GuiGraphics.class)
abstract class GuiGraphicsMixin {
    @Inject(method = "submitBlit", at = @At("HEAD"))
    private void diegesis$blit(RenderPipeline pipeline, com.mojang.blaze3d.textures.GpuTextureView texture,
            com.mojang.blaze3d.textures.GpuSampler sampler, int x0, int y0, int x1, int y1,
            float u0, float u1, float v0, float v1, int color, CallbackInfo ci) {
        HudSlide.include(self().pose(), x0, y0, x1, y1);
    }

    @Inject(method = "submitTiledBlit", at = @At("HEAD"))
    private void diegesis$tiledBlit(RenderPipeline pipeline, com.mojang.blaze3d.textures.GpuTextureView texture,
            com.mojang.blaze3d.textures.GpuSampler sampler, int tileWidth, int tileHeight, int x0, int y0, int x1, int y1,
            float u0, float u1, float v0, float v1, int color, CallbackInfo ci) {
        HudSlide.include(self().pose(), x0, y0, x1, y1);
    }

    @Inject(method = "submitColoredRectangle", at = @At("HEAD"))
    private void diegesis$rectangle(RenderPipeline pipeline, TextureSetup textures, int x0, int y0, int x1, int y1,
            int color, Integer bottomColor, CallbackInfo ci) {
        HudSlide.include(self().pose(), x0, y0, x1, y1);
    }

    @Inject(method = "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V", at = @At("HEAD"))
    private void diegesis$text(Font font, String text, int x, int y, int color, boolean shadow, CallbackInfo ci) {
        if (text != null) HudSlide.include(self().pose(), x, y, x + font.width(text), y + font.lineHeight);
    }

    @Inject(method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V", at = @At("HEAD"))
    private void diegesis$sequence(Font font, FormattedCharSequence text, int x, int y, int color, boolean shadow, CallbackInfo ci) {
        if (text != null) HudSlide.include(self().pose(), x, y, x + font.width(text), y + font.lineHeight);
    }

    @Inject(method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V",
            at = @At("HEAD"))
    private void diegesis$item(LivingEntity entity, Level level, ItemStack stack, int x, int y, int seed, CallbackInfo ci) {
        HudSlide.include(self().pose(), x, y, x + 16, y + 16);
    }

    private GuiGraphics self() { return (GuiGraphics) (Object) this; }
}
