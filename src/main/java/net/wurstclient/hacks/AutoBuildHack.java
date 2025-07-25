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
import java.util.List;
import java.util.Map;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.events.RenderListener;
import net.wurstclient.events.RightClickListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.FileSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.*;
import net.wurstclient.util.BlockPlacer.BlockPlacingParams;
import net.wurstclient.util.json.JsonException;

public final class AutoBuildHack extends Hack
	implements UpdateListener, RightClickListener, RenderListener
{
	private static final Box BLOCK_BOX =
		new Box(1 / 16.0, 1 / 16.0, 1 / 16.0, 15 / 16.0, 15 / 16.0, 15 / 16.0);
	
	private final FileSetting templateSetting = new FileSetting("Template",
		"Determines what to build.\n\n"
			+ "Templates are just JSON files. Feel free to add your own or to edit / delete the default templates.\n\n"
			+ "If you mess up, simply press the 'Reset to Defaults' button or delete the folder.",
		"autobuild", DefaultAutoBuildTemplates::createFiles);
	
	private final SliderSetting range = new SliderSetting("Range",
		"How far to reach when placing blocks.\n" + "Recommended values:\n"
			+ "6.0 for vanilla\n" + "4.25 for NoCheat+",
		6, 1, 10, 0.05, ValueDisplay.DECIMAL);
	
	private final CheckboxSetting checkLOS = new CheckboxSetting(
		"Check line of sight",
		"Makes sure that you don't reach through walls when placing blocks. Can help with AntiCheat plugins but slows down building.",
		false);
	
	private final CheckboxSetting useSavedBlocks = new CheckboxSetting(
		"Use saved blocks",
		"Tries to place the same blocks that were saved in the template.\n\n"
			+ "If the template does not specify block types, it will be built"
			+ " from whatever block you are holding.",
		true);
	
	private final CheckboxSetting fastPlace =
		new CheckboxSetting("Always FastPlace",
			"Builds as if FastPlace was enabled, even if it's not.", true);
	
	private final CheckboxSetting strictBuildOrder = new CheckboxSetting(
		"Strict build order",
		"Places blocks in exactly the same order that they appear in the"
			+ " template. This is slower, but provides more consistent results.",
		false);
	
	private Status status = Status.NO_TEMPLATE;
	private AutoBuildTemplate template;
	private LinkedHashMap<BlockPos, Item> remainingBlocks =
		new LinkedHashMap<>();
	
	public AutoBuildHack()
	{
		super("AutoBuild");
		setCategory(Category.BLOCKS);
		addSetting(templateSetting);
		addSetting(range);
		addSetting(checkLOS);
		addSetting(useSavedBlocks);
		addSetting(fastPlace);
		addSetting(strictBuildOrder);
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
			
			case BUILDING:
			double total = template.size();
			double placed = total - remainingBlocks.size();
			double progress = Math.round(placed / total * 1e4) / 1e2;
			name += " [" + template.getName() + "] " + progress + "%";
			break;
		}
		
		return name;
	}
	
	@Override
	protected void onEnable()
	{
		WURST.getHax().instaBuildHack.setEnabled(false);
		WURST.getHax().templateToolHack.setEnabled(false);
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(RightClickListener.class, this);
		EVENTS.add(RenderListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(RightClickListener.class, this);
		EVENTS.remove(RenderListener.class, this);
		
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
		
		status = Status.BUILDING;
	}
	
	@Override
	public void onUpdate()
	{
		switch(status)
		{
			case NO_TEMPLATE:
			loadSelectedTemplate();
			break;
			
			case LOADING:
			break;
			
			case IDLE:
			if(!template.isSelected(templateSetting))
				loadSelectedTemplate();
			break;
			
			case BUILDING:
			buildNormally();
			break;
		}
	}
	
	@Override
	public void onRender(MatrixStack matrixStack, float partialTicks)
	{
		if(status != Status.BUILDING)
			return;
		
		List<BlockPos> blocksToDraw = remainingBlocks.keySet().stream()
			.filter(pos -> BlockUtils.getState(pos).isReplaceable()).limit(1024)
			.toList();
		
		int black = 0x80000000;
		List<Box> outlineBoxes =
			blocksToDraw.stream().map(pos -> BLOCK_BOX.offset(pos)).toList();
		RenderUtils.drawOutlinedBoxes(matrixStack, outlineBoxes, black, true);
		
		int green = 0x2600FF00;
		Vec3d eyesPos = RotationUtils.getEyesPos();
		double rangeSq = range.getValueSq();
		List<Box> greenBoxes = blocksToDraw.stream()
			.filter(pos -> pos.getSquaredDistance(eyesPos) <= rangeSq)
			.map(pos -> BLOCK_BOX.offset(pos)).toList();
		RenderUtils.drawSolidBoxes(matrixStack, greenBoxes, green, true);
	}
	
	private void buildNormally()
	{
		remainingBlocks.keySet()
			.removeIf(pos -> !BlockUtils.getState(pos).isReplaceable());
		
		if(remainingBlocks.isEmpty())
		{
			status = Status.IDLE;
			return;
		}
		
		if(!fastPlace.isChecked() && MC.itemUseCooldown > 0)
			return;
		
		double rangeSq = range.getValueSq();
		for(Map.Entry<BlockPos, Item> entry : remainingBlocks.entrySet())
		{
			BlockPos pos = entry.getKey();
			Item item = entry.getValue();
			
			BlockPlacingParams params = BlockPlacer.getBlockPlacingParams(pos);
			if(params == null || params.distanceSq() > rangeSq
				|| checkLOS.isChecked() && !params.lineOfSight())
				if(strictBuildOrder.isChecked())
					return;
				else
					continue;
				
			if(useSavedBlocks.isChecked() && item != Items.AIR
				&& !MC.player.getMainHandStack().isOf(item))
			{
				giveOrSelectItem(item);
				return;
			}
			
			MC.itemUseCooldown = 4;
			RotationUtils.getNeededRotations(params.hitVec())
				.sendPlayerLookPacket();
			InteractionSimulator.rightClickBlock(params.toHitResult());
			return;
		}
	}
	
	private void giveOrSelectItem(Item item)
	{
		if(InventoryUtils.selectItem(item, 36, true))
			return;
		
		if(!MC.player.isInCreativeMode())
			return;
		
		PlayerInventory inventory = MC.player.getInventory();
		int slot = inventory.getEmptySlot();
		if(slot < 0)
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
	
	public Path getFolder()
	{
		return templateSetting.getFolder();
	}
	
	private enum Status
	{
		NO_TEMPLATE,
		LOADING,
		IDLE,
		BUILDING;
	}
}
