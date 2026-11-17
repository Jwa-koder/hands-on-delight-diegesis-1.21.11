package com.pockyl.handson_delight.compat;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;

/** Registry-aware item-stack NBT serialization for the 1.21.11 codec API. */
public final class StackNbt {
    private StackNbt() {}
    public static CompoundTag save(ItemStack stack, HolderLookup.Provider registries) {
        if (stack.isEmpty()) return new CompoundTag();
        return (CompoundTag) ItemStack.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack).getOrThrow();
    }
    public static ItemStack load(HolderLookup.Provider registries, CompoundTag tag) {
        return ItemStack.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag).result().orElse(ItemStack.EMPTY);
    }
}
