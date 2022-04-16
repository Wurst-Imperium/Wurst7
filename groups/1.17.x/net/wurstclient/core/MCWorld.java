package net.wurstclient.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.world.dimension.DimensionType;

public class MCWorld {
    private final MinecraftClient MC;

    public MCWorld(MinecraftClient MC){
        this.MC = MC;
    }

    public DimensionType getDimension(){
        return this.MC.world.getDimension();
    }

    public boolean respawnAnchorWorks(DimensionType dimension){
        return dimension.isRespawnAnchorWorking();
    }

    public boolean respawnAnchorWorksCurrentDimension(){
        return this.respawnAnchorWorks(this.getDimension());
    }
}
