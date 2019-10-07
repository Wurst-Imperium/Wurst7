package net.mersid.util;

import java.util.LinkedList;
import java.util.List;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class GetEntitiesInRadius {
	private static final int RADIUS = 200;
	
	/**
	 * Returns all entities within a radius of 200, excluding self.
	 * @return
	 */
	public static List<Entity> get()
	{
		return get(RADIUS);
	}
	
	
	/**
	 * Returns all entities within a radius of radius, excluding self.
	 * @param radius
	 * @return
	 */
	public static List<Entity> get(int radius)
	{
		// Ensures the player and the world exists.
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player == null) return new LinkedList<>();
		World world = player.getEntityWorld();
		if (world == null) return new LinkedList<>();
		
		Vec3d playerPos = player.getPosVector();
		
		// The bounding box around the player with specified radius that will capture any entities within.
		Box entitiesIn = new Box(
				playerPos.x - radius,
				playerPos.y - radius,
				playerPos.z - radius,
				
				playerPos.x + radius,
				playerPos.y + radius,
				playerPos.z + radius);
		
		
		List<Entity> entities = world.getEntities(player, entitiesIn);
		
		entities.remove(player);
		return entities;
	}
}
