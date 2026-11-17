package com.pockyl.diegesis.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import com.pockyl.diegesis.api.station.StationType;
import com.pockyl.diegesis.impl.StationRegistry;
import com.pockyl.diegesis.impl.client.DiegesisClientRuntime;
import com.pockyl.diegesis.impl.client.debug.DebugView;
import com.pockyl.diegesis.impl.client.hud.HudSlide;
import com.pockyl.diegesis.impl.client.render.SceneRenderer;
import com.pockyl.diegesis.impl.client.scene.SceneLibrary;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

public final class DiegesisFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        DiegesisClientRuntime.init();
        FabricNetwork.registerClient();
        ClientTickEvents.END_CLIENT_TICK.register(client -> DiegesisClientRuntime.tick());
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> dispatcher.register(
                ClientCommandManager.literal("diegesis").then(ClientCommandManager.literal("debug").executes(command -> {
                    DebugView.toggle();
                    return 1;
                }))));
        // What mods draw through the HUD hook slides away with the HUD as one part, around everybody's drawing.
        Identifier beforeHud = Identifier.fromNamespaceAndPath("diegesis", "hud_slide_begin");
        Identifier afterHud = Identifier.fromNamespaceAndPath("diegesis", "hud_slide_end");
        HudRenderCallback.EVENT.addPhaseOrdering(beforeHud, Event.DEFAULT_PHASE);
        HudRenderCallback.EVENT.addPhaseOrdering(Event.DEFAULT_PHASE, afterHud);
        HudRenderCallback.EVENT.register(beforeHud, (graphics, deltaTracker) -> HudSlide.begin("fabric:hud", graphics));
        HudRenderCallback.EVENT.register(afterHud, (graphics, deltaTracker) -> HudSlide.end(graphics));
        WorldRenderEvents.AFTER_ENTITIES.register(context -> SceneRenderer.render(context.matrices(), SceneRenderer.currentView(),
                SceneRenderer.currentProjection(), context.gameRenderer().getMainCamera()));
        WorldRenderEvents.END_MAIN.register(context -> SceneRenderer.renderCounts());
        net.fabricmc.fabric.api.resource.v1.ResourceLoader.get(PackType.CLIENT_RESOURCES)
                .registerReloader(SceneLibrary.ID, SceneLibrary.INSTANCE);
        PreparableModelLoadingPlugin.register(
                (store, executor) -> CompletableFuture.supplyAsync(() -> SceneLibrary.modelsToLoad(store.resourceManager()), executor),
                (Set<Identifier> models, ModelLoadingPlugin.Context context) -> {
                    for(Identifier id:models) {
                        ExtraModelKey<BlockStateModel> key=FabricClientPlatform.MODELS.computeIfAbsent(id,k->ExtraModelKey.create(k::toString));
                        context.addModel(key,SimpleUnbakedExtraModel.blockStateModel(id));
                    }
                    context.modifyBlockModelAfterBake().register((model, modify) -> {
                        boolean hideable=StationRegistry.all().stream().filter(StationType::hidesBlock)
                            .anyMatch(type->type.blocks().contains(modify.state().getBlock()) || type.lateBlocks().contains(modify.state().getBlock()));
                        return hideable?new HideableFabricModel(model):model;
                    });
                });
    }
}
