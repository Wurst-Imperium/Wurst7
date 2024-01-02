/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.block.Block;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.math.BlockPos;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.clickgui.screens.EditBlockListScreen;
import net.wurstclient.events.GetAmbientOcclusionLightLevelListener;
import net.wurstclient.events.RenderBlockEntityListener;
import net.wurstclient.events.SetOpaqueCubeListener;
import net.wurstclient.events.ShouldDrawSideListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.ISimpleOption;
import net.wurstclient.settings.BlockListSetting;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.ChatUtils;

@SearchTags({"XRay", "x ray", "OreFinder", "ore finder"})
public final class XRayHack extends Hack implements UpdateListener,
	SetOpaqueCubeListener, GetAmbientOcclusionLightLevelListener,
	ShouldDrawSideListener, RenderBlockEntityListener
{
	private final BlockListSetting ores = new BlockListSetting("Ores",
		"A list of blocks that X-Ray will show. They don't have to be just ores"
			+ " - you can add any block you want.\n\n"
			+ "Remember to restart X-Ray when changing this setting.",
		"minecraft:ancient_debris", "minecraft:anvil", "minecraft:beacon",
		"minecraft:bone_block", "minecraft:bookshelf",
		"minecraft:brewing_stand", "minecraft:chain_command_block",
		"minecraft:chest", "minecraft:clay", "minecraft:coal_block",
		"minecraft:coal_ore", "minecraft:command_block", "minecraft:copper_ore",
		"minecraft:crafter", "minecraft:crafting_table",
		"minecraft:decorated_pot", "minecraft:deepslate_coal_ore",
		"minecraft:deepslate_copper_ore", "minecraft:deepslate_diamond_ore",
		"minecraft:deepslate_emerald_ore", "minecraft:deepslate_gold_ore",
		"minecraft:deepslate_iron_ore", "minecraft:deepslate_lapis_ore",
		"minecraft:deepslate_redstone_ore", "minecraft:diamond_block",
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
		"minecraft:raw_copper_block", "minecraft:raw_gold_block",
		"minecraft:raw_iron_block", "minecraft:redstone_block",
		"minecraft:redstone_ore", "minecraft:repeating_command_block",
		"minecraft:spawner", "minecraft:suspicious_gravel",
		"minecraft:suspicious_sand", "minecraft:tnt", "minecraft:torch",
		"minecraft:trapped_chest", "minecraft:water");
	
	private final CheckboxSetting onlyExposed = new CheckboxSetting(
		"Only show exposed",
		"Only shows ores that would be visible in caves. This can help against"
			+ " anti-X-Ray plugins.\n\n"
			+ "Remember to restart X-Ray when changing this setting.",
		false);
	
	private final String optiFineWarning;
	private final String renderName =
		Math.random() < 0.01 ? "X-Wurst" : getName();
	
	private ArrayList<String> oreNamesCache;
	
	public XRayHack()
	{
		super("X-Ray");
		setCategory(Category.RENDER);
		addSetting(ores);
		addSetting(onlyExposed);
		optiFineWarning = checkOptiFine();
	}
	
	@Override
	public String getRenderName()
	{
		return renderName;
	}
	
	@Override
	public void onEnable()
	{
		// cache block names in case the setting changes while X-Ray is enabled
		oreNamesCache = new ArrayList<>(ores.getBlockNames());
		
		// add event listeners
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(SetOpaqueCubeListener.class, this);
		EVENTS.add(GetAmbientOcclusionLightLevelListener.class, this);
		EVENTS.add(ShouldDrawSideListener.class, this);
		EVENTS.add(RenderBlockEntityListener.class, this);
		
		// reload chunks
		MC.worldRenderer.reload();
		
		// display warning if OptiFine is detected
		if(optiFineWarning != null)
			ChatUtils.warning(optiFineWarning);
	}
	
	@Override
	public void onDisable()
	{
		// remove event listeners
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(SetOpaqueCubeListener.class, this);
		EVENTS.remove(GetAmbientOcclusionLightLevelListener.class, this);
		EVENTS.remove(ShouldDrawSideListener.class, this);
		EVENTS.remove(RenderBlockEntityListener.class, this);
		
		// reload chunks
		MC.worldRenderer.reload();
		
		// reset gamma
		FullbrightHack fullbright = WURST.getHax().fullbrightHack;
		if(!fullbright.isChangingGamma())
			ISimpleOption.get(MC.options.getGamma())
				.forceSetValue(fullbright.getDefaultGamma());
	}
	
	@Override
	public void onUpdate()
	{
		// force gamma to 16 so that ores are bright enough to see
		ISimpleOption.get(MC.options.getGamma()).forceSetValue(16.0);
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
		event.setRendered(
			isVisible(event.getState().getBlock(), event.getPos()));
	}
	
	@Override
	public void onRenderBlockEntity(RenderBlockEntityEvent event)
	{
		BlockPos pos = event.getBlockEntity().getPos();
		if(!isVisible(BlockUtils.getBlock(pos), pos))
			event.cancel();
	}
	
	private boolean isVisible(Block block, BlockPos pos)
	{
		String name = BlockUtils.getName(block);
		int index = Collections.binarySearch(oreNamesCache, name);
		boolean visible = index >= 0;
		
		if(visible && onlyExposed.isChecked() && pos != null)
			return !BlockUtils.isOpaqueFullCube(pos.up())
				|| !BlockUtils.isOpaqueFullCube(pos.down())
				|| !BlockUtils.isOpaqueFullCube(pos.east())
				|| !BlockUtils.isOpaqueFullCube(pos.west())
				|| !BlockUtils.isOpaqueFullCube(pos.north())
				|| !BlockUtils.isOpaqueFullCube(pos.south());
		
		return visible;
	}
	
	/**
	 * Checks if OptiFine/OptiFabric is installed and returns a warning message
	 * if it is.
	 */
	private String checkOptiFine()
	{
		Stream<String> mods = FabricLoader.getInstance().getAllMods().stream()
			.map(ModContainer::getMetadata).map(ModMetadata::getId);
		
		Pattern optifine = Pattern.compile("opti(?:fine|fabric).*");
		
		if(mods.anyMatch(optifine.asPredicate()))
			return "OptiFine is installed. X-Ray will not work properly!";
		
		return null;
	}
	
	public void openBlockListEditor(Screen prevScreen)
	{
		MC.setScreen(new EditBlockListScreen(prevScreen, ores));
	}
}
