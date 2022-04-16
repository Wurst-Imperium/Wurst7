package net.wurstclient.core;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;


public class MCNbtUtils {
    public static void setNbt(ItemStack itemStack, NbtCompound nbt){
        itemStack.setNbt(nbt);
    }

    public static NbtCompound getNbt(ItemStack itemStack){
        return itemStack.getNbt();
    }

    public static void setSubNbt(ItemStack itemStack, String key, NbtElement tag){
        itemStack.setSubNbt(key, tag);
    }

    public static NbtCompound getSubNbt(ItemStack itemStack, String key){
        return itemStack.getSubNbt(key);
    }

    public static boolean hasNbt(ItemStack itemStack){
        return itemStack.hasNbt();
    }

}
