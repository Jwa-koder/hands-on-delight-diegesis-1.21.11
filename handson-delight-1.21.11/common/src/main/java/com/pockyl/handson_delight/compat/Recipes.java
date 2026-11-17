package com.pockyl.handson_delight.compat;

import net.fabricmc.fabric.api.recipe.v1.FabricRecipeManager;
import net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import java.util.*;
import java.lang.ref.WeakReference;

/** Accesses authoritative server recipes or Fabric's explicitly synchronized client recipes. */
public final class Recipes {
    private static final Map<RecipeAccess, View> VIEWS = Collections.synchronizedMap(new WeakHashMap<>());
    private Recipes() {}
    public static View manager(Level level) {
        return VIEWS.computeIfAbsent(level.recipeAccess(), View::new);
    }
    public static Map<Identifier, RecipeHolder<?>> byName(RecipeManager manager) {
        Map<Identifier, RecipeHolder<?>> result = new HashMap<>();
        for (RecipeHolder<?> holder : manager.getRecipes()) result.put(holder.id().identifier(), holder);
        return result;
    }
    public static void registerSync() {
        RecipeSynchronization.synchronizeRecipeSerializer(RecipeSerializer.SHAPED_RECIPE);
        RecipeSynchronization.synchronizeRecipeSerializer(RecipeSerializer.SHAPELESS_RECIPE);
        RecipeSynchronization.synchronizeRecipeSerializer(RecipeSerializer.SMELTING_RECIPE);
        RecipeSynchronization.synchronizeRecipeSerializer(RecipeSerializer.BLASTING_RECIPE);
        RecipeSynchronization.synchronizeRecipeSerializer(RecipeSerializer.SMOKING_RECIPE);
        RecipeSynchronization.synchronizeRecipeSerializer(RecipeSerializer.CAMPFIRE_COOKING_RECIPE);
    }
    public static net.minecraft.world.item.ItemStack result(Recipe<?> recipe, Level level) {
        return result(recipe, level.registryAccess());
    }
    public static net.minecraft.world.item.ItemStack result(Recipe<?> recipe, net.minecraft.core.HolderLookup.Provider registries) {
        if (recipe instanceof vectorwing.farmersdelight.common.crafting.CookingPotRecipe pot) return pot.result().copy();
        var context = new net.minecraft.util.context.ContextMap.Builder()
                .withParameter(net.minecraft.world.item.crafting.display.SlotDisplayContext.REGISTRIES, registries)
                .create(new net.minecraft.util.context.ContextKeySet.Builder()
                        .required(net.minecraft.world.item.crafting.display.SlotDisplayContext.REGISTRIES).build());
        return recipe.display().stream().map(display -> display.result().resolveForFirstStack(context))
                .filter(stack -> !stack.isEmpty()).findFirst().orElse(net.minecraft.world.item.ItemStack.EMPTY);
    }
    public static List<Ingredient> ingredients(Recipe<?> recipe) {
        if (recipe instanceof vectorwing.farmersdelight.common.crafting.CookingPotRecipe pot) return pot.input();
        if (recipe instanceof vectorwing.farmersdelight.common.crafting.CuttingBoardRecipe cutting) return List.of(cutting.getInput());
        if (recipe instanceof ShapedRecipe shaped) return shaped.getIngredients().stream().flatMap(Optional::stream).toList();
        return recipe.placementInfo().ingredients();
    }
    public static net.minecraft.world.item.ItemStack[] items(Ingredient ingredient) {
        if (ingredient == null) return new net.minecraft.world.item.ItemStack[0];
        return ingredient.items().map(net.minecraft.world.item.ItemStack::new).toArray(net.minecraft.world.item.ItemStack[]::new);
    }
    public static final class View {
        private final WeakReference<RecipeAccess> reference;
        private View(RecipeAccess access) { reference = new WeakReference<>(access); }
        public Collection<RecipeHolder<?>> getRecipes() {
            RecipeAccess access = reference.get();
            if (access == null) return List.of();
            if (access instanceof RecipeManager manager) return manager.getRecipes();
            return ((FabricRecipeManager) access).getSynchronizedRecipes().recipes();
        }
        @SuppressWarnings("unchecked")
        public <T extends Recipe<?>> List<RecipeHolder<T>> getAllRecipesFor(RecipeType<T> type) {
            return getRecipes().stream().filter(h -> h.value().getType() == type).map(h -> (RecipeHolder<T>) h).toList();
        }
        public <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> getRecipeFor(RecipeType<T> type, I input, Level level) {
            return getAllRecipesFor(type).stream().filter(h -> h.value().matches(input, level)).findFirst();
        }
        public Optional<RecipeHolder<?>> byKey(Identifier id) {
            ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, id);
            return byKey(key);
        }
        public Optional<RecipeHolder<?>> byKey(ResourceKey<Recipe<?>> key) {
            return getRecipes().stream().filter(h -> h.id().equals(key)).findFirst();
        }
    }
}
