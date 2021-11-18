/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.block.Block;
import net.minecraft.client.util.math.MatrixStack;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.BlockMatchHack;
import net.wurstclient.settings.BlockListSetting;
import net.wurstclient.settings.ColorSetting;
import net.wurstclient.util.BlockUtils;

import java.awt.*;
import java.util.Set;
import java.util.stream.Collectors;

@SearchTags({"base finder", "factions"})
public final class BaseFinderHack extends BlockMatchHack
	implements UpdateListener, RenderListener
{
	private final BlockListSetting naturalBlocks = new BlockListSetting(
		"Natural Blocks",
		"These blocks will be considered\n" + "part of natural generation.\n\n"
			+ "They will NOT be highlighted\n" + "as player bases.",
			this::updateBlockMatcher,
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
		"Man-made blocks will be\n" + "highlighted in this color.", Color.RED);

	private int cachedMatchingCount = 0;

	public BaseFinderHack()
	{
		super("BaseFinder");
		setCategory(Category.RENDER);
		addSetting(naturalBlocks);
		addSetting(color);
		setDisplayStyle(DisplayStyle.BLOCKS);
	}
	
	@Override
	public String getRenderName()
	{
		String name = getName() + " [";
		
		// counter
		if(cachedMatchingCount >= 10000)
			name += "10000+ blocks";
		else if(cachedMatchingCount == 1)
			name += "1 block";
		else if(cachedMatchingCount == 0)
			name += "nothing";
		else
			name += cachedMatchingCount + " blocks";
		
		name += " found]";
		return name;
	}

	private void updateBlockMatcher()
	{
		final Set<Block> blocks = naturalBlocks.getBlockNames().stream().map(
			BlockUtils::getBlockFromName).collect(Collectors.toSet());
		setBlockMatcher(b -> !blocks.contains(b));
	}

	@Override
	public void onEnable()
	{
		super.onEnable();

		updateBlockMatcher();
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		super.onDisable();
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		float[] colorF = color.getColorF();
		render(matrixStack, colorF[0], colorF[1], colorF[2], 0.15F);
	}
	
	@Override
	public void onUpdate()
	{
		updateSearch();

		if((MC.player.age & 31) == 0 && matchingWorld != null)
		{
			cachedMatchingCount = matchingWorld.chunks().stream().mapToInt(c -> c.getMatchChunk().getBitsSetAtLeastOnce()).sum();
		}
	}
}
