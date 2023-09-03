/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import java.util.Objects;
import java.util.stream.Stream;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import net.wurstclient.WurstClient;

public enum ChunkUtils
{
	;
	
	private static final MinecraftClient MC = WurstClient.MC;
	
	public static Stream<BlockEntity> getLoadedBlockEntities()
	{
		return getLoadedChunks()
			.flatMap(chunk -> chunk.getBlockEntities().values().stream());
	}
	
	public static Stream<WorldChunk> getLoadedChunks()
	{
		int radius = Math.max(2, MC.options.getClampedViewDistance()) + 3;
		int diameter = radius * 2 + 1;
		
		ChunkPos center = MC.player.getChunkPos();
		ChunkPos min = new ChunkPos(center.x - radius, center.z - radius);
		ChunkPos max = new ChunkPos(center.x + radius, center.z + radius);
		
		Stream<WorldChunk> stream = Stream.<ChunkPos> iterate(min, pos -> {
			
			int x = pos.x;
			int z = pos.z;
			
			x++;
			
			if(x > max.x)
			{
				x = min.x;
				z++;
			}
			
			if(z > max.z)
				throw new IllegalStateException("Stream limit didn't work.");
			
			return new ChunkPos(x, z);
			
		}).limit(diameter * diameter)
			.filter(c -> MC.world.isChunkLoaded(c.x, c.z))
			.map(c -> MC.world.getChunk(c.x, c.z)).filter(Objects::nonNull);
		
		return stream;
	}
	
	/**
	 * Returns the y-coordinate of the highest non-empty section in the chunk.
	 *
	 * <p>
	 * This is a re-implementation of
	 * {@link Chunk#getHighestNonEmptySectionYOffset()}, which has been
	 * deprecated and marked for removal in 23w17a with no apparent replacement
	 * provided by Mojang.
	 */
	public static int getHighestNonEmptySectionYOffset(Chunk chunk)
	{
		int i = chunk.getHighestNonEmptySection();
		if(i == -1)
			return chunk.getBottomY();
		
		return ChunkSectionPos.getBlockCoord(chunk.sectionIndexToCoord(i));
	}
}
