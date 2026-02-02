/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util.chunk;

import java.util.Objects;
import java.util.stream.Stream;

import net.minecraft.client.Minecraft;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.wurstclient.WurstClient;

public enum ChunkUtils
{
	;
	
	private static final Minecraft MC = WurstClient.MC;
	
	public static Stream<BlockEntity> getLoadedBlockEntities()
	{
		return getLoadedChunks()
			.flatMap(chunk -> chunk.getBlockEntities().values().stream());
	}
	
	public static int getManhattanDistance(ChunkPos a, ChunkPos b)
	{
		return Math.abs(a.x - b.x) + Math.abs(a.z - b.z);
	}
	
	/**
	 * Returns the position of the chunk affected by the given
	 * {@link ClientboundBlockUpdatePacket},
	 * {@link ClientboundSectionBlocksUpdatePacket}, or
	 * {@link ClientboundLevelChunkWithLightPacket}.
	 *
	 * <p>
	 * Returns <code>null</code> if the given packet is of a different type than
	 * the ones listed above.
	 */
	public static ChunkPos getAffectedChunk(Packet<?> packet)
	{
		if(packet instanceof ClientboundBlockUpdatePacket p)
			return new ChunkPos(p.getPos());
		if(packet instanceof ClientboundSectionBlocksUpdatePacket p)
			return p.sectionPos.chunk();
		if(packet instanceof ClientboundLevelChunkWithLightPacket p)
			return new ChunkPos(p.getX(), p.getZ());
		
		return null;
	}
	
	public static Stream<LevelChunk> getLoadedChunks()
	{
		int radius = Math.max(2, MC.options.getEffectiveRenderDistance()) + 3;
		int diameter = radius * 2 + 1;
		
		ChunkPos center = MC.player.chunkPosition();
		ChunkPos min = new ChunkPos(center.x - radius, center.z - radius);
		ChunkPos max = new ChunkPos(center.x + radius, center.z + radius);
		
		Stream<LevelChunk> stream = Stream.<ChunkPos> iterate(min, pos -> {
			
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
			
		}).limit(diameter * diameter).filter(c -> MC.level.hasChunk(c.x, c.z))
			.map(c -> MC.level.getChunk(c.x, c.z)).filter(Objects::nonNull);
		
		return stream;
	}
	
	/**
	 * Returns the y-coordinate of the highest non-empty section in the chunk.
	 *
	 * <p>
	 * This is a re-implementation of
	 * {@link ChunkAccess#getHighestSectionPosition()}, which has been
	 * deprecated and marked for removal in 23w17a with no apparent replacement
	 * provided by Mojang.
	 */
	public static int getHighestNonEmptySectionYOffset(ChunkAccess chunk)
	{
		int i = chunk.getHighestFilledSectionIndex();
		if(i == -1)
			return chunk.getMinY();
		
		return SectionPos
			.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(i));
	}
}
