/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
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
import net.wurstclient.mixinterface.ISimpleOption;
import net.wurstclient.settings.BlockListSetting;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.ChatUtils;

@SearchTags({"XRay", "x ray", "OreFinder", "ore finder"})
public final class XRayHack extends Hack implements UpdateListener,
	SetOpaqueCubeListener, GetAmbientOcclusionLightLevelListener,
	ShouldDrawSideListener, TesselateBlockListener, RenderBlockEntityListener
{
	//private static final Set<Block> EXPOSED_NEIGHBOUR_BLOCKS = new HashSet<>(Arrays.asList(
	//		Blocks.AIR,
	//		Blocks.CAVE_AIR,
	//		Blocks.WATER,
	//		Blocks.LAVA,
	//		Blocks.VINE, // deep dark?
	//		Blocks.CAVE_VINES,
	//		Blocks.POINTED_DRIPSTONE,
	//		Blocks.BIG_DRIPLEAF_STEM,
	//		Blocks.BIG_DRIPLEAF,
	//		Blocks.SMALL_DRIPLEAF,
	//		Blocks.IRON_BARS, // stronghold?
	//		Blocks.IRON_DOOR,
	//		Blocks.OAK_FENCE // mineshaft?
	//));

	private final BlockListSetting ores = new BlockListSetting("Ores", "",
		"minecraft:ancient_debris", "minecraft:anvil", "minecraft:beacon",
		"minecraft:bone_block", "minecraft:bookshelf",
		"minecraft:brewing_stand", "minecraft:chain_command_block",
		"minecraft:chest", "minecraft:clay", "minecraft:coal_block",
		"minecraft:coal_ore", "minecraft:command_block", "minecraft:copper_ore",
		"minecraft:crafting_table", "minecraft:deepslate_coal_ore",
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

	private ArrayList<String> oreNames;

	//private final BlockListSetting exposedBlocks = new BlockListSetting("Exposed", "Blocks to filter as exposed",
	//		"minecraft:air",
	//		"minecraft:big_dripleaf",
	//		"minecraft:big_dripleaf_stem",
	//		"minecraft:big_pointed_dripstone",
	//		"minecraft:cave_air",
	//		"minecraft:cave_vines",
	//		"minecraft:iron_bars",
	//		"minecraft:iron_door",
	//		"minecraft:lava",
	//		"minecraft:oak_fence",
	//		"minecraft:pointed_dripstone",
	//		"minecraft:small_dripleaf",
	//		"minecraft:vine",
	//		"minecraft:water");

	//private ArrayList<String> exposedNames;

	private final CheckboxSetting showExposed = new CheckboxSetting("Show exposed", "Only show exposed ores touching air/water/lava.\nUseful for servers utilizing anti-X-Ray.\nReenable this hack if nothing appears changed.", false);

	// TODO does anyone use optifine anymore?
	// 		closed source + alternatives like sodium = why
	private final String warning;
	
	private final String renderName =
		Math.random() < 0.01 ? "X-Wurst" : getName();
	
	public XRayHack()
	{
		super("X-Ray");
		setCategory(Category.RENDER);
		addSetting(ores);
		//addSetting(exposedBlocks);
		addSetting(showExposed);
		
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
		return renderName;
	}
	
	@Override
	public void onEnable()
	{
		// TODO BlockList already contains an ArrayList, why not just use that?
		oreNames = new ArrayList<>(ores.getBlockNames());
		//exposedNames = new ArrayList<>(exposedBlocks.getBlockNames());
		
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

		@SuppressWarnings("unchecked")
		ISimpleOption<Double> gammaOption =
			(ISimpleOption<Double>)(Object)MC.options.getGamma();
		
		// TODO: Why does this use 0.5 instead of
		// FullBright's defaultGamma setting?
		if(!WURST.getHax().fullbrightHack.isEnabled())
			gammaOption.forceSetValue(0.5);
	}
	
	@Override
	public void onUpdate()
	{
		@SuppressWarnings("unchecked")
		ISimpleOption<Double> gammaOption =
			(ISimpleOption<Double>)(Object)MC.options.getGamma();
		
		gammaOption.forceSetValue(16.0);
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
		BlockPos pos = event.getBlockEntity().getPos();
		if(!isVisible(BlockUtils.getBlock(pos), pos))
			event.cancel();
	}
	
	public void openBlockListEditor(Screen prevScreen)
	{
		MC.setScreen(new EditBlockListScreen(prevScreen, ores));
	}

	private boolean isVisible(Block block, BlockPos pos)
	{
		String name = BlockUtils.getName(block);
		int index = Collections.binarySearch(oreNames, name);
		boolean visible = index >= 0;

		if (visible && showExposed.isChecked()) {
			// TODO this only considers blocks with no hitbox
			//		shouldnt blocks like fences, vines be considered exposed?
			//		this works for most cases so its probably redundant...
			return !BlockUtils.canBeClicked(pos.up())
					|| !BlockUtils.canBeClicked(pos.down())
					|| !BlockUtils.canBeClicked(pos.east())
					|| !BlockUtils.canBeClicked(pos.west())
					|| !BlockUtils.canBeClicked(pos.north())
					|| !BlockUtils.canBeClicked(pos.south());

			//return Collections.binarySearch(exposedNames, BlockUtils.getName(pos.up())) >= 0
			//		|| Collections.binarySearch(exposedNames, BlockUtils.getName(pos.down())) >= 0
			//		|| Collections.binarySearch(exposedNames, BlockUtils.getName(pos.east())) >= 0
			//		|| Collections.binarySearch(exposedNames, BlockUtils.getName(pos.west())) >= 0
			//		|| Collections.binarySearch(exposedNames, BlockUtils.getName(pos.north())) >= 0
			//		|| Collections.binarySearch(exposedNames, BlockUtils.getName(pos.south())) >= 0;

			//return EXPOSED_NEIGHBOUR_BLOCKS.contains(BlockUtils.getBlock(pos.up()))
			//		|| EXPOSED_NEIGHBOUR_BLOCKS.contains(BlockUtils.getBlock(pos.down()))
			//		|| EXPOSED_NEIGHBOUR_BLOCKS.contains(BlockUtils.getBlock(pos.east()))
			//		|| EXPOSED_NEIGHBOUR_BLOCKS.contains(BlockUtils.getBlock(pos.west()))
			//		|| EXPOSED_NEIGHBOUR_BLOCKS.contains(BlockUtils.getBlock(pos.north()))
			//		|| EXPOSED_NEIGHBOUR_BLOCKS.contains(BlockUtils.getBlock(pos.south()));
		}

		return visible;
	}
}
