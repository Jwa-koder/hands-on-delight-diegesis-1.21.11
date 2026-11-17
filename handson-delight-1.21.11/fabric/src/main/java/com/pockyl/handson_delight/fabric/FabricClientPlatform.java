package com.pockyl.handson_delight.fabric;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pockyl.diegesis.impl.client.model.SceneModel;
import net.minecraft.world.item.ItemDisplayContext;

import com.pockyl.handson_delight.client.ClientPlatform;

/** Vanilla's way: the model's GUI transform, the same model. */
public final class FabricClientPlatform implements ClientPlatform {
    @Override
    public SceneModel guiModel(SceneModel model, PoseStack poseStack) {
        // SceneModel is extracted for GUI already; its render state applies the display transform.
        return model;
    }
}
