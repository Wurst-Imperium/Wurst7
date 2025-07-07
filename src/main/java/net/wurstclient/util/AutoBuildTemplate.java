/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashSet;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.wurstclient.settings.FileSetting;
import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.json.JsonUtils;
import net.wurstclient.util.json.WsonArray;
import net.wurstclient.util.json.WsonObject;

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
		WsonObject json = JsonUtils.parseFileToObject(path);
		WsonArray jsonBlocks = json.getArray("blocks");
		
		if(jsonBlocks.isEmpty())
			throw new JsonException("Template has no blocks!");
		
		int[][] intBlocks = new int[jsonBlocks.size()][];
		for(int i = 0; i < jsonBlocks.size(); i++)
		{
			WsonArray jsonBlock = jsonBlocks.getArray(i);
			try
			{
				int[] intBlock = new int[3];
				intBlock[0] = jsonBlock.getInt(0);
				intBlock[1] = jsonBlock.getInt(1);
				intBlock[2] = jsonBlock.getInt(2);
				intBlocks[i] = intBlock;
				
			}catch(JsonException e)
			{
				throw new JsonException("Entry blocks[" + i + "] is not valid",
					e);
			}
		}
		
		return new AutoBuildTemplate(path, intBlocks);
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
