package net.wurstclient.core;

import net.minecraft.item.Item;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.math.random.AbstractRandom;
import java.util.Optional;

public class RegistryUtils {
    public static Item getRandomItem(){
        // Not sure if it's possible to get an empty optional here,
        // but if so it will just retry.
        Optional<RegistryEntry<Item>> optional = Optional.empty();
        AbstractRandom random = AbstractRandom.create();
        while(optional.isEmpty())
            optional = Registry.ITEM.getRandom(random);
        return optional.get().value();
    }
}
