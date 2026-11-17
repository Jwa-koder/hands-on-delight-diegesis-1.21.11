package com.pockyl.handson_delight.compat;

import com.pockyl.handson_delight.platform.Platform;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Optional freshness adapters: Medieval Logistics' real-time harvest stamp and Spoiled's timer. Never discards
 * other item components or rejuvenates merged food: when two ages merge, the older one is kept.
 */
public final class FoodFreshness {
    private FoodFreshness() {}
    private static final net.minecraft.resources.Identifier MEDIEVAL_STAMP = net.minecraft.resources.Identifier.fromNamespaceAndPath("medieval_logistics", "freshness");

    public static boolean sameFood(ItemStack first, ItemStack second) {
        if (ItemStack.isSameItemSameComponents(first, second)) return true;
        if (!ItemStack.isSameItem(first, second)) return false;
        ItemStack a = first.copy(), b = second.copy();
        var stamp = medievalStamp();
        if (stamp != null) { a.remove(stamp); b.remove(stamp); }
        if (ItemStack.isSameItemSameComponents(a, b)) return true;
        return Platform.INSTANCE.isLoaded("spoiled") && SpoiledBridge.sameIgnoringTimer(a, b);
    }

    public static void olderInto(ItemStack target, ItemStack source) {
        if (target.isEmpty() || source.isEmpty()) return;
        var stamp = medievalStamp();
        if (stamp != null) {
            Long from = source.get(stamp);
            if (from != null && ItemStack.isSameItem(target, source)) {
                Long current = target.get(stamp);
                target.set(stamp, current == null ? from : Math.min(current, from));
            }
        }
        if (Platform.INSTANCE.isLoaded("spoiled")) SpoiledBridge.olderInto(target, source);
    }

    /** Medieval Logistics stores the harvest hour as a plain Long, so no compile-time dependency is needed. */
    @SuppressWarnings("unchecked")
    private static net.minecraft.core.component.DataComponentType<Long> medievalStamp() {
        if (!Platform.INSTANCE.isLoaded("medieval_logistics")) return null;
        return (net.minecraft.core.component.DataComponentType<Long>) net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE
                .getValue(MEDIEVAL_STAMP);
    }

    /** A newly prepared dish inherits the worst ingredient's fractional age, using its own shelf life. */
    public static ItemStack prepared(ItemStack result, Level level, List<ItemStack> ingredients) {
        if (result.isEmpty() || level == null || !Platform.INSTANCE.isLoaded("spoiled")) return result;
        ItemStack copy = result.copy();
        SpoiledBridge.initialize(copy, level);
        for (ItemStack ingredient : ingredients) olderInto(copy, ingredient);
        return copy;
    }

    public static int scaledAge(int sourceAge, int sourceLifetime, int targetLifetime) {
        if (sourceAge <= 0 || sourceLifetime <= 0 || targetLifetime <= 0) return 0;
        // Round upward: moving food between recipes cannot gain freshness through rounding.
        long age = ((long) sourceAge * targetLifetime + sourceLifetime - 1) / sourceLifetime;
        return (int) Math.min(Integer.MAX_VALUE, age);
    }

    /** Separate class stays unloaded when Spoiled is absent. */
    private static final class SpoiledBridge {
        static boolean sameIgnoringTimer(ItemStack first, ItemStack second) {
            var timer = com.mrbysco.spoiled.registration.SpoiledComponents.SPOIL_TIMER.get();
            ItemStack a = first.copy(), b = second.copy();
            a.remove(timer); b.remove(timer);
            return ItemStack.isSameItemSameComponents(a, b);
        }

        static void initialize(ItemStack result, Level level) {
            var recipe = com.mrbysco.spoiled.util.SpoilHelper.getSpoilRecipe(level, result);
            if (recipe != null) {
                var type = com.mrbysco.spoiled.registration.SpoiledComponents.SPOIL_TIMER.get();
                if (!result.has(type)) result.set(type, new com.mrbysco.spoiled.component.SpoilTimer(recipe.value().getSpoilTime()));
            }
        }

        static void olderInto(ItemStack target, ItemStack source) {
            var type = com.mrbysco.spoiled.registration.SpoiledComponents.SPOIL_TIMER.get();
            var age = source.get(type);
            if (age == null) return;
            var current = target.get(type);
            if (current == null) {
                // Same-kind ingredient groups: copying a timer retains age and never aliases mutable state.
                if (ItemStack.isSameItem(target, source)) target.set(type, age);
                return;
            }
            int worst = Math.max(current.timer(), scaledAge(age.timer(), age.maxTime(), current.maxTime()));
            target.set(type, new com.mrbysco.spoiled.component.SpoilTimer(worst, current.maxTime()));
        }
    }
}
