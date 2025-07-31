/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
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
import net.minecraft.util.math.Direction;
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
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
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
		"minecraft:amethyst_cluster", "minecraft:ancient_debris",
		"minecraft:anvil", "minecraft:beacon", "minecraft:bone_block",
		"minecraft:bookshelf", "minecraft:brewing_stand",
		"minecraft:budding_amethyst", "minecraft:chain_command_block",
		"minecraft:chest", "minecraft:coal_block", "minecraft:coal_ore",
		"minecraft:command_block", "minecraft:copper_ore", "minecraft:crafter",
		"minecraft:crafting_table", "minecraft:creaking_heart",
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
		"minecraft:sculk_catalyst", "minecraft:sculk_sensor",
		"minecraft:sculk_shrieker", "minecraft:spawner",
		"minecraft:suspicious_gravel", "minecraft:suspicious_sand",
		"minecraft:tnt", "minecraft:torch", "minecraft:trapped_chest",
		"minecraft:trial_spawner", "minecraft:vault", "minecraft:wall_torch",
		"minecraft:water");
	
	private final CheckboxSetting onlyExposed = new CheckboxSetting(
		"Only show exposed",
		"Only shows ores that would be visible in caves. This can help against"
			+ " anti-X-Ray plugins.\n\n"
			+ "Remember to restart X-Ray when changing this setting.",
		false);
	
	private final SliderSetting opacity = new SliderSetting("Opacity",
		"Opacity of non-ore blocks when X-Ray is enabled.\n\n"
			+ "Remember to restart X-Ray when changing this setting.",
		0, 0, 0.99, 0.01, ValueDisplay.PERCENTAGE.withLabel(0, "off"));
	
	private final String optiFineWarning;
	private final String renderName =
		Math.random() < 0.01 ? "X-Wurst" : getName();
	
	private ArrayList<String> oreNamesCache;
	private final ThreadLocal<BlockPos.Mutable> mutablePosForExposedCheck =
		ThreadLocal.withInitial(BlockPos.Mutable::new);
	
	public XRayHack()
	{
		super("X-Ray");
		setCategory(Category.RENDER);
		addSetting(ores);
		addSetting(onlyExposed);
		addSetting(opacity);
		optiFineWarning = checkOptiFine();
	}
	
	@Override
	public String getRenderName()
	{
		return renderName;
	}
	
	@Override
	protected void onEnable()
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
	protected void onDisable()
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
		boolean visible =
			isVisible(event.getState().getBlock(), event.getPos());
		if(!visible && opacity.getValue() > 0)
			return;
		
		event.setRendered(visible);
	}
	
	@Override
	public void onRenderBlockEntity(RenderBlockEntityEvent event)
	{
		BlockPos pos = event.getBlockEntity().getPos();
		if(!isVisible(BlockUtils.getBlock(pos), pos))
			event.cancel();
	}
	
	public boolean isVisible(Block block, BlockPos pos)
	{
		String name = BlockUtils.getName(block);
		int index = Collections.binarySearch(oreNamesCache, name);
		boolean visible = index >= 0;
		
		if(visible && onlyExposed.isChecked() && pos != null)
			return isExposed(pos);
		
		return visible;
	}
	
	private boolean isExposed(BlockPos pos)
	{
		BlockPos.Mutable mutablePos = mutablePosForExposedCheck.get();
		for(Direction direction : Direction.values())
			if(!BlockUtils.isOpaqueFullCube(mutablePos.set(pos, direction)))
				return true;
			
		return false;
	}
	
	public boolean isOpacityMode()
	{
		return isEnabled() && opacity.getValue() > 0;
	}
	
	public int getOpacityColorMask()
	{
		return (int)(opacity.getValue() * 255) << 24 | 0xFFFFFF;
	}
	
	public float getOpacityFloat()
	{
		return opacity.getValueF();
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
	
	// See AbstractBlockRenderContextMixin, RenderLayersMixin
}
