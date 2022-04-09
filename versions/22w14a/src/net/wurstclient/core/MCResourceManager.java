package net.wurstclient.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.FileNotFoundException;

public class MCResourceManager {

    private final ResourceManager resourceManager;

    public MCResourceManager(MinecraftClient MC) {
            this.resourceManager = MC.getResourceManager();
    }

    public Resource getResourceOrThrow(Identifier id) throws FileNotFoundException{
        return this.resourceManager.getResourceOrThrow(id);
    }
}

