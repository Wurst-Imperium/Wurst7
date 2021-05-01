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
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.block.Block;
import net.minecraft.client.gui.screen.Screen;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.clickgui.screens.EditBlockListScreen;
import net.wurstclient.events.GetAmbientOcclusionLightLevelListener;
import net.wurstclient.events.RenderBlockEntityListener;
import net.wurstclient.events.SetOpaqueCubeListener;
import net.wurstclient.events.ShouldDrawSideListener;
import net.wurstclient.events.TesselateBlockListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.BlockListSetting;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.ChatUtils;

@SearchTags({"XRay", "x ray", "OreFinder", "ore finder"})
public final class XRayHack extends Hack implements UpdateListener,
	SetOpaqueCubeListener, GetAmbientOcclusionLightLevelListener,
	ShouldDrawSideListener, TesselateBlockListener, RenderBlockEntityListener
{
	private final BlockListSetting ores = new BlockListSetting("Ores", "",
		"minecraft:ancient_debris", "minecraft:anvil", "minecraft:beacon",
		"minecraft:bone_block", "minecraft:bookshelf",
		"minecraft:brewing_stand", "minecraft:chain_command_block",
		"minecraft:chest", "minecraft:clay", "minecraft:coal_block",
		"minecraft:coal_ore", "minecraft:command_block",
		"minecraft:crafting_table", "minecraft:diamond_block",
		"minecraft:diamond_ore", "minecraft:dispenser", "minecraft:dropper",
		"minecraft:emerald_block", "minecraft:emerald_ore",
		"minecraft:enchanting_table", "minecraft:end_portal",
		"minecraft:end_portal_frame", "minecraft:ender_chest",
		"minecraft:furnace", "minecraft:glowstone", "minecraft:gold_block",
		"minecraft:gold_ore", "minecraft:hopper", "minecraft:iron_block",
		"minecraft:iron_ore", "minecraft:ladder", "minecraft:lapis_block",
		"minecraft:lapis_ore", "minecraft:lava", "minecraft:lodestone",
		"minecraft:mossy_cobblestone", "minecraft:nether_gold_ore",
		"minecraft:nether_portal", "minecraft:nether_quartz_ore",
		"minecraft:redstone_block", "minecraft:redstone_ore",
		"minecraft:repeating_command_block", "minecraft:spawner",
		"minecraft:tnt", "minecraft:torch", "minecraft:trapped_chest",
		"minecraft:water");
	
	private ArrayList<String> oreNames;
	private final String warning;
	
	private final String renderName =
		Math.random() < 0.01 ? "X-Wurst" : getName();
	
	public XRayHack()
	{
		super("X-Ray", "Allows you to see ores through walls.");
		setCategory(Category.RENDER);
		addSetting(ores);
		
		List<String> mods = FabricLoader.getInstance().getAllMods().stream()
			.map(ModContainer::getMetadata).map(ModMetadata::getId)
			.collect(Collectors.toList());
		
		Pattern sodium = Pattern.compile("sodium.*");
		Pattern optifine = Pattern.compile("opti(?:fine|fabric).*");
		
		if(mods.stream().anyMatch(sodium.asPredicate()))
			warning = "Sodium is installed. X-Ray will not work properly!";
		else if(mods.stream().anyMatch(optifine.asPredicate()))
			warning = "OptiFine is installed. X-Ray will not work properly!";
		else
			warning = null;
	}
	
	@Override
	public String getRenderName()
	{
		return renderName;
	}
	
	@Override
	public void onEnable()
	{
		oreNames = new ArrayList<>(ores.getBlockNames());
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(SetOpaqueCubeListener.class, this);
		EVENTS.add(GetAmbientOcclusionLightLevelListener.class, this);
		EVENTS.add(ShouldDrawSideListener.class, this);
		EVENTS.add(TesselateBlockListener.class, this);
		EVENTS.add(RenderBlockEntityListener.class, this);
		MC.worldRenderer.reload();
		
		if(warning != null)
			ChatUtils.warning(warning);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(SetOpaqueCubeListener.class, this);
		EVENTS.remove(GetAmbientOcclusionLightLevelListener.class, this);
		EVENTS.remove(ShouldDrawSideListener.class, this);
		EVENTS.remove(TesselateBlockListener.class, this);
		EVENTS.remove(RenderBlockEntityListener.class, this);
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
	
	public void openBlockListEditor(Screen prevScreen)
	{
		MC.openScreen(new EditBlockListScreen(prevScreen, ores));
	}
	
	private boolean isVisible(Block block)
	{
		String name = BlockUtils.getName(block);
		int index = Collections.binarySearch(oreNames, name);
		return index >= 0;
	}
}
