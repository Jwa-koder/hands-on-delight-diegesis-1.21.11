package com.pockyl.handson_delight.assembly;

import com.google.gson.JsonParser;
import com.pockyl.handson_delight.compat.StackNbt;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RecipePortTest {
    static RegistryAccess registries;
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }
    @Test void shapedFoodRecipeRetainsBlankCellsAndResultCount() {
        var json = JsonParser.parseString("""
            {"type":"minecraft:crafting_shaped","category":"misc","pattern":["B "," C"],
             "key":{"B":"minecraft:bread","C":"minecraft:carrot"},
             "result":{"id":"minecraft:cooked_beef","count":2}}
            """).getAsJsonObject();
        var assembly = AssemblyRecipes.parse(Identifier.parse("test:meal"), json, null, registries).orElseThrow();
        assertArrayEquals(new int[]{0,4}, assembly.slots());
        assertEquals(2, assembly.ingredients().size());
        assertTrue(assembly.ingredients().get(0).test(new ItemStack(Items.BREAD)));
        assertTrue(assembly.ingredients().get(1).test(new ItemStack(Items.CARROT)));
        assertEquals(AssemblyRecipes.Base.BREAD, assembly.kind());
        assertTrue(assembly.result().is(Items.COOKED_BEEF));
        assertEquals(2, assembly.result().getCount());
    }
    @Test void eventStackNbtRetainsCountAndRejectsMalformedInput() {
        var stack = new ItemStack(Items.CARROT, 7);
        var copy = StackNbt.load(registries, StackNbt.save(stack, registries));
        assertTrue(ItemStack.isSameItemSameComponents(stack, copy));
        assertEquals(7, copy.getCount());
        assertTrue(StackNbt.load(registries, new CompoundTag()).isEmpty());
        assertTrue(StackNbt.save(ItemStack.EMPTY, registries).isEmpty());
    }
}
