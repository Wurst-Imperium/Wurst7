/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
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
import net.minecraft.util.math.BlockPos;
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
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.ChatUtils;

@SearchTags({"XRay", "x ray", "OreFinder", "ore finder"})
public final class XRayHack extends Hack implements UpdateListener,
	SetOpaqueCubeListener, GetAmbientOcclusionLightLevelListener,
	ShouldDrawSideListener, TesselateBlockListener, RenderBlockEntityListener
{
	private final CheckboxSetting legitMode = new CheckboxSetting("Legit mode",
		"Only reveals blocks that can be legitimately seen,\n"
				+ "where at least one side is next to a non-solid block.\n\n"
				+ "Can be used to bypass anti-xray plugins like Orebfuscator\n"
				+ "which replace fully concealed blocks with random ores.",
		false);

	private final BlockListSetting ores = new BlockListSetting("Ores", "",
		"minecraft:ancient_debris", "minecraft:anvil", "minecraft:beacon",
		"minecraft:bone_block", "minecraft:bookshelf",
		"minecraft:brewing_stand", "minecraft:chain_command_block",
		"minecraft:chest", "minecraft:clay", "minecraft:coal_block",
		"minecraft:coal_ore", "minecraft:command_block", "minecraft:copper_ore",
		"minecraft:crafting_table", "minecraft:deepslate_coal_ore",
		"minecraft:deepslate_copper_ore", "minecraft:deepslate_diamond_ore",
		"minecraft:deepslate_gold_ore", "minecraft:deepslate_iron_ore",
		"minecraft:deepslate_lapis_ore", "minecraft:deepslate_redstone_ore",
		"minecraft:diamond_block", "minecraft:diamond_ore",
		"minecraft:dispenser", "minecraft:dropper", "minecraft:emerald_block",
		"minecraft:emerald_ore", "minecraft:enchanting_table",
		"minecraft:end_portal", "minecraft:end_portal_frame",
		"minecraft:ender_chest", "minecraft:furnace", "minecraft:glowstone",
		"minecraft:gold_block", "minecraft:gold_ore", "minecraft:hopper",
		"minecraft:iron_block", "minecraft:iron_ore", "minecraft:ladder",
		"minecraft:lapis_block", "minecraft:lapis_ore", "minecraft:lava",
		"minecraft:lodestone", "minecraft:mossy_cobblestone",
		"minecraft:nether_gold_ore", "minecraft:nether_portal",
		"minecraft:nether_quartz_ore", "minecraft:raw_copper_block",
		"minecraft:raw_gold_block", "minecraft:raw_iron_block",
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
		super("X-Ray");
		setCategory(Category.RENDER);
		addSetting(legitMode);
		addSetting(ores);
		
		List<String> mods = FabricLoader.getInstance().getAllMods().stream()
			.map(ModContainer::getMetadata).map(ModMetadata::getId)
			.collect(Collectors.toList());
		
		Pattern optifine = Pattern.compile("opti(?:fine|fabric).*");
		
		if(mods.stream().anyMatch(optifine.asPredicate()))
			warning = "OptiFine is installed. X-Ray will not work properly!";
		else
			warning = null;
	}
	
	@Override
	public String getRenderName()
	{
		return renderName + (legitMode.isChecked() ? " Legit" : "");
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
		event.setRendered(isVisible(event.getState().getBlock(), event.getPos()));
	}
	
	@Override
	public void onTesselateBlock(TesselateBlockEvent event)
	{
		if(!isVisible(event.getState().getBlock(), event.getPos()))
			event.cancel();
	}
	
	@Override
	public void onRenderBlockEntity(RenderBlockEntityEvent event)
	{
		if(!isVisible(BlockUtils.getBlock(event.getBlockEntity().getPos()), event.getBlockEntity().getPos()))
			event.cancel();
	}
	
	public void openBlockListEditor(Screen prevScreen)
	{
		MC.setScreen(new EditBlockListScreen(prevScreen, ores));
	}

	private boolean isOpaque(BlockPos pos)
	{
		return BlockUtils.getState(pos).isOpaque();
	}

	private boolean isFullyConcealed(BlockPos pos)
	{
		return isOpaque(pos.up()) && isOpaque(pos.down()) && isOpaque(pos.north()) && isOpaque(pos.south()) && isOpaque(pos.east()) && isOpaque(pos.west());
	}

	private boolean isVisible(Block block, BlockPos pos)
	{
		if (legitMode.isChecked() && isFullyConcealed(pos))
			return false;

		String name = BlockUtils.getName(block);
		int index = Collections.binarySearch(oreNames, name);
		return index >= 0;
	}
}
