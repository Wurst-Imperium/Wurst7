/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings;

import java.util.ArrayList;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.wurstclient.WurstClient;

public final class ChunkAreaSetting
	extends EnumSetting<ChunkAreaSetting.ChunkArea>
{
	private static final Minecraft MC = WurstClient.MC;
	
	public ChunkAreaSetting(String name, String description)
	{
		super(name, description, ChunkArea.values(), ChunkArea.A11);
	}
	
	public ChunkAreaSetting(String name, String description, ChunkArea selected)
	{
		super(name, description, ChunkArea.values(), selected);
	}
	
	public ArrayList<ChunkAccess> getChunksInRange()
	{
		return getSelected().getChunksInRange();
	}
	
	public boolean isInRange(ChunkPos pos)
	{
		return getSelected().isInRange(pos);
	}
	
	public enum ChunkArea
	{
		A3("3x3 chunks", 1),
		A5("5x5 chunks", 2),
		A7("7x7 chunks", 3),
		A9("9x9 chunks", 4),
		A11("11x11 chunks", 5),
		A13("13x13 chunks", 6),
		A15("15x15 chunks", 7),
		A17("17x17 chunks", 8),
		A19("19x19 chunks", 9),
		A21("21x21 chunks", 10),
		A23("23x23 chunks", 11),
		A25("25x25 chunks", 12),
		A27("27x27 chunks", 13),
		A29("29x29 chunks", 14),
		A31("31x31 chunks", 15),
		A33("33x33 chunks", 16);
		
		private final String name;
		private final int chunkRange;
		
		private ChunkArea(String name, int chunkRange)
		{
			this.name = name;
			this.chunkRange = chunkRange;
		}
		
		public ArrayList<ChunkAccess> getChunksInRange()
		{
			ChunkPos center = MC.player.chunkPosition();
			ArrayList<ChunkAccess> chunksInRange = new ArrayList<>();
			
			for(int x = center.x - chunkRange; x <= center.x + chunkRange; x++)
				for(int z = center.z - chunkRange; z <= center.z
					+ chunkRange; z++)
				{
					ChunkAccess chunk = MC.level.getChunk(x, z);
					if(chunk instanceof EmptyLevelChunk)
						continue;
					
					chunksInRange.add(chunk);
				}
			
			return chunksInRange;
		}
		
		public boolean isInRange(ChunkPos pos)
		{
			ChunkPos center = MC.player.chunkPosition();
			return Math.abs(pos.x - center.x) <= chunkRange
				&& Math.abs(pos.z - center.z) <= chunkRange;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
