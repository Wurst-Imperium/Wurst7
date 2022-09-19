/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.search;

import java.util.ArrayList;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.wurstclient.WurstClient;

public enum SearchArea
{
	D3("3x3 chunks", 1),
	D5("5x5 chunks", 2),
	D7("7x7 chunks", 3),
	D9("9x9 chunks", 4),
	D11("11x11 chunks", 5),
	D13("13x13 chunks", 6),
	D15("15x15 chunks", 7),
	D17("17x17 chunks", 8),
	D19("19x19 chunks", 9),
	D21("21x21 chunks", 10),
	D23("23x23 chunks", 11),
	D25("25x25 chunks", 12),
	D27("27x27 chunks", 13),
	D29("29x29 chunks", 14),
	D31("31x31 chunks", 15),
	D33("33x33 chunks", 16);
	
	private final String name;
	private final int chunkRange;
	
	private SearchArea(String name, int chunkRange)
	{
		this.name = name;
		this.chunkRange = chunkRange;
	}
	
	public ArrayList<Chunk> getChunksInRange(ChunkPos center)
	{
		ArrayList<Chunk> chunksInRange = new ArrayList<>();
		
		for(int x = center.x - chunkRange; x <= center.x + chunkRange; x++)
			for(int z = center.z - chunkRange; z <= center.z + chunkRange; z++)
			{
				Chunk chunk = WurstClient.MC.world.getChunk(x, z);
				if(chunk instanceof EmptyChunk)
					continue;
				
				chunksInRange.add(chunk);
			}
		
		return chunksInRange;
	}
	
	public boolean isInRange(ChunkPos pos, ChunkPos center)
	{
		return Math.abs(pos.x - center.x) <= chunkRange
			&& Math.abs(pos.z - center.z) <= chunkRange;
	}
	
	@Override
	public String toString()
	{
		return name;
	}
}
