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
import java.util.LinkedHashSet;

import net.minecraft.item.Item;
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
	private final LinkedHashSet<BlockData> blocks;
	
	private AutoBuildTemplate(Path path, LinkedHashSet<BlockData> blocks)
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
		int version = json.getInt("version", 1);
		
		WsonArray jsonBlocks = json.getArray("blocks");
		LinkedHashSet<BlockData> loadedBlocks = new LinkedHashSet<>();
		if(jsonBlocks.isEmpty())
			throw new JsonException("Template has no blocks!");
		
		switch(version)
		{
			case 1 -> loadV1(jsonBlocks, loadedBlocks);
			case 2 -> loadV2(jsonBlocks, loadedBlocks);
			default -> throw new JsonException(
				"Unknown template version: " + version);
		}
		
		return new AutoBuildTemplate(path, loadedBlocks);
	}
	
	private static void loadV2(WsonArray jsonBlocks,
		LinkedHashSet<BlockData> loadedBlocks) throws JsonException
	{
		for(int i = 0; i < jsonBlocks.size(); i++)
		{
			WsonObject jsonBlock = jsonBlocks.getObject(i);
			try
			{
				WsonArray jsonPos = jsonBlock.getArray("pos");
				int[] pos = new int[3];
				pos[0] = jsonPos.getInt(0);
				pos[1] = jsonPos.getInt(1);
				pos[2] = jsonPos.getInt(2);
				String name = jsonBlock.getString("block", "");
				loadedBlocks.add(new BlockData(pos, name));
				
			}catch(JsonException e)
			{
				throw new JsonException("Entry blocks[" + i + "] is not valid",
					e);
			}
		}
	}
	
	private static void loadV1(WsonArray jsonBlocks,
		LinkedHashSet<BlockData> loadedBlocks) throws JsonException
	{
		for(int i = 0; i < jsonBlocks.size(); i++)
		{
			WsonArray jsonBlock = jsonBlocks.getArray(i);
			try
			{
				int[] pos = new int[3];
				pos[0] = jsonBlock.getInt(0);
				pos[1] = jsonBlock.getInt(1);
				pos[2] = jsonBlock.getInt(2);
				loadedBlocks.add(new BlockData(pos, ""));
				
			}catch(JsonException e)
			{
				throw new JsonException("Entry blocks[" + i + "] is not valid",
					e);
			}
		}
	}
	
	public LinkedHashMap<BlockPos, Item> getBlocksToPlace(BlockPos origin,
		Direction direction)
	{
		Direction front = direction;
		Direction left = front.rotateYCounterclockwise();
		LinkedHashMap<BlockPos, Item> blocksToPlace = new LinkedHashMap<>();
		
		for(BlockData block : blocks)
		{
			BlockPos pos = block.toBlockPos(origin, front, left);
			Item item = block.toItem();
			blocksToPlace.put(pos, item);
		}
		
		return blocksToPlace;
	}
	
	public int size()
	{
		return blocks.size();
	}
	
	public boolean isSelected(FileSetting setting)
	{
		return path.equals(setting.getSelectedFile());
	}
	
	public String getName()
	{
		return name;
	}
	
	private record BlockData(int[] pos, String name)
	{
		public BlockPos toBlockPos(BlockPos origin, Direction front,
			Direction left)
		{
			return origin.offset(left, pos[0]).up(pos[1]).offset(front, pos[2]);
		}
		
		public Item toItem()
		{
			return BlockUtils.getBlockFromName(name).asItem();
		}
	}
}
