package com.pockyl.handson_delight.assembly;

import com.pockyl.handson_delight.station.PotLogic;
import com.pockyl.handson_delight.compat.FoodFreshness;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PotMealCapacityTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void unstackableServingsStillAllowBulkPotCooking() {
        ItemStack meal = new ItemStack(Items.MUSHROOM_STEW);
        assertEquals(1, meal.getMaxStackSize());
        assertEquals(64, PotLogic.batchRoom(meal, 0));
        assertEquals(1, PotLogic.batchRoom(meal, 63));
        assertEquals(0, PotLogic.batchRoom(meal, 64));
    }

    @Test void multiItemRecipesAndSmallSnackLimitsPreserveRecipeYield() {
        ItemStack twoMeals = new ItemStack(Items.COOKED_BEEF, 2);
        twoMeals.set(DataComponents.MAX_STACK_SIZE, 1);
        assertEquals(32, PotLogic.batchRoom(twoMeals, 0));
        assertEquals(0, PotLogic.batchRoom(twoMeals, 63));
        ItemStack snack = new ItemStack(Items.COOKIE);
        snack.set(DataComponents.MAX_STACK_SIZE, 4);
        assertEquals(64, PotLogic.batchRoom(snack, 0));
        assertEquals(0, PotLogic.batchRoom(ItemStack.EMPTY, 0));
    }

    @Test void freshnessConversionRoundsTowardOlderAndDoesNotOverflow() {
        assertEquals(50, FoodFreshness.scaledAge(25, 100, 200));
        assertEquals(1, FoodFreshness.scaledAge(1, 100, 30));
        assertEquals(300, FoodFreshness.scaledAge(150, 100, 200));
        assertEquals(Integer.MAX_VALUE, FoodFreshness.scaledAge(Integer.MAX_VALUE, 1, Integer.MAX_VALUE));
        assertEquals(0, FoodFreshness.scaledAge(5, 0, 200));
    }
}
