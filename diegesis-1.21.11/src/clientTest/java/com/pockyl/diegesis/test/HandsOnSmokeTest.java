package com.pockyl.diegesis.test;

import com.pockyl.diegesis.api.Diegesis;
import com.pockyl.diegesis.api.client.DiegesisClient;
import com.pockyl.diegesis.impl.client.focus.FocusController;
import com.pockyl.diegesis.impl.client.focus.FocusScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/** Actual core addon stations, loaded only by the opt-in integration test run. */
public final class HandsOnSmokeTest implements FabricClientGameTest {
    private static final BlockPos POS = new BlockPos(0,-60,0);
    @Override public void runTest(ClientGameTestContext context) {
        if (!FabricLoader.getInstance().isModLoaded("handson_delight")) return;
        try (var game = context.worldBuilder().create()) {
            game.getServer().runCommand("gamemode creative @a");
            game.getServer().runCommand("time set noon");
            for (String station : new String[]{"cooking_pot", "cutting_board", "skillet", "stove"}) {
                String block = "farmersdelight:" + station;
                context.runOnClient(client -> {
                    if (BuiltInRegistries.BLOCK.getOptional(Identifier.parse(block)).isEmpty())
                        throw new AssertionError("Missing addon block " + block);
                });
                game.getServer().runCommand("setblock 0 -60 0 minecraft:air");
                game.getServer().runCommand("setblock 0 -61 0 " + (station.equals("cooking_pot") || station.equals("skillet") ? "minecraft:campfire" : "minecraft:stone"));
                game.getServer().runCommand("setblock 0 -60 0 " + block);
                game.getServer().runCommand("item replace entity @a weapon.mainhand with minecraft:air");
                game.getServer().runCommand("tp @a 0.5 -60 2.5 180 38");
                game.getClientWorld().waitForChunksRender();
                context.waitTicks(10);
                context.waitFor(client -> client.player != null && client.player.getMainHandItem().isEmpty(),200);
                context.runOnClient(client -> {
                    client.options.pauseOnLostFocus=false;
                    client.setScreen(null);
                    var type=Diegesis.stationFor(client.level.getBlockState(POS));
                    if(type.isEmpty()) throw new AssertionError("No station registered for " + block);
                });
                context.getInput().pressKey(options -> options.keyUse);
                context.waitFor(client -> client.screen instanceof FocusScreen,200);
                context.waitFor(client -> FocusController.get().isInteractive(),200);
                context.waitTicks(15);
                context.runOnClient(client -> {
                    var view=DiegesisClient.focusedStation().orElseThrow();
                    if(!view.pos().equals(POS)) throw new AssertionError("Wrong focused position for " + block);
                });
                context.takeScreenshot("handson-"+station);
                if (station.equals("cooking_pot")) {
                    game.getServer().runCommand("give @a farmersdelight:rice 8");
                    game.getServer().runCommand("give @a minecraft:bowl 8");
                    context.waitTicks(10);
                    context.runOnClient(client -> {
                        // Invoke the real recipe-book selection path, including its key serialization.
                        try {
                            var view=DiegesisClient.focusedStation().orElseThrow();
                            Class<?> type=Class.forName("com.pockyl.handson_delight.client.PotBook");
                            var constructor=type.getDeclaredConstructor(int.class); constructor.setAccessible(true);
                            Object book=constructor.newInstance(6);
                            var pagesMethod=type.getDeclaredMethod("pages",com.pockyl.diegesis.api.client.StationView.class);
                            pagesMethod.setAccessible(true);
                            Object rice=null;
                            for(Object page:(java.util.List<?>)pagesMethod.invoke(book,view)) {
                                var keyMethod=page.getClass().getDeclaredMethod("key");keyMethod.setAccessible(true);
                                Object key=keyMethod.invoke(page);
                                if(Identifier.parse("farmersdelight:cooking/cooked_rice").equals(key)) {rice=page;break;}
                            }
                            if(rice==null)throw new AssertionError("Cooked rice page missing or recipe-book key not an Identifier");
                            var picked=type.getDeclaredMethod("picked",com.pockyl.diegesis.api.client.StationView.class,rice.getClass());
                            picked.setAccessible(true);picked.invoke(book,view,rice);
                        } catch (ReflectiveOperationException error) {
                            throw new AssertionError("Real pot recipe-book selection failed", error);
                        }
                    });
                    context.waitFor(client -> DiegesisClient.focusedStation().map(view ->
                        java.util.Arrays.stream(view.state().getItems("ingredients")).anyMatch(stack -> !stack.isEmpty())).orElse(false),200);
                    boolean cooked=false;
                    for(int tick=0;tick<180;tick++) {
                        final int at=tick;
                        context.runOnClient(client -> {
                            CompoundTag stir=new CompoundTag();
                            stir.putFloat("x",(float)Math.cos(at*.4));stir.putFloat("z",(float)Math.sin(at*.4));
                            stir.putFloat("at",client.level.getGameTime());
                            DiegesisClient.focusedStation().orElseThrow().sendAction("stir",stir);
                        });
                        context.waitTicks(4);
                        cooked=context.computeOnClient(client -> !DiegesisClient.focusedStation().orElseThrow().state()
                            .getItem(new com.pockyl.diegesis.api.state.SlotRef("meal",0)).isEmpty());
                        if(cooked) break;
                    }
                    if(!cooked) throw new AssertionError("Stirred rice failed to cook in the real pot");
                    context.takeScreenshot("handson-cooked-rice");
                }
                context.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
                context.waitFor(client -> !DiegesisClient.isFocused(),200);
                context.waitTicks(5);
            }
            // ClientStation contains addon callback exceptions to keep playing; those must still fail this test.
            try {
                var log = java.nio.file.Files.readString(FabricLoader.getInstance().getGameDir().resolve("logs/latest.log"));
                if (log.lines().anyMatch(line -> line.contains("Client code of station handson_delight:") && line.contains("failed")))
                    throw new AssertionError("An addon station callback failed; inspect logs/latest.log");
            } catch (java.io.IOException error) {
                throw new AssertionError("Could not inspect station callback errors", error);
            }
        }
    }
}
