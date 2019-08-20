/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.Collections;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.GetAmbientOcclusionLightLevelListener;
import net.wurstclient.events.RenderBlockEntityListener;
import net.wurstclient.events.SetOpaqueCubeListener;
import net.wurstclient.events.ShouldDrawSideListener;
import net.wurstclient.events.TesselateBlockListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.BlockListSetting;
import net.wurstclient.util.BlockUtils;

@SearchTags({"XRay", "x ray", "OreFinder", "ore finder"})
public final class XRayHack extends Hack implements UpdateListener,
	SetOpaqueCubeListener, GetAmbientOcclusionLightLevelListener,
	ShouldDrawSideListener, TesselateBlockListener, RenderBlockEntityListener
{
	private final BlockListSetting ores = new BlockListSetting("Ores",
		Blocks.COAL_ORE, Blocks.COAL_BLOCK, Blocks.IRON_ORE, Blocks.IRON_BLOCK,
		Blocks.GOLD_ORE, Blocks.GOLD_BLOCK, Blocks.LAPIS_ORE,
		Blocks.LAPIS_BLOCK, Blocks.REDSTONE_ORE, Blocks.REDSTONE_BLOCK,
		Blocks.DIAMOND_ORE, Blocks.DIAMOND_BLOCK, Blocks.EMERALD_ORE,
		Blocks.EMERALD_BLOCK, Blocks.NETHER_QUARTZ_ORE, Blocks.CLAY,
		Blocks.BONE_BLOCK, Blocks.GLOWSTONE, Blocks.CRAFTING_TABLE,
		Blocks.FURNACE, Blocks.TORCH, Blocks.LADDER, Blocks.TNT,
		Blocks.ENCHANTING_TABLE, Blocks.BOOKSHELF, Blocks.ANVIL,
		Blocks.BREWING_STAND, Blocks.BEACON, Blocks.CHEST, Blocks.TRAPPED_CHEST,
		Blocks.ENDER_CHEST, Blocks.HOPPER, Blocks.DROPPER, Blocks.DISPENSER,
		Blocks.WATER, Blocks.LAVA, Blocks.MOSSY_COBBLESTONE, Blocks.SPAWNER,
		Blocks.NETHER_PORTAL, Blocks.END_PORTAL, Blocks.END_PORTAL_FRAME,
		Blocks.COMMAND_BLOCK, Blocks.CHAIN_COMMAND_BLOCK,
		Blocks.REPEATING_COMMAND_BLOCK);
	
	private ArrayList<String> oreNames;
	
	public XRayHack()
	{
		super("X-Ray", "Allows you to see ores through walls.");
		setCategory(Category.RENDER);
		addSetting(ores);
	}
	
	@Override
	public String getRenderName()
	{
		return "X-Wurst";
	}
	
	@Override
	public void onEnable()
	{
		oreNames = new ArrayList<>(ores.getBlockNames());
		
		WURST.getEventManager().add(UpdateListener.class, this);
		WURST.getEventManager().add(SetOpaqueCubeListener.class, this);
		WURST.getEventManager().add(GetAmbientOcclusionLightLevelListener.class,
			this);
		WURST.getEventManager().add(ShouldDrawSideListener.class, this);
		WURST.getEventManager().add(TesselateBlockListener.class, this);
		WURST.getEventManager().add(RenderBlockEntityListener.class, this);
		MC.worldRenderer.reload();
	}
	
	@Override
	public void onDisable()
	{
		WURST.getEventManager().remove(UpdateListener.class, this);
		WURST.getEventManager().remove(SetOpaqueCubeListener.class, this);
		WURST.getEventManager()
			.remove(GetAmbientOcclusionLightLevelListener.class, this);
		WURST.getEventManager().remove(ShouldDrawSideListener.class, this);
		WURST.getEventManager().remove(TesselateBlockListener.class, this);
		WURST.getEventManager().remove(RenderBlockEntityListener.class, this);
		MC.worldRenderer.reload();
		
		if(!WURST.getHax().fullbrightHack.isEnabled())
			MC.options.gamma = 0.5F;
	}
	
	@Override
	public void onUpdate()
	{
		MC.options.gamma = 16;
	}
	
	@Override
	public void onSetOpaqueCube(SetOpaqueCubeEvent event)
	{
		event.cancel();
	}
	
	@Override
	public void onGetAmbientOcclusionLightLevel(
		GetAmbientOcclusionLightLevelEvent event)
	{
		event.setLightLevel(1);
	}
	
	@Override
	public void onShouldDrawSide(ShouldDrawSideEvent event)
	{
		event.setRendered(isVisible(event.getState().getBlock()));
	}
	
	@Override
	public void onTesselateBlock(TesselateBlockEvent event)
	{
		if(!isVisible(event.getState().getBlock()))
			event.cancel();
	}
	
	@Override
	public void onRenderBlockEntity(RenderBlockEntityEvent event)
	{
		if(!isVisible(BlockUtils.getBlock(event.getBlockEntity().getPos())))
			event.cancel();
	}
	
	private boolean isVisible(Block block)
	{
		String name = BlockUtils.getName(block);
		int index = Collections.binarySearch(oreNames, name);
		return index >= 0;
	}
}
