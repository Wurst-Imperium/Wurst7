/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.CraftingTableBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MushroomStewItem;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"auto soup", "AutoStew", "auto stew"})
public final class AutoSoupHack extends Hack implements UpdateListener
{
	private final SliderSetting health = new SliderSetting("Health",
		"Eats a soup when your health\n"
			+ "reaches this value or falls below it.",
		6.5, 0.5, 9.5, 0.5, ValueDisplay.DECIMAL);
	
	private int oldSlot = -1;
	
	public AutoSoupHack()
	{
		super("AutoSoup", "Automatically eats soup when your health is low.\n\n"
			+ "\u00a7lNote:\u00a7r This hack ignores hunger and assumes that eating\n"
			+ "soup directly refills your health. If the server you are\n"
			+ "playing on is not configured to do that, use AutoEat instead.");
		
		setCategory(Category.COMBAT);
		addSetting(health);
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
		stopIfEating();
	}
	
	@Override
	public void onUpdate()
	{
		// sort empty bowls
		for(int i = 0; i < 36; i++)
		{
			// filter out non-bowl items and empty bowl slot
			ItemStack stack = MC.player.inventory.getStack(i);
			if(stack == null || stack.getItem() != Items.BOWL || i == 9)
				continue;
			
			// check if empty bowl slot contains a non-bowl item
			ItemStack emptyBowlStack = MC.player.inventory.getStack(9);
			boolean swap = !emptyBowlStack.isEmpty()
				&& emptyBowlStack.getItem() != Items.BOWL;
			
			// place bowl in empty bowl slot
			IMC.getInteractionManager().windowClick_PICKUP(i < 9 ? 36 + i : i);
			IMC.getInteractionManager().windowClick_PICKUP(9);
			
			// place non-bowl item from empty bowl slot in current slot
			if(swap)
				IMC.getInteractionManager()
					.windowClick_PICKUP(i < 9 ? 36 + i : i);
		}
		
		// search soup in hotbar
		int soupInHotbar = findSoup(0, 9);
		
		// check if any soup was found
		if(soupInHotbar != -1)
		{
			// check if player should eat soup
			if(!shouldEatSoup())
			{
				stopIfEating();
				return;
			}
			
			// save old slot
			if(oldSlot == -1)
				oldSlot = MC.player.inventory.selectedSlot;
			
			// set slot
			MC.player.inventory.selectedSlot = soupInHotbar;
			
			// eat soup
			MC.options.keyUse.setPressed(true);
			IMC.getInteractionManager().rightClickItem();
			
			return;
		}
		
		stopIfEating();
		
		// search soup in inventory
		int soupInInventory = findSoup(9, 36);
		
		// move soup in inventory to hotbar
		if(soupInInventory != -1)
			IMC.getInteractionManager().windowClick_QUICK_MOVE(soupInInventory);
	}
	
	private int findSoup(int startSlot, int endSlot)
	{
		for(int i = startSlot; i < endSlot; i++)
		{
			ItemStack stack = MC.player.inventory.getStack(i);
			
			if(stack != null && stack.getItem() instanceof MushroomStewItem)
				return i;
		}
		
		return -1;
	}
	
	private boolean shouldEatSoup()
	{
		// check health
		if(MC.player.getHealth() > health.getValueF() * 2F)
			return false;
		
		// check for clickable objects
		if(isClickable(MC.crosshairTarget))
			return false;
		
		return true;
	}
	
	private boolean isClickable(HitResult hitResult)
	{
		if(hitResult == null)
			return false;
		
		if(hitResult instanceof EntityHitResult)
		{
			Entity entity = ((EntityHitResult)MC.crosshairTarget).getEntity();
			return entity instanceof VillagerEntity
				|| entity instanceof TameableEntity;
		}
		
		if(hitResult instanceof BlockHitResult)
		{
			BlockPos pos = ((BlockHitResult)MC.crosshairTarget).getBlockPos();
			if(pos == null)
				return false;
			
			Block block = MC.world.getBlockState(pos).getBlock();
			return block instanceof BlockWithEntity
				|| block instanceof CraftingTableBlock;
		}
		
		return false;
	}
	
	private void stopIfEating()
	{
		// check if eating
		if(oldSlot == -1)
			return;
		
		// stop eating
		MC.options.keyUse.setPressed(false);
		
		// reset slot
		MC.player.inventory.selectedSlot = oldSlot;
		oldSlot = -1;
	}
}
