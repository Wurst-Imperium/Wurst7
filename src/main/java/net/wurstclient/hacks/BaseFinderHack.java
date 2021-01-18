/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import org.lwjgl.opengl.GL11;

import net.minecraft.util.math.BlockPos;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.BlockListSetting;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.RenderUtils;

@SearchTags({"base finder", "factions"})
public final class BaseFinderHack extends Hack
	implements UpdateListener, RenderListener
{
	private final BlockListSetting naturalBlocks = new BlockListSetting(
		"Natural Blocks",
		"These blocks will be considered\n" + "part of natural generation.\n\n"
			+ "They will NOT be highlighted\n" + "as player bases.",
		"minecraft:acacia_leaves", "minecraft:acacia_log", "minecraft:air",
		"minecraft:allium", "minecraft:andesite", "minecraft:azure_bluet",
		"minecraft:bedrock", "minecraft:birch_leaves", "minecraft:birch_log",
		"minecraft:blue_orchid", "minecraft:brown_mushroom",
		"minecraft:brown_mushroom_block", "minecraft:bubble_column",
		"minecraft:cave_air", "minecraft:clay", "minecraft:coal_ore",
		"minecraft:cobweb", "minecraft:cornflower", "minecraft:dandelion",
		"minecraft:dark_oak_leaves", "minecraft:dark_oak_log",
		"minecraft:dead_bush", "minecraft:diamond_ore", "minecraft:diorite",
		"minecraft:dirt", "minecraft:emerald_ore", "minecraft:fern",
		"minecraft:gold_ore", "minecraft:granite", "minecraft:grass",
		"minecraft:grass_block", "minecraft:gravel", "minecraft:ice",
		"minecraft:infested_stone", "minecraft:iron_ore",
		"minecraft:jungle_leaves", "minecraft:jungle_log", "minecraft:kelp",
		"minecraft:kelp_plant", "minecraft:lapis_ore", "minecraft:large_fern",
		"minecraft:lava", "minecraft:lilac", "minecraft:lily_of_the_valley",
		"minecraft:lily_pad", "minecraft:mossy_cobblestone",
		"minecraft:mushroom_stem", "minecraft:nether_quartz_ore",
		"minecraft:netherrack", "minecraft:oak_leaves", "minecraft:oak_log",
		"minecraft:obsidian", "minecraft:orange_tulip", "minecraft:oxeye_daisy",
		"minecraft:peony", "minecraft:pink_tulip", "minecraft:poppy",
		"minecraft:red_mushroom", "minecraft:red_mushroom_block",
		"minecraft:red_tulip", "minecraft:redstone_ore", "minecraft:rose_bush",
		"minecraft:sand", "minecraft:sandstone", "minecraft:seagrass",
		"minecraft:snow", "minecraft:spawner", "minecraft:spruce_leaves",
		"minecraft:spruce_log", "minecraft:stone", "minecraft:sunflower",
		"minecraft:tall_grass", "minecraft:tall_seagrass", "minecraft:vine",
		"minecraft:water", "minecraft:white_tulip");
	
	private ArrayList<String> blockNames;
	
	private final HashSet<BlockPos> matchingBlocks = new HashSet<>();
	private final ArrayList<int[]> vertices = new ArrayList<>();
	
	private int messageTimer = 0;
	private int counter;
	
	public BaseFinderHack()
	{
		super("BaseFinder",
			"Finds player bases by searching for man-made blocks.\n"
				+ "The blocks that it finds will be highlighted in red.\n"
				+ "Good for finding faction bases.");
		setCategory(Category.RENDER);
		addSetting(naturalBlocks);
	}
	
	@Override
	public String getRenderName()
	{
		String name = getName() + " [";
		
		// counter
		if(counter >= 10000)
			name += "10000+ blocks";
		else if(counter == 1)
			name += "1 block";
		else if(counter == 0)
			name += "nothing";
		else
			name += counter + " blocks";
		
		name += " found]";
		return name;
	}
	
	@Override
	public void onEnable()
	{
		// reset timer
		messageTimer = 0;
		blockNames = new ArrayList<>(naturalBlocks.getBlockNames());
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		matchingBlocks.clear();
		vertices.clear();
	}
	
	@Override
	public void onRender(float partialTicks)
	{
		// GL settings
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glColor4f(1F, 0F, 0F, 0.15F);
		
		GL11.glPushMatrix();
		RenderUtils.applyRenderOffset();
		
		// vertices
		GL11.glBegin(GL11.GL_QUADS);
		{
			for(int[] vertex : vertices)
				GL11.glVertex3d(vertex[0], vertex[1], vertex[2]);
		}
		GL11.glEnd();
		
		GL11.glPopMatrix();
		
		// GL resets
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glColor4f(1, 1, 1, 1);
	}
	
	@Override
	public void onUpdate()
	{
		int modulo = MC.player.age % 64;
		
		// reset matching blocks
		if(modulo == 0)
			matchingBlocks.clear();
		
		int startY = 255 - modulo * 4;
		int endY = startY - 4;
		
		BlockPos playerPos =
			new BlockPos(MC.player.getX(), 0, MC.player.getZ());
		
		// search matching blocks
		loop: for(int y = startY; y > endY; y--)
			for(int x = 64; x > -64; x--)
				for(int z = 64; z > -64; z--)
				{
					if(matchingBlocks.size() >= 10000)
						break loop;
					
					BlockPos pos = playerPos.add(x, y, z);
					
					if(Collections.binarySearch(blockNames,
						BlockUtils.getName(pos)) >= 0)
						continue;
					
					matchingBlocks.add(pos);
				}
			
		if(modulo != 63)
			return;
		
		// update timer
		if(matchingBlocks.size() < 10000)
			messageTimer--;
		else
		{
			// show message
			if(messageTimer <= 0)
			{
				ChatUtils
					.warning("BaseFinder found \u00a7lA LOT\u00a7r of blocks.");
				ChatUtils.message(
					"To prevent lag, it will only show the first 10000 blocks.");
			}
			
			// reset timer
			messageTimer = 3;
		}
		
		// update counter
		counter = matchingBlocks.size();
		
		// calculate vertices
		vertices.clear();
		for(BlockPos pos : matchingBlocks)
		{
			if(!matchingBlocks.contains(pos.down()))
			{
				addVertex(pos, 0, 0, 0);
				addVertex(pos, 1, 0, 0);
				addVertex(pos, 1, 0, 1);
				addVertex(pos, 0, 0, 1);
			}
			
			if(!matchingBlocks.contains(pos.up()))
			{
				addVertex(pos, 0, 1, 0);
				addVertex(pos, 0, 1, 1);
				addVertex(pos, 1, 1, 1);
				addVertex(pos, 1, 1, 0);
			}
			
			if(!matchingBlocks.contains(pos.north()))
			{
				addVertex(pos, 0, 0, 0);
				addVertex(pos, 0, 1, 0);
				addVertex(pos, 1, 1, 0);
				addVertex(pos, 1, 0, 0);
			}
			
			if(!matchingBlocks.contains(pos.east()))
			{
				addVertex(pos, 1, 0, 0);
				addVertex(pos, 1, 1, 0);
				addVertex(pos, 1, 1, 1);
				addVertex(pos, 1, 0, 1);
			}
			
			if(!matchingBlocks.contains(pos.south()))
			{
				addVertex(pos, 0, 0, 1);
				addVertex(pos, 1, 0, 1);
				addVertex(pos, 1, 1, 1);
				addVertex(pos, 0, 1, 1);
			}
			
			if(!matchingBlocks.contains(pos.west()))
			{
				addVertex(pos, 0, 0, 0);
				addVertex(pos, 0, 0, 1);
				addVertex(pos, 0, 1, 1);
				addVertex(pos, 0, 1, 0);
			}
		}
	}
	
	private void addVertex(BlockPos pos, int x, int y, int z)
	{
		vertices.add(new int[]{pos.getX() + x, pos.getY() + y, pos.getZ() + z});
	}
}
