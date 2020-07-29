/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashSet;

import com.google.gson.JsonObject;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.wurstclient.settings.FileSetting;
import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.json.JsonUtils;

public final class AutoBuildTemplate
{
	private final Path path;
	private final String name;
	private final int[][] blocks;
	
	private AutoBuildTemplate(Path path, int[][] blocks)
	{
		this.path = path;
		String fileName = path.getFileName().toString();
		name = fileName.substring(0, fileName.lastIndexOf("."));
		this.blocks = blocks;
	}
	
	public static AutoBuildTemplate load(Path path)
		throws IOException, JsonException
	{
		JsonObject json = JsonUtils.parseFileToObject(path).toJsonObject();
		int[][] blocks =
			JsonUtils.GSON.fromJson(json.get("blocks"), int[][].class);
		
		for(int i = 0; i < blocks.length; i++)
		{
			int length = blocks[i].length;
			
			if(length < 3)
				throw new JsonException("Entry blocks[" + i
					+ "] doesn't have X, Y and Z offset. Only found " + length
					+ " values");
		}
		
		return new AutoBuildTemplate(path, blocks);
	}
	
	public LinkedHashSet<BlockPos> getPositions(BlockPos startPos,
		Direction direction)
	{
		Direction front = direction;
		Direction left = front.rotateYCounterclockwise();
		LinkedHashSet<BlockPos> positions = new LinkedHashSet<>();
		
		for(int[] block : blocks)
		{
			BlockPos pos = startPos;
			pos = pos.offset(left, block[0]);
			pos = pos.up(block[1]);
			pos = pos.offset(front, block[2]);
			positions.add(pos);
		}
		
		return positions;
	}
	
	public int size()
	{
		return blocks.length;
	}
	
	public boolean isSelected(FileSetting setting)
	{
		return path.equals(setting.getSelectedFile());
	}
	
	public Path getPath()
	{
		return path;
	}
	
	public String getName()
	{
		return name;
	}
	
	public int[][] getBlocks()
	{
		return blocks;
	}
}
