package com.pockyl.diegesis.impl.client.focus;

import net.minecraft.client.Minecraft;

import com.pockyl.diegesis.platform.Platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Mouse Tweaks changes the mouse in every vanilla container screen; a station is no such screen, so a player used to it found
 * none of it there. With Mouse Tweaks installed its tweaks work at stations too, each as its own config file has it
 * ({@code config/MouseTweaks.cfg}, read once): the right drag puts one item into every slot it passes, again on a slot passed
 * again; a stack picked up with the left button gathers the same items from the slots it is dragged over (with Shift held it
 * shift-clicks them); Shift with an empty hand and the left button shift-clicks every slot dragged over. Its wheel is
 * Diegesis' own wheel already. Without Mouse Tweaks nothing here is on.
 */
final class MouseTweaksMode {
    private static boolean read;
    private static boolean rmb;
    private static boolean lmbWithItem;
    private static boolean lmbWithoutItem;

    private MouseTweaksMode() {
    }

    /** The right drag that puts one into each slot passed, as many times as it is passed. */
    static boolean rmb() {
        read();
        return rmb;
    }

    /** A stack picked up with the left button gathers the same items from the slots it is dragged over. */
    static boolean lmbWithItem() {
        read();
        return lmbWithItem;
    }

    /** Shift and the left button with an empty hand shift-click every slot dragged over. */
    static boolean lmbWithoutItem() {
        read();
        return lmbWithoutItem;
    }

    private static void read() {
        if (read) {
            return;
        }
        read = true;
        if (!Platform.INSTANCE.isModLoaded("mousetweaks")) {
            return;
        }
        rmb = true;
        lmbWithItem = true;
        lmbWithoutItem = true;
        Path config = Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("MouseTweaks.cfg");
        try {
            List<String> lines = Files.exists(config) ? Files.readAllLines(config) : List.of();
            for (String line : lines) {
                String[] pair = line.trim().split("=", 2);
                if (pair.length != 2) {
                    continue;
                }
                boolean on = !pair[1].trim().equals("0") && !pair[1].trim().equalsIgnoreCase("false");
                switch (pair[0].trim()) {
                    case "RMBTweak" -> rmb = on;
                    case "LMBTweakWithItem" -> lmbWithItem = on;
                    case "LMBTweakWithoutItem" -> lmbWithoutItem = on;
                    default -> {
                    }
                }
            }
        } catch (IOException | RuntimeException ignored) {
            // Its defaults: every tweak on.
        }
    }
}
