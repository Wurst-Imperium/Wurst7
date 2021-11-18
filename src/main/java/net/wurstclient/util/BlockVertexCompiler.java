/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.wurstclient.util.world.*;

import java.util.Objects;

/**
 * Converts a chunk of {@link BlockMatchingWorld} into a
 * {@link BufferBuilder} that can be used to render those blocks.
 * <p>
 * Used by hacks descending from {@link net.wurstclient.hack.BlockMatchHack}.
 */
public enum BlockVertexCompiler
{
	;

	public static BufferBuilder compileVertices(BlockMatchingWorld matchingWorld, int cx, int cz)
	{
		BufferBuilder buffer = BufferBuilderStorage.take();
		buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
		int bx = cx << 4;
		int bz = cz << 4;
		BoolChunk chunk = Objects.requireNonNull(matchingWorld.getChunk(ChunkPos.toLong(cx, cz))).getMatchChunk();
		if(chunk instanceof ArrayBoolChunk)
			for(int y = 0; y < BoolChunk.HEIGHT; y++)
				for(int z = 0; z < BoolChunk.LENGTH; z++)
				{
					short row = ((ArrayBoolChunk)chunk).getRow(y, z);
					if(row != 0)
						for(int x = 0; x < BoolChunk.WIDTH; x++)
							if(((row >> x) & 1) == 1)
								getVertices(buffer, bx + x, y, bz + z, matchingWorld);
				}
		else if(chunk instanceof SetBoolChunk)
			for(Long pos : ((SetBoolChunk)chunk).getBlockPositions())
				getVertices(buffer, BlockPos.unpackLongX(pos), BlockPos.unpackLongY(pos), BlockPos.unpackLongZ(pos), matchingWorld);
		else
			throw new UnsupportedOperationException();
		buffer.end();
		return buffer;
	}
	
	@SuppressWarnings({"PointlessArithmeticExpression", "squid:S2184"})
	public static void getVertices(BufferBuilder buffer, int x, int y, int z,
								   BoolView matchingWorld)
	{
		int vx = x & 15;
		int vz = z & 15;
		if(y == 0 || !matchingWorld.get(x, y - 1, z))
		{
			buffer.vertex(vx + 0, y + 0, vz + 0).next();
			buffer.vertex(vx + 1, y + 0, vz + 0).next();
			buffer.vertex(vx + 1, y + 0, vz + 1).next();
			buffer.vertex(vx + 0, y + 0, vz + 1).next();
		}
		
		if(y == BoolChunk.HEIGHT - 1 || !matchingWorld.get(x, y + 1, z))
		{
			buffer.vertex(vx + 0, y + 1, vz + 0).next();
			buffer.vertex(vx + 0, y + 1, vz + 1).next();
			buffer.vertex(vx + 1, y + 1, vz + 1).next();
			buffer.vertex(vx + 1, y + 1, vz + 0).next();
		}
		
		if(!matchingWorld.get(x, y, z - 1))
		{
			buffer.vertex(vx + 0, y + 0, vz + 0).next();
			buffer.vertex(vx + 0, y + 1, vz + 0).next();
			buffer.vertex(vx + 1, y + 1, vz + 0).next();
			buffer.vertex(vx + 1, y + 0, vz + 0).next();
		}

		if(!matchingWorld.get(x + 1, y, z))
		{
			buffer.vertex(vx + 1, y + 0, vz + 0).next();
			buffer.vertex(vx + 1, y + 1, vz + 0).next();
			buffer.vertex(vx + 1, y + 1, vz + 1).next();
			buffer.vertex(vx + 1, y + 0, vz + 1).next();
		}
		
		if(!matchingWorld.get(x, y, z + 1))
		{
			buffer.vertex(vx + 0, y + 0, vz + 1).next();
			buffer.vertex(vx + 1, y + 0, vz + 1).next();
			buffer.vertex(vx + 1, y + 1, vz + 1).next();
			buffer.vertex(vx + 0, y + 1, vz + 1).next();
		}
		
		if(!matchingWorld.get(x - 1, y, z))
		{
			buffer.vertex(vx + 0, y + 0, vz + 0).next();
			buffer.vertex(vx + 0, y + 0, vz + 1).next();
			buffer.vertex(vx + 0, y + 1, vz + 1).next();
			buffer.vertex(vx + 0, y + 1, vz + 0).next();
		}
	}
}
