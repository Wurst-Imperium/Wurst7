/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.wurstclient.Category;
import net.wurstclient.events.RightClickListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.FileSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.settings.SwingHandSetting.SwingHand;
import net.wurstclient.util.AutoBuildTemplate;
import net.wurstclient.util.BlockPlacer;
import net.wurstclient.util.BlockPlacer.BlockPlacingParams;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.InteractionSimulator;
import net.wurstclient.util.InventoryUtils;
import net.wurstclient.util.json.JsonException;

public final class InstaBuildHack extends Hack
	implements UpdateListener, RightClickListener
{
	private final FileSetting templateSetting = new FileSetting("Template",
		"Determines what to build.\n\n"
			+ "Templates are just JSON files. Feel free to add your own or to edit / delete the default templates.\n\n"
			+ "If you mess up, simply press the 'Reset to Defaults' button or delete the folder.",
		"autobuild", path -> {});
	
	private final SliderSetting range = new SliderSetting("Range",
		"How far to reach when placing blocks.\n" + "Recommended values:\n"
			+ "6.0 for vanilla\n" + "4.25 for NoCheat+",
		6, 1, 10, 0.05, ValueDisplay.DECIMAL);
	
	private final CheckboxSetting useSavedBlocks = new CheckboxSetting(
		"Use saved blocks",
		"Tries to place the same blocks that were saved in the template.\n\n"
			+ "If the template does not specify block types, it will be built"
			+ " from whatever block you are holding.",
		false);
	
	private Status status = Status.NO_TEMPLATE;
	private AutoBuildTemplate template;
	private LinkedHashMap<BlockPos, Item> remainingBlocks =
		new LinkedHashMap<>();
	
	public InstaBuildHack()
	{
		super("InstaBuild");
		setCategory(Category.BLOCKS);
		addSetting(templateSetting);
		addSetting(range);
		addSetting(useSavedBlocks);
	}
	
	@Override
	public String getRenderName()
	{
		String name = getName();
		
		switch(status)
		{
			case NO_TEMPLATE:
			break;
			
			case LOADING:
			name += " [Loading...]";
			break;
			
			case IDLE:
			name += " [" + template.getName() + "]";
			break;
		}
		
		return name;
	}
	
	@Override
	protected void onEnable()
	{
		WURST.getHax().autoBuildHack.setEnabled(false);
		WURST.getHax().templateToolHack.setEnabled(false);
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(RightClickListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(RightClickListener.class, this);
		
		remainingBlocks.clear();
		
		if(template == null)
			status = Status.NO_TEMPLATE;
		else
			status = Status.IDLE;
	}
	
	@Override
	public void onRightClick(RightClickEvent event)
	{
		if(status != Status.IDLE)
			return;
		
		HitResult hitResult = MC.crosshairTarget;
		if(hitResult == null || hitResult.getType() != HitResult.Type.BLOCK
			|| !(hitResult instanceof BlockHitResult blockHitResult))
			return;
		
		BlockPos hitResultPos = blockHitResult.getBlockPos();
		if(!BlockUtils.canBeClicked(hitResultPos))
			return;
		
		BlockPos startPos = hitResultPos.offset(blockHitResult.getSide());
		Direction direction = MC.player.getHorizontalFacing();
		remainingBlocks = template.getBlocksToPlace(startPos, direction);
		
		buildInstantly();
	}
	
	@Override
	public void onUpdate()
	{
		switch(status)
		{
			case NO_TEMPLATE:
			loadSelectedTemplate();
			break;
			
			default:
			case LOADING:
			break;
			
			case IDLE:
			if(!template.isSelected(templateSetting))
				loadSelectedTemplate();
			break;
		}
	}
	
	private void buildInstantly()
	{
		PlayerInventory inventory = MC.player.getInventory();
		int oldSlot = inventory.getSelectedSlot();
		
		for(Map.Entry<BlockPos, Item> entry : remainingBlocks.entrySet())
		{
			BlockPos pos = entry.getKey();
			Item item = entry.getValue();
			
			if(!BlockUtils.getState(pos).isReplaceable())
				continue;
			
			BlockPlacingParams params = BlockPlacer.getBlockPlacingParams(pos);
			if(params == null || params.distanceSq() > range.getValueSq())
				continue;
			
			if(useSavedBlocks.isChecked() && item != Items.AIR
				&& !MC.player.getMainHandStack().isOf(item))
				giveOrSelectItem(item);
			
			InteractionSimulator.rightClickBlock(params.toHitResult(),
				SwingHand.OFF);
		}
		
		inventory.setSelectedSlot(oldSlot);
		remainingBlocks.clear();
	}
	
	private void giveOrSelectItem(Item item)
	{
		if(InventoryUtils.selectItem(item, 9))
			return;
		
		if(!MC.player.isInCreativeMode())
			return;
		
		PlayerInventory inventory = MC.player.getInventory();
		int slot = inventory.getEmptySlot();
		if(!PlayerInventory.isValidHotbarIndex(slot))
			slot = inventory.getSelectedSlot();
		
		ItemStack stack = new ItemStack(item);
		InventoryUtils.setCreativeStack(slot, stack);
	}
	
	private void loadSelectedTemplate()
	{
		status = Status.LOADING;
		Path path = templateSetting.getSelectedFile();
		
		try
		{
			template = AutoBuildTemplate.load(path);
			status = Status.IDLE;
			
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
	
	private enum Status
	{
		NO_TEMPLATE,
		LOADING,
		IDLE;
	}
}
