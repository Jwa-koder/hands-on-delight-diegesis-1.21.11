package com.pockyl.diegesis.test;

import com.pockyl.diegesis.api.Diegesis;
import com.pockyl.diegesis.api.client.DiegesisClient;
import com.pockyl.diegesis.api.client.StationBehavior;
import com.pockyl.diegesis.api.client.StationView;
import com.pockyl.diegesis.api.scene.*;
import com.pockyl.diegesis.api.station.StationLogic;
import com.pockyl.diegesis.api.station.StationContext;
import com.pockyl.diegesis.impl.client.focus.FocusController;
import com.pockyl.diegesis.impl.client.focus.FocusScreen;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import java.util.concurrent.atomic.AtomicInteger;

/** Exercises the actual render loop, camera focus, gameplay use hook and two-way server packets. */
public final class StationSmokeTest implements ModInitializer, ClientModInitializer, FabricClientGameTest {
    private static final Identifier ID = Identifier.fromNamespaceAndPath("diegesis_smoke_test", "table");
    private static final BlockPos POS = new BlockPos(0,-60,0);
    private static final AtomicInteger PAINTED = new AtomicInteger();

    @Override public void onInitialize() {
        Diegesis.station(ID).blocks(Blocks.CRAFTING_TABLE).scene(ID).hidesBlock().logic(station -> new StationLogic() {
            @Override public void onCustomAction(StationContext station, Player player, String action, CompoundTag data) {
                if (action.equals("ping")) {
                    CompoundTag reply = new CompoundTag();
                    reply.putInt("reply", data.getIntOr("value",0));
                    station.state().setData(reply);
                }
            }
        }).register();
    }

    @Override public void onInitializeClient() {
        DiegesisClient.registerScene(ID, SceneDefinition.builder()
                .camera(CameraRig.of(new Vector3f(8,42,42),new Vector3f(8,16,8)))
                .idleVisible(false)
                .node(NodeDefinition.builder("table").at(8,0,8).visual(Visual.ofBlock(Blocks.CRAFTING_TABLE.defaultBlockState()).asLitAsBlock()).build())
                .node(NodeDefinition.builder("apple").at(8,22,8).visual(Visual.ofItem(Items.APPLE)).light(LightDefinition.of(12,24)).build())
                .node(NodeDefinition.builder("glass").at(19,16,8).visual(Visual.ofBlock(Blocks.GLASS.defaultBlockState()).asTranslucent()).build())
                .node(NodeDefinition.builder("shape").at(-5,20,8).build())
                .node(NodeDefinition.builder("painted").build())
                .build());
        DiegesisClient.registerBehavior(ID, view -> new StationBehavior() {
            @Override public void onSceneReady(StationView view) {
                var shape = com.pockyl.diegesis.api.client.MeshShape.builder()
                        .corner(-4,0,0,0,1,0,0,1).corner(4,0,0,1,1,0,0,1)
                        .corner(4,8,0,1,0,0,0,1).corner(-4,8,0,0,0,0,0,1).build();
                view.node("shape").orElseThrow().setShape(shape,Identifier.fromNamespaceAndPath("minecraft","item/apple"));
                view.node("painted").orElseThrow().setPainter((pose,buffers,light) -> PAINTED.incrementAndGet());
            }
        });
    }

    @Override public void runTest(ClientGameTestContext context) {
        context.takeScreenshot("diegesis-title");
        try (var game = context.worldBuilder().create()) {
            game.getServer().runCommand("gamemode creative @a");
            game.getServer().runCommand("setblock 0 -60 0 minecraft:crafting_table");
            game.getServer().runCommand("tp @a 0.5 -60 2.5 180 30");
            game.getServer().runCommand("time set noon");
            game.getClientWorld().waitForChunksRender();
            context.waitTicks(10);
            context.runOnClient(client -> {
                if (!client.level.getBlockState(POS).is(Blocks.CRAFTING_TABLE)) throw new AssertionError("Fixture block missing");
                client.options.pauseOnLostFocus = false;
                client.setScreen(null);
            });
            context.getInput().pressKey(options -> options.keyUse);
            context.waitFor(client -> client.screen instanceof FocusScreen, 200);
            context.waitFor(client -> FocusController.get().isInteractive(), 200);
            context.waitTicks(20);
            context.runOnClient(client -> {
                if (PAINTED.get() == 0) throw new AssertionError("No station scene render frames observed");
                if (!DiegesisClient.isFocused()) throw new AssertionError("Camera never entered station focus");
                var view = DiegesisClient.focusedStation().orElseThrow();
                if (!view.pos().equals(POS)) throw new AssertionError("Focused wrong station: " + view.pos());
                CompoundTag ping = new CompoundTag(); ping.putInt("value",42);
                view.sendAction("ping",ping);
            });
            context.waitFor(client -> DiegesisClient.focusedStation().map(view -> view.state().data().getIntOr("reply",0) == 42).orElse(false),200);
            context.takeScreenshot("diegesis-focused-station");
            context.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
            context.waitFor(client -> !DiegesisClient.isFocused(),200);
            context.waitTicks(10);
            context.takeScreenshot("diegesis-after-close");
        }
    }
}
