package com.pockyl.diegesis.fabric.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.pockyl.diegesis.impl.client.focus.FocusController;
import com.pockyl.diegesis.impl.client.hud.HudSlide;

/**
 * While the player works at a station its interface replaces the HUD (when the scene says so): each part slides off to its
 * nearest edge, then is not drawn. Fabric has no event per HUD part (NeoForge's layer event does it there); the chat slides by a
 * common mixin, what mods draw through {@code HudRenderCallback} as one part ({@code DiegesisFabricClient}). Since 1.20.5 the
 * level number over the experience bar is a layer of its own.
 */
@Mixin(Gui.class)
abstract class GuiMixin {
    /** The HUD's graphics this frame: the parts' own methods do not all take them first. */
    @Unique
    private GuiGraphics diegesis$graphics;

    @Inject(method = "render", at = @At("HEAD"))
    private void diegesis$frame(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        diegesis$graphics = graphics;
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true, require = 0)
    private void diegesis$noCrosshair(CallbackInfo ci) {
        // The crosshair is where the player aims, and the player no longer aims.
        if (diegesis$hides()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderItemHotbar", at = @At("HEAD"), cancellable = true, require = 0)
    private void diegesis$hotbar(CallbackInfo ci) {
        diegesis$begin(HudSlide.HOTBAR, ci);
    }

    @Inject(method = "renderPlayerHealth", at = @At("HEAD"), cancellable = true, require = 0)
    private void diegesis$health(CallbackInfo ci) {
        diegesis$begin("health", ci);
    }

    @Inject(method = "renderVehicleHealth", at = @At("HEAD"), cancellable = true, require = 0)
    private void diegesis$vehicle(CallbackInfo ci) {
        diegesis$begin("vehicle_health", ci);
    }

    @Inject(method = "renderHotbarAndDecorations", at = @At("HEAD"), cancellable = true)
    private void diegesis$hideHotbarAndBars(CallbackInfo ci) {
        if (diegesis$hides() && HudSlide.gone()) ci.cancel();
    }

    @Inject(method = "renderHotbarAndDecorations", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;renderBackground(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"))
    private void diegesis$contextualBegin(CallbackInfo ci) {
        if (diegesis$graphics != null) HudSlide.begin("experience", diegesis$graphics);
    }

    @Inject(method = "renderHotbarAndDecorations", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/client/gui/contextualbar/ContextualBarRenderer;render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V", shift = At.Shift.AFTER))
    private void diegesis$contextualEnd(CallbackInfo ci) {
        if (diegesis$graphics != null) HudSlide.end(diegesis$graphics);
    }

    @Inject(method = "renderSelectedItemName", at = @At("HEAD"), cancellable = true, require = 0)
    private void diegesis$itemName(CallbackInfo ci) {
        diegesis$begin("item_name", ci);
    }

    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true, require = 0)
    private void diegesis$effects(CallbackInfo ci) {
        diegesis$begin("effects", ci);
    }

    @Inject(method = "displayScoreboardSidebar", at = @At("HEAD"), cancellable = true, require = 0)
    private void diegesis$scoreboard(CallbackInfo ci) {
        diegesis$begin("scoreboard", ci);
    }

    @Inject(method = {"renderItemHotbar", "renderPlayerHealth", "renderVehicleHealth", "renderSelectedItemName", "renderEffects", "displayScoreboardSidebar"}, at = @At("RETURN"),
            require = 0)
    private void diegesis$drawn(CallbackInfo ci) {
        if (diegesis$graphics != null) {
            HudSlide.end(diegesis$graphics);
        }
    }

    /** A part starts: not drawn once the HUD is off the screen, else drawn where the slide has it. */
    @Unique
    private void diegesis$begin(String key, CallbackInfo ci) {
        if (diegesis$hides() && HudSlide.gone()) {
            ci.cancel();
        } else if (diegesis$graphics != null) {
            HudSlide.begin(key, diegesis$graphics);
        }
    }

    @Unique
    private static boolean diegesis$hides() {
        FocusController focus = FocusController.get();
        return focus.isActive() && focus.options().hideHud();
    }
}
