package com.pockyl.diegesis.port;

import com.pockyl.diegesis.api.state.*;
import com.pockyl.diegesis.impl.network.Packets;
import io.netty.buffer.Unpooled;
import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Quaternionf;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PortStateTest {
    private static RegistryAccess registries;
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }
    private StationSchema schema() {
        return StationSchema.builder().slots("ingredients", 2)
            .control("heat", ControlSpec.range(0, 100, 20)).tank("water", TankSpec.of(1000)).build();
    }
    private StationState example() {
        var state = new StationState(schema());
        state.setItem(new SlotRef("ingredients", 1), new ItemStack(Items.CARROT, 7));
        state.setControl("heat", 65);
        state.setTank("water", TankContents.of(Identifier.parse("minecraft:water"), 625));
        state.setPose(new SlotRef("ingredients", 1), new ItemPose(1, 2, 3, 4, new Quaternionf(), 1, -1));
        var tag = new CompoundTag(); tag.putString("recipe", "test:soup"); state.setData(tag);
        return state;
    }
    private void assertRestored(StationState state) {
        assertTrue(state.getItem(new SlotRef("ingredients", 0)).isEmpty());
        assertTrue(state.getItem(new SlotRef("ingredients", 1)).is(Items.CARROT));
        assertEquals(7, state.getItem(new SlotRef("ingredients", 1)).getCount());
        assertEquals(65, state.getControl("heat"));
        assertEquals(625, state.getTank("water").amount());
        assertEquals("test:soup", state.data().getStringOr("recipe", ""));
        assertNotNull(state.getPose(new SlotRef("ingredients", 1)));
        assertEquals(-1, state.getPose(new SlotRef("ingredients", 1)).on());
    }
    @Test void nbtRoundTripRetainsItemsControlsTanksAndPoses() {
        var copy = new StationState(schema()); copy.load(example().save(registries), registries); assertRestored(copy);
    }
    @Test void fullNetworkRoundTripRetainsStationState() {
        var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        try {
            example().writeChanges(buf, true);
            var copy = new StationState(schema()); copy.readChanges(buf); assertRestored(copy);
            assertEquals(0, buf.readableBytes());
        } finally { buf.release(); }
    }
    @Test void invalidAndMissingPoseDataIsRejected() {
        assertNull(ItemPose.load(new CompoundTag()));
        var tag = new ItemPose(0, 0, 0, 1, new Quaternionf(), 1, -1).save();
        tag.putFloat("qx", Float.NaN);
        assertNull(ItemPose.load(tag));
    }
    @Test void recipeRequestRoundTripPreservesMenuAndRecipe() {
        var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        try {
            var request = new Packets.PlaceRecipe(3, Identifier.parse("test:soup"), true);
            request.write(buf);
            assertEquals(request, Packets.readClient(buf));
            assertEquals(0, buf.readableBytes());
        } finally { buf.release(); }
    }
}
