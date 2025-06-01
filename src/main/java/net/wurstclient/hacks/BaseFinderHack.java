/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;

import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.WurstRenderLayers;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.BlockListSetting;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.BlockVertexCompiler;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.EasyVertexBuffer;
import net.wurstclient.util.RegionPos;
import net.wurstclient.util.RenderUtils;

@SearchTags({"base finder", "factions"})
public final class BaseFinderHack extends Hack
	implements UpdateListener, RenderListener
{
	private final BlockListSetting naturalBlocks = new BlockListSetting(
		"Natural Blocks",
		"These blocks will be considered part of natural generation.\n\n"
			+ "They will NOT be highlighted as player bases.",
		"minecraft:acacia_leaves", "minecraft:acacia_log", "minecraft:air",
		"minecraft:allium", "minecraft:amethyst_block",
		"minecraft:amethyst_cluster", "minecraft:andesite",
		"minecraft:azure_bluet", "minecraft:bedrock", "minecraft:birch_leaves",
		"minecraft:birch_log", "minecraft:blue_orchid",
		"minecraft:brown_mushroom", "minecraft:brown_mushroom_block",
		"minecraft:bubble_column", "minecraft:budding_amethyst",
		"minecraft:calcite", "minecraft:cave_air", "minecraft:clay",
		"minecraft:coal_ore", "minecraft:cobweb", "minecraft:copper_ore",
		"minecraft:cornflower", "minecraft:dandelion",
		"minecraft:dark_oak_leaves", "minecraft:dark_oak_log",
		"minecraft:dead_bush", "minecraft:deepslate",
		"minecraft:deepslate_coal_ore", "minecraft:deepslate_copper_ore",
		"minecraft:deepslate_diamond_ore", "minecraft:deepslate_emerald_ore",
		"minecraft:deepslate_gold_ore", "minecraft:deepslate_iron_ore",
		"minecraft:deepslate_lapis_ore", "minecraft:deepslate_redstone_ore",
		"minecraft:diamond_ore", "minecraft:diorite", "minecraft:dirt",
		"minecraft:dripstone_block", "minecraft:emerald_ore", "minecraft:fern",
		"minecraft:glow_lichen", "minecraft:gold_ore", "minecraft:granite",
		"minecraft:grass", "minecraft:grass_block", "minecraft:gravel",
		"minecraft:ice", "minecraft:infested_stone", "minecraft:iron_ore",
		"minecraft:jungle_leaves", "minecraft:jungle_log", "minecraft:kelp",
		"minecraft:kelp_plant", "minecraft:lapis_ore",
		"minecraft:large_amethyst_bud", "minecraft:large_fern",
		"minecraft:lava", "minecraft:lilac", "minecraft:lily_of_the_valley",
		"minecraft:lily_pad", "minecraft:medium_amethyst_bud",
		"minecraft:mossy_cobblestone", "minecraft:mushroom_stem",
		"minecraft:nether_quartz_ore", "minecraft:netherrack",
		"minecraft:oak_leaves", "minecraft:oak_log", "minecraft:obsidian",
		"minecraft:orange_tulip", "minecraft:oxeye_daisy", "minecraft:peony",
		"minecraft:pink_tulip", "minecraft:pointed_dripstone",
		"minecraft:poppy", "minecraft:red_mushroom",
		"minecraft:red_mushroom_block", "minecraft:red_tulip",
		"minecraft:redstone_ore", "minecraft:rose_bush", "minecraft:sand",
		"minecraft:sandstone", "minecraft:seagrass",
		"minecraft:small_amethyst_bud", "minecraft:smooth_basalt",
		"minecraft:snow", "minecraft:spawner", "minecraft:spruce_leaves",
		"minecraft:spruce_log", "minecraft:stone", "minecraft:sunflower",
		"minecraft:tall_grass", "minecraft:tall_seagrass", "minecraft:tuff",
		"minecraft:vine", "minecraft:water", "minecraft:white_tulip");
	
	private final ColorSetting color = new ColorSetting("Color",
		"Man-made blocks will be highlighted in this color.", Color.RED);
	
	private ArrayList<String> blockNames;
	
	private final HashSet<BlockPos> matchingBlocks = new HashSet<>();
	private ArrayList<int[]> vertices = new ArrayList<>();
	private EasyVertexBuffer vertexBuffer;
	
	private int messageTimer = 0;
	private int counter;
	
	private RegionPos lastRegion;
	
	public BaseFinderHack()
	{
		super("BaseFinder");
		setCategory(Category.RENDER);
		addSetting(naturalBlocks);
		addSetting(color);
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
	protected void onEnable()
	{
		// reset timer
		messageTimer = 0;
		blockNames = new ArrayList<>(naturalBlocks.getBlockNames());
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		matchingBlocks.clear();
		vertices.clear();
		
		if(vertexBuffer != null)
			vertexBuffer.close();
		vertexBuffer = null;
		lastRegion = null;
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		RegionPos region = RenderUtils.getCameraRegion();
		if(!region.equals(lastRegion))
			onUpdate();
		
		if(vertexBuffer == null)
			return;
		
		matrixStack.push();
		RenderUtils.applyRegionalRenderOffset(matrixStack, region);
		
		vertexBuffer.draw(matrixStack, WurstRenderLayers.ESP_QUADS,
			color.getColorF(), 0.25F);
		
		matrixStack.pop();
	}
	
	@Override
	public void onUpdate()
	{
		int modulo = MC.player.age % 64;
		RegionPos region = RenderUtils.getCameraRegion();
		
		if(modulo == 0 || !region.equals(lastRegion))
		{
			if(vertexBuffer != null)
				vertexBuffer.close();
			
			vertexBuffer = EasyVertexBuffer.createAndUpload(DrawMode.QUADS,
				VertexFormats.POSITION_COLOR, buffer -> {
					for(int[] vertex : vertices)
						buffer.vertex(vertex[0] - region.x(), vertex[1],
							vertex[2] - region.z()).color(0xFFFFFFFF);
				});
			
			lastRegion = region;
		}
		
		// reset matching blocks
		if(modulo == 0)
			matchingBlocks.clear();
		
		int stepSize = MC.world.getHeight() / 64;
		int startY = MC.world.getTopYInclusive() - 1 - modulo * stepSize;
		int endY = startY - stepSize;
		
		BlockPos playerPos =
			BlockPos.ofFloored(MC.player.getX(), 0, MC.player.getZ());
		
		// search matching blocks
		loop: for(int y = startY; y > endY; y--)
			for(int x = 64; x > -64; x--)
				for(int z = 64; z > -64; z--)
				{
					if(matchingBlocks.size() >= 10000)
						break loop;
					
					BlockPos pos = new BlockPos(playerPos.getX() + x, y,
						playerPos.getZ() + z);
					
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
		vertices = BlockVertexCompiler.compile(matchingBlocks);
	}
}
