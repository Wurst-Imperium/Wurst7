package net.wurstclient.core;

import net.minecraft.item.Item;
import net.minecraft.util.registry.Registry;

import java.util.Random;

public class RegistryUtils {
    public static Item getRandomItem(){
        Random random = new Random();
        return Registry.ITEM.getRandom(random);
    }
}
