package com.pockyl.handson_delight.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.pockyl.handson_delight.assembly.AssemblyRecipes;

import java.util.HashMap;
import java.util.Map;

/**
 * Food put together on the cutting board: its recipes are found before the manager reads them and kept for the board as well. They
 * stay in the manager, for automation; a player's crafting grid does not make them ({@code CraftingMenuMixin}).
 */
@Mixin(RecipeManager.class)
abstract class RecipeManagerMixin {
    @Shadow
    @Final
    private HolderLookup.Provider registries;

    @Unique
    private static final String APPLY =
            "apply(Lnet/minecraft/world/item/crafting/RecipeMap;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V";

    @Unique
    private ResourceManager handson$resources;

    @Unique
    private final Map<Identifier, JsonObject> handson$taken = new HashMap<>();

    @Inject(method = APPLY, at = @At("HEAD"))
    private void handson$findAssembled(net.minecraft.world.item.crafting.RecipeMap recipes, ResourceManager resources, ProfilerFiller profiler,
            CallbackInfo callback) {
        handson$taken.clear();
        Map<Identifier, JsonElement> json = new HashMap<>();
        resources.listResources("recipe", id -> id.getPath().endsWith(".json")).forEach((file, resource) -> {
            String path = file.getPath();
            Identifier id = Identifier.fromNamespaceAndPath(file.getNamespace(), path.substring("recipe/".length(), path.length() - 5));
            try (var reader = resource.openAsReader()) {
                json.put(id, com.google.gson.JsonParser.parseReader(reader));
            } catch (Exception exception) {
                org.slf4j.LoggerFactory.getLogger("HandsOnDelight").warn("Could not inspect board recipe {}", id, exception);
            }
        });
        AssemblyRecipes.collect(json, handson$taken);
    }

    @Inject(method = APPLY, at = @At("TAIL"))
    private void handson$keepAssembled(net.minecraft.world.item.crafting.RecipeMap recipes, ResourceManager resources, ProfilerFiller profiler,
            CallbackInfo callback) {
        // Since 1.21.2 this stage precedes tag binding. Keep the raw candidates
        // until vanilla has finalized recipes against the bound registry tags.
        handson$resources = resources;
    }

    @Inject(method = "finalizeRecipeLoading", at = @At("TAIL"))
    private void handson$loadAfterTags(net.minecraft.world.flag.FeatureFlagSet flags, CallbackInfo callback) {
        if (handson$resources == null) return;
        AssemblyRecipes.load(handson$taken, (RecipeManager) (Object) this, registries, handson$resources);
        handson$taken.clear();
        handson$resources = null;
    }
}
