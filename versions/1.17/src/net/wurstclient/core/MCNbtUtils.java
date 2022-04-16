package net.wurstclient.core;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;


public class MCNbtUtils {
    public static void setNbt(ItemStack itemStack, NbtCompound nbt){
        itemStack.writeNbt(nbt);
    }

    public static NbtCompound getNbt(ItemStack itemStack){
        return itemStack.getTag();
    }

    public static void setSubNbt(ItemStack itemStack, String key, NbtElement tag){
        itemStack.putSubTag(key, tag);
    }

    public static NbtCompound getSubNbt(ItemStack itemStack, String key){
        return itemStack.getSubTag(key);
    }

    public static boolean hasNbt(ItemStack itemStack){
        return itemStack.hasTag();
    }

}
