/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.util.math.BlockPos;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.BlockBreakingProgressListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.util.BlockUtils;

@SearchTags({"auto tool", "AutoSwitch", "auto switch"})
public final class AutoToolHack extends Hack
	implements BlockBreakingProgressListener, UpdateListener
{
	private final CheckboxSetting useSwords = new CheckboxSetting("用剑",
		"用剑砍断树叶,蜘蛛网等", false);
	
	private final CheckboxSetting useHands =
		new CheckboxSetting(
			"用手", "当没有合适的工具时",
			true);
	
	private final CheckboxSetting repairMode = new CheckboxSetting(
		"维修方式", "不使用耐久值即将为0的工具", false);
	
	private final CheckboxSetting switchBack = new CheckboxSetting(
		"回选", "使用工具后，自动切换回之前选择的物品槽",
		true);
	
	private int prevSelectedSlot;
	
	public AutoToolHack()
	{
		super("自动换工具", "当您破坏一个方块时,\n自动从物品栏中选择最快且最适用工具");
		
		setCategory(Category.BLOCKS);
		addSetting(useSwords);
		addSetting(useHands);
		addSetting(repairMode);
		addSetting(switchBack);
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(BlockBreakingProgressListener.class, this);
		EVENTS.add(UpdateListener.class, this);
		prevSelectedSlot = -1;
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(BlockBreakingProgressListener.class, this);
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onBlockBreakingProgress(BlockBreakingProgressEvent event)
	{
		BlockPos pos = event.getBlockPos();
		if(!BlockUtils.canBeClicked(pos))
			return;
		
		if(prevSelectedSlot == -1)
			prevSelectedSlot = MC.player.getInventory().selectedSlot;
		
		equipBestTool(pos, useSwords.isChecked(), useHands.isChecked(),
			repairMode.isChecked());
	}
	
	@Override
	public void onUpdate()
	{
		if(prevSelectedSlot == -1 || MC.interactionManager.isBreakingBlock())
			return;
		
		if(switchBack.isChecked())
			MC.player.getInventory().selectedSlot = prevSelectedSlot;
		
		prevSelectedSlot = -1;
	}
	
	public void equipIfEnabled(BlockPos pos)
	{
		if(!isEnabled())
			return;
		
		equipBestTool(pos, useSwords.isChecked(), useHands.isChecked(),
			repairMode.isChecked());
	}
	
	public void equipBestTool(BlockPos pos, boolean useSwords, boolean useHands,
		boolean repairMode)
	{
		ClientPlayerEntity player = MC.player;
		if(player.getAbilities().creativeMode)
			return;
		
		int bestSlot = getBestSlot(pos, useSwords, repairMode);
		if(bestSlot == -1)
		{
			ItemStack heldItem = player.getMainHandStack();
			if(!isDamageable(heldItem))
				return;
			
			if(repairMode && isTooDamaged(heldItem))
			{
				selectFallbackSlot();
				return;
			}
			
			if(useHands && isWrongTool(heldItem, pos))
			{
				selectFallbackSlot();
				return;
			}
			
			return;
		}
		
		player.getInventory().selectedSlot = bestSlot;
	}
	
	private int getBestSlot(BlockPos pos, boolean useSwords, boolean repairMode)
	{
		ClientPlayerEntity player = MC.player;
		PlayerInventory inventory = player.getInventory();
		ItemStack heldItem = MC.player.getMainHandStack();
		
		BlockState state = BlockUtils.getState(pos);
		float bestSpeed = getMiningSpeed(heldItem, state);
		int bestSlot = -1;
		
		for(int slot = 0; slot < 9; slot++)
		{
			if(slot == inventory.selectedSlot)
				continue;
			
			ItemStack stack = inventory.getStack(slot);
			
			float speed = getMiningSpeed(stack, state);
			if(speed <= bestSpeed)
				continue;
			
			if(!useSwords && stack.getItem() instanceof SwordItem)
				continue;
			
			if(repairMode && isTooDamaged(stack))
				continue;
			
			bestSpeed = speed;
			bestSlot = slot;
		}
		
		return bestSlot;
	}
	
	private float getMiningSpeed(ItemStack stack, BlockState state)
	{
		float speed = stack.getMiningSpeedMultiplier(state);
		
		if(speed > 1)
		{
			int efficiency =
				EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, stack);
			if(efficiency > 0 && !stack.isEmpty())
				speed += efficiency * efficiency + 1;
		}
		
		return speed;
	}
	
	private boolean isDamageable(ItemStack stack)
	{
		return !stack.isEmpty() && stack.getItem().isDamageable();
	}
	
	private boolean isTooDamaged(ItemStack stack)
	{
		return stack.getMaxDamage() - stack.getDamage() <= 4;
	}
	
	private boolean isWrongTool(ItemStack heldItem, BlockPos pos)
	{
		BlockState state = BlockUtils.getState(pos);
		return getMiningSpeed(heldItem, state) <= 1;
	}
	
	private void selectFallbackSlot()
	{
		int fallbackSlot = getFallbackSlot();
		PlayerInventory inventory = MC.player.getInventory();
		
		if(fallbackSlot == -1)
		{
			if(inventory.selectedSlot == 8)
				inventory.selectedSlot = 0;
			else
				inventory.selectedSlot++;
			
			return;
		}
		
		inventory.selectedSlot = fallbackSlot;
	}
	
	private int getFallbackSlot()
	{
		PlayerInventory inventory = MC.player.getInventory();
		
		for(int slot = 0; slot < 9; slot++)
		{
			if(slot == inventory.selectedSlot)
				continue;
			
			ItemStack stack = inventory.getStack(slot);
			
			if(!isDamageable(stack))
				return slot;
		}
		
		return -1;
	}
}
