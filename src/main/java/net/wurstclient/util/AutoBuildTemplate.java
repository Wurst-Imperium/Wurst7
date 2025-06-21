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
import java.util.LinkedHashMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.wurstclient.settings.FileSetting;
import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.json.JsonUtils;
import net.wurstclient.util.json.WsonObject;

public final class AutoBuildTemplate
{
	private final Path path;
	private final String name;
	private final BlockData[] blocks;
	
	private AutoBuildTemplate(Path path, BlockData[] blocks)
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
		JsonElement blocksElement = json.getElement("blocks");
		
		if(blocksElement == null)
			throw new JsonException("Template has no blocks!");
		
		if(!blocksElement.isJsonArray())
			throw new JsonException("'blocks' is not a JSON array.");
		
		JsonArray jsonBlocks = blocksElement.getAsJsonArray();
		BlockData[] blocks = new BlockData[jsonBlocks.size()];
		
		if(jsonBlocks.isEmpty())
			return new AutoBuildTemplate(path, blocks);
		
		JsonElement first = jsonBlocks.get(0);
		
		if(first.isJsonArray())
		{
			// old format compatibility
			int[][] oldBlocks =
				JsonUtils.GSON.fromJson(jsonBlocks, int[][].class);
			for(int i = 0; i < oldBlocks.length; i++)
			{
				if(oldBlocks[i].length < 3)
					throw new JsonException("Entry blocks[" + i
						+ "] doesn't have X, Y and Z offset. Only found "
						+ oldBlocks[i].length + " values");
				
				blocks[i] = new BlockData(oldBlocks[i], null);
			}
			
		}else if(first.isJsonObject())
		{
			// New format
			for(int i = 0; i < jsonBlocks.size(); i++)
			{
				JsonObject blockObj = jsonBlocks.get(i).getAsJsonObject();
				int[] pos =
					JsonUtils.GSON.fromJson(blockObj.get("pos"), int[].class);
				
				if(pos == null || pos.length < 3)
					throw new JsonException(
						"Block " + i + " has invalid 'pos'.");
				
				String name = blockObj.has("name")
					? blockObj.get("name").getAsString() : null;
				blocks[i] = new BlockData(pos, name);
			}
			
		}else
			throw new JsonException(
				"Unknown format for 'blocks' array elements.");
		
		return new AutoBuildTemplate(path, blocks);
	}
	
	public LinkedHashMap<BlockPos, String> getBlocksToPlace(BlockPos startPos,
		Direction direction)
	{
		Direction front = direction;
		Direction left = front.rotateYCounterclockwise();
		LinkedHashMap<BlockPos, String> positions = new LinkedHashMap<>();
		
		for(BlockData block : blocks)
		{
			int[] blockPosArray = block.getPos();
			BlockPos pos = startPos;
			pos = pos.offset(left, blockPosArray[0]);
			pos = pos.up(blockPosArray[1]);
			pos = pos.offset(front, blockPosArray[2]);
			positions.put(pos, block.getName());
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
	
	public BlockData[] getBlocks()
	{
		return blocks;
	}
	
	public static final class BlockData
	{
		private final int[] pos;
		private final String name;
		
		public BlockData(int[] pos, String name)
		{
			this.pos = pos;
			this.name = name;
		}
		
		public int[] getPos()
		{
			return pos;
		}
		
		public String getName()
		{
			return name;
		}
	}
}
