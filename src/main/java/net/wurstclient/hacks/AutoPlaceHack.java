/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.util.BlockUtils;

@SearchTags({"bridge", "auto place"})
public final class AutoPlaceHack extends Hack implements UpdateListener
{
	private final CheckboxSetting onlyBelowFeet =
		new CheckboxSetting("Only Below Feet",
			"Only place blocks when target block is 1 block below your feet.",
			false);
	private final CheckboxSetting fastPlace =
		new CheckboxSetting("Always FastPlace",
			"Builds as if FastPlace was enabled, even if it's not.", true);
	
	public AutoPlaceHack()
	{
		super("AutoPlace");
		setCategory(Category.BLOCKS);
		addSetting(onlyBelowFeet);
		addSetting(fastPlace);
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		if(MC.currentScreen instanceof HandledScreen)
			return;
		
		if(MC.itemUseCooldown != 0 && !fastPlace.isChecked())
			return;
		
		ClientPlayerEntity player = MC.player;
		assert player != null;
		
		// Item in hand is not a block
		if(!(player.getInventory().getStack(player.getInventory().selectedSlot)
			.getItem() instanceof BlockItem))
			return;
		
		HitResult hitResult = MC.crosshairTarget;
		if(hitResult == null || hitResult.getType() != HitResult.Type.BLOCK)
			return;
		
		BlockHitResult blockHitResult = (BlockHitResult)hitResult;
		if(blockHitResult.getSide() == Direction.UP
			|| blockHitResult.getSide() == Direction.DOWN)
			return;
		
		if(!BlockUtils.canBeClicked(blockHitResult.getBlockPos()))
			return;
		
		BlockPos blockToPlacePos =
			blockHitResult.getBlockPos().offset(blockHitResult.getSide());
		if(!BlockUtils.getState(blockToPlacePos).isReplaceable())
			return;
		
		// Option: only below feet
		if(blockToPlacePos.getY() != BlockPos.ofFloored(MC.player.getPos())
			.down().getY() && onlyBelowFeet.isChecked())
			return;
		
		assert MC.interactionManager != null;
		MC.interactionManager.interactItem(player, Hand.MAIN_HAND);
		ActionResult actionResult = MC.interactionManager.interactBlock(player,
			Hand.MAIN_HAND, blockHitResult);
		if(actionResult.isAccepted())
			MC.player.swingHand(Hand.MAIN_HAND);
		
		MC.itemUseCooldown = 4;
	}
}
