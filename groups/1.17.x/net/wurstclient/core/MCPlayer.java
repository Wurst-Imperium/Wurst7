package net.wurstclient.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.stat.StatHandler;


public class MCPlayer extends ClientPlayerEntity {

    public MCPlayer(MinecraftClient client, ClientWorld world, ClientPlayNetworkHandler networkHandler, StatHandler stats, ClientRecipeBook recipeBook, boolean lastSneaking, boolean lastSprinting) {
        super(client, world, networkHandler, stats, recipeBook, lastSneaking, lastSprinting);
    }

    public float getAirSpeed(){
        return this.flyingSpeed;
    }

    public void setAirSpeed(float value){
        this.flyingSpeed = value;
    }
}
