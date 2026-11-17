package com.pockyl.diegesis.impl.client.mesh;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.pockyl.diegesis.api.client.MeshShape;
import com.pockyl.diegesis.mixin.client.ItemStackStateAccess;
import org.joml.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/** Data-generated item geometry, inserted into the 1.21.11 item render-state pipeline. */
public final class ItemShapes {
    private static final Map<Item,Entry> ITEMS=new ConcurrentHashMap<>();
    private ItemShapes(){}
    public static void register(Item item,Function<ItemStack,MeshShape> shape,Identifier texture) {ITEMS.put(item,new Entry(shape,texture));}
    public static void resolve(ItemStack stack,ItemStackRenderState state,ItemDisplayContext context) {
        Entry entry=ITEMS.get(stack.getItem());if(entry==null)return;
        // Preserve the raw transform: capturing an already mirrored hand pose would mirror it twice.
        ItemStackStateAccess original=(ItemStackStateAccess)state;
        ItemTransform transform=original.diegesis$activeLayerCount()>0
            ? ((com.pockyl.diegesis.mixin.client.ItemLayerStateAccess)original.diegesis$layers()[0]).diegesis$transform()
            : ItemTransform.NO_TRANSFORM;
        List<BakedQuad> quads=entry.quads(stack);
        state.clear();((ItemStackStateAccess)state).diegesis$displayContext(context);
        var layer=state.newLayer();layer.prepareQuadList().addAll(quads);layer.setTransform(transform);
        layer.setUsesBlockLight(true);layer.setRenderType(net.minecraft.client.renderer.rendertype.RenderTypes.itemEntityTranslucentCull(sprite(entry.texture).atlasLocation()));
        layer.setParticleIcon(sprite(entry.texture));
        layer.setFoilType(stack.hasFoil()?ItemStackRenderState.FoilType.STANDARD:ItemStackRenderState.FoilType.NONE);
        layer.setExtents(()->quads.stream().flatMap(q->java.util.stream.IntStream.range(0,4).mapToObj(q::position)).toArray(Vector3fc[]::new));
    }
    private static net.minecraft.client.renderer.texture.TextureAtlasSprite sprite(Identifier texture) {
        var atlases=Minecraft.getInstance().getAtlasManager();
        var sprite=atlases.getAtlasOrThrow(net.minecraft.data.AtlasIds.BLOCKS).getSprite(texture);
        if(sprite.contents().name().equals(net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation()))
            sprite=atlases.getAtlasOrThrow(net.minecraft.data.AtlasIds.ITEMS).getSprite(texture);
        return sprite;
    }
    public static void clearCache(){ITEMS.values().forEach(e->e.baked.clear());com.pockyl.diegesis.impl.client.model.SceneModel.clearCache();}
    private static final class Entry {
        final Function<ItemStack,MeshShape> shape;final Identifier texture;
        final Map<DataComponentPatch,List<BakedQuad>> baked=new LinkedHashMap<>(16,.75F,true) {
            protected boolean removeEldestEntry(Map.Entry<DataComponentPatch,List<BakedQuad>> e){return size()>256;}
        };
        Entry(Function<ItemStack,MeshShape> shape,Identifier texture){this.shape=shape;this.texture=texture;}
        synchronized List<BakedQuad> quads(ItemStack stack){return baked.computeIfAbsent(stack.getComponentsPatch(),key->shape.apply(stack).mesh().quads(sprite(texture)));}
    }
}
