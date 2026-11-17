package com.pockyl.handson_delight.mixin;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** Kept as source reference only; runtime code uses RecipeManager.getRecipes(). */
@Mixin(RecipeManager.class)
public interface RecipeManagerAccessor {
    @Accessor("recipes") RecipeMap handson$recipes();
}
