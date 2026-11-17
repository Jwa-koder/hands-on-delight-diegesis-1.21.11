package com.pockyl.diegesis.fabric;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.resources.Identifier;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.FabricBakedModelManager;
import com.pockyl.diegesis.impl.client.model.SceneModel;
import com.pockyl.diegesis.impl.network.Packets;
import com.pockyl.diegesis.platform.ClientPlatform;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
public final class FabricClientPlatform implements ClientPlatform {
    static final Map<Identifier,ExtraModelKey<BlockStateModel>> MODELS=new ConcurrentHashMap<>();
    @Override public boolean serverHasChannel(){return Minecraft.getInstance().getConnection()!=null && FabricNetwork.serverHasChannel();}
    @Override public void sendToServer(Packets.ClientPacket packet){if(serverHasChannel())FabricNetwork.sendToServer(packet);}
    @Override public SceneModel getModel(Identifier id){
        var manager=Minecraft.getInstance().getModelManager();
        var key=MODELS.get(id);
        BlockStateModel model=key==null?null:((FabricBakedModelManager)manager).getModel(key);
        return SceneModel.block(model!=null?model:manager.getMissingBlockStateModel());
    }
}
