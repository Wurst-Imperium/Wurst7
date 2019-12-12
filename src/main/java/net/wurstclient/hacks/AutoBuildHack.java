/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.events.RightClickListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.FileSetting;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.RotationUtils;
import net.wurstclient.util.json.JsonException;
import net.wurstclient.util.json.JsonUtils;

public final class AutoBuildHack extends Hack
	implements UpdateListener, RightClickListener
{
	private final FileSetting template =
		new FileSetting("Template", "Determines what to build.", "autobuild",
			folder -> createDefaultTemplates(folder));
	
	private int[][] blocks;
	private String loadedTemplateName = "";
	private final ArrayList<BlockPos> positions = new ArrayList<>();
	
	public AutoBuildHack()
	{
		super("AutoBuild", "Builds things automatically.");
		setCategory(Category.BLOCKS);
		addSetting(template);
	}
	
	@Override
	public String getRenderName()
	{
		String name = getName();
		
		if(!loadedTemplateName.isEmpty())
			name += " [" + loadedTemplateName + "]";
		
		return name;
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(RightClickListener.class, this);
		loadSelectedTemplate();
	}
	
	private void loadSelectedTemplate()
	{
		Path path = template.getSelectedFile();
		
		try
		{
			loadTemplate(path);
			
		}catch(IOException | JsonException e)
		{
			Path fileName = path.getFileName();
			ChatUtils.error("Couldn't load template '" + fileName + "'.");
			
			String simpleClassName = e.getClass().getSimpleName();
			String message = e.getMessage();
			ChatUtils.message(simpleClassName + ": " + message);
			
			e.printStackTrace();
			setEnabled(false);
		}
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(RightClickListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		
	}
	
	@Override
	public void onRightClick(RightClickEvent event)
	{
		HitResult hitResult = MC.crosshairTarget;
		if(hitResult == null || hitResult.getPos() == null
			|| hitResult.getType() != HitResult.Type.BLOCK
			|| !(hitResult instanceof BlockHitResult))
			return;
		
		BlockHitResult blockHitResult = (BlockHitResult)hitResult;
		BlockPos hitResultPos = blockHitResult.getBlockPos();
		if(!BlockUtils.canBeClicked(hitResultPos))
			return;
		
		BlockPos startPos = hitResultPos.offset(blockHitResult.getSide());
		loadPositions(startPos);
		
		if(positions.size() <= 64)
			buildInstantly();
		else
		{
			EVENTS.add(UpdateListener.class, this);
			EVENTS.remove(RightClickListener.class, this);
		}
	}
	
	private void loadPositions(BlockPos startPos)
	{
		Direction front = MC.player.getHorizontalFacing();
		Direction left = front.rotateYCounterclockwise();
		
		positions.clear();
		for(int[] block : blocks)
		{
			BlockPos pos = startPos;
			pos = pos.offset(left, block[0]);
			pos = pos.up(block[1]);
			pos = pos.offset(front, block[2]);
			positions.add(pos);
		}
	}
	
	private void buildInstantly()
	{
		for(BlockPos pos : positions)
			if(BlockUtils.getState(pos).getMaterial().isReplaceable())
				placeBlockSimple_old(pos);
	}
	
	private boolean placeBlockSimple_old(BlockPos pos)
	{
		Vec3d eyesPos = RotationUtils.getEyesPos();
		Vec3d posVec = new Vec3d(pos).add(0.5, 0.5, 0.5);
		
		for(Direction side : Direction.values())
		{
			BlockPos neighbor = pos.offset(side);
			
			// check if neighbor can be right clicked
			if(!BlockUtils.canBeClicked(neighbor))
				continue;
			
			Vec3d hitVec =
				posVec.add(new Vec3d(side.getVector()).multiply(0.5));
			
			// check if hitVec is within range (6 blocks)
			if(eyesPos.squaredDistanceTo(hitVec) > 36)
				continue;
			
			// place block
			IMC.getInteractionManager().rightClickBlock(neighbor,
				side.getOpposite(), hitVec);
			
			return true;
		}
		
		return false;
	}
	
	private void loadTemplate(Path path) throws IOException, JsonException
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
		
		String fileName = template.getSelectedFileName();
		loadedTemplateName = fileName.substring(0, fileName.lastIndexOf("."));
		this.blocks = blocks;
	}
	
	private void createDefaultTemplates(Path folder)
	{
		for(DefaultTemplate template : DefaultTemplate.values())
		{
			JsonObject json = createJson(template);
			
			Path path = folder.resolve(template.name + ".json");
			
			try
			{
				JsonUtils.toJson(json, path);
				
			}catch(IOException | JsonException e)
			{
				System.out.println("Couldn't save " + path.getFileName());
				e.printStackTrace();
			}
		}
	}
	
	private JsonObject createJson(DefaultTemplate template)
	{
		JsonObject json = new JsonObject();
		JsonElement blocks = JsonUtils.GSON.toJsonTree(template.data);
		json.add("blocks", blocks);
		
		return json;
	}
	
	private static enum DefaultTemplate
	{
		BRIDGE("Bridge",
			new int[][]{{0, 0, 0}, {1, 0, 0}, {1, 0, -1}, {0, 0, -1},
				{-1, 0, -1}, {-1, 0, 0}, {-1, 0, -2}, {0, 0, -2}, {1, 0, -2},
				{1, 0, -3}, {0, 0, -3}, {-1, 0, -3}, {-1, 0, -4}, {0, 0, -4},
				{1, 0, -4}, {1, 0, -5}, {0, 0, -5}, {-1, 0, -5}}),
		
		FLOOR("Floor",
			new int[][]{{0, 0, 0}, {0, 0, 1}, {1, 0, 1}, {1, 0, 0}, {1, 0, -1},
				{0, 0, -1}, {-1, 0, -1}, {-1, 0, 0}, {-1, 0, 1}, {-1, 0, 2},
				{0, 0, 2}, {1, 0, 2}, {2, 0, 2}, {2, 0, 1}, {2, 0, 0},
				{2, 0, -1}, {2, 0, -2}, {1, 0, -2}, {0, 0, -2}, {-1, 0, -2},
				{-2, 0, -2}, {-2, 0, -1}, {-2, 0, 0}, {-2, 0, 1}, {-2, 0, 2},
				{-2, 0, 3}, {-1, 0, 3}, {0, 0, 3}, {1, 0, 3}, {2, 0, 3},
				{3, 0, 3}, {3, 0, 2}, {3, 0, 1}, {3, 0, 0}, {3, 0, -1},
				{3, 0, -2}, {3, 0, -3}, {2, 0, -3}, {1, 0, -3}, {0, 0, -3},
				{-1, 0, -3}, {-2, 0, -3}, {-3, 0, -3}, {-3, 0, -2}, {-3, 0, -1},
				{-3, 0, 0}, {-3, 0, 1}, {-3, 0, 2}, {-3, 0, 3}}),
		
		PENIS("Penis", new int[][]{{0, 0, 0}, {0, 0, 1}, {1, 0, 1}, {1, 0, 0},
			{1, 1, 0}, {0, 1, 0}, {0, 1, 1}, {1, 1, 1}, {1, 2, 1}, {0, 2, 1},
			{0, 2, 0}, {1, 2, 0}, {1, 3, 0}, {0, 3, 0}, {0, 3, 1}, {1, 3, 1},
			{1, 4, 1}, {0, 4, 1}, {0, 4, 0}, {1, 4, 0}, {1, 5, 0}, {0, 5, 0},
			{0, 5, 1}, {1, 5, 1}, {1, 6, 1}, {0, 6, 1}, {0, 6, 0}, {1, 6, 0},
			{1, 7, 0}, {0, 7, 0}, {0, 7, 1}, {1, 7, 1}, {-1, 0, -1},
			{-1, 1, -1}, {-2, 1, -1}, {-2, 0, -1}, {-2, 0, -2}, {-1, 0, -2},
			{-1, 1, -2}, {-2, 1, -2}, {2, 0, -1}, {2, 1, -1}, {2, 1, -2},
			{2, 0, -2}, {3, 0, -2}, {3, 0, -1}, {3, 1, -1}, {3, 1, -2}}),
		
		PILLAR("Pillar",
			new int[][]{{0, 0, 0}, {0, 1, 0}, {0, 2, 0}, {0, 3, 0}, {0, 4, 0},
				{0, 5, 0}, {0, 6, 0}}),
		
		SWASTIKA("Swastika",
			new int[][]{{0, 0, 0}, {1, 0, 0}, {2, 0, 0}, {0, 1, 0}, {0, 2, 0},
				{1, 2, 0}, {2, 2, 0}, {2, 3, 0}, {2, 4, 0}, {0, 3, 0},
				{0, 4, 0}, {-1, 4, 0}, {-2, 4, 0}, {-1, 2, 0}, {-2, 2, 0},
				{-2, 1, 0}, {-2, 0, 0}}),
		
		WALL("Wall",
			new int[][]{{0, 0, 0}, {1, 0, 0}, {1, 1, 0}, {0, 1, 0}, {-1, 1, 0},
				{-1, 0, 0}, {-2, 0, 0}, {-2, 1, 0}, {-2, 2, 0}, {-1, 2, 0},
				{0, 2, 0}, {1, 2, 0}, {2, 2, 0}, {2, 1, 0}, {2, 0, 0},
				{3, 0, 0}, {3, 1, 0}, {3, 2, 0}, {3, 3, 0}, {2, 3, 0},
				{1, 3, 0}, {0, 3, 0}, {-1, 3, 0}, {-2, 3, 0}, {-3, 3, 0},
				{-3, 2, 0}, {-3, 1, 0}, {-3, 0, 0}, {-3, 4, 0}, {-2, 4, 0},
				{-1, 4, 0}, {0, 4, 0}, {1, 4, 0}, {2, 4, 0}, {3, 4, 0},
				{3, 5, 0}, {2, 5, 0}, {1, 5, 0}, {0, 5, 0}, {-1, 5, 0},
				{-2, 5, 0}, {-3, 5, 0}, {-3, 6, 0}, {-2, 6, 0}, {-1, 6, 0},
				{0, 6, 0}, {1, 6, 0}, {2, 6, 0}, {3, 6, 0}}),
		
		WURST("Wurst",
			new int[][]{{0, 0, 0}, {1, 0, 0}, {1, 1, 0}, {0, 1, 0}, {0, 1, 1},
				{1, 1, 1}, {2, 1, 1}, {2, 1, 0}, {2, 0, 0}, {2, 1, -1},
				{1, 1, -1}, {0, 1, -1}, {-1, 1, -1}, {-1, 1, 0}, {-1, 0, 0},
				{-2, 0, 0}, {-2, 1, 0}, {-2, 1, 1}, {-1, 1, 1}, {-1, 2, 0},
				{0, 2, 0}, {1, 2, 0}, {2, 2, 0}, {3, 1, 0}, {-2, 1, -1},
				{-2, 2, 0}, {-3, 1, 0}});
		
		private final String name;
		private final int[][] data;
		
		private DefaultTemplate(String name, int[][] data)
		{
			this.name = name;
			this.data = data;
		}
	}
}
