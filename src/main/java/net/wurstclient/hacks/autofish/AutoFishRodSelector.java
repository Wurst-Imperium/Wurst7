/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.autofish;

import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.wurstclient.WurstClient;
import net.wurstclient.util.InventoryUtils;

public final class AutoFishRodSelector
{
	private static final MinecraftClient MC = WurstClient.MC;
	
	private int bestRodValue;
	private int bestRodSlot;
	
	public void reset()
	{
		bestRodValue = -1;
		bestRodSlot = -1;
	}
	
	public void updateBestRod()
	{
		PlayerInventory inventory = MC.player.getInventory();
		int selectedSlot = inventory.selectedSlot;
		ItemStack selectedStack = inventory.getStack(selectedSlot);
		
		// start with selected rod
		bestRodValue = getRodValue(selectedStack);
		bestRodSlot = bestRodValue > -1 ? selectedSlot : -1;
		
		// search inventory for better rod
		for(int slot = 0; slot < 36; slot++)
		{
			ItemStack stack = inventory.getStack(slot);
			int rodValue = getRodValue(stack);
			
			if(rodValue > bestRodValue)
			{
				bestRodValue = rodValue;
				bestRodSlot = slot;
			}
		}
	}
	
	public boolean hasARod()
	{
		return bestRodSlot != -1;
	}
	
	public boolean isBestRodAlreadySelected()
	{
		return bestRodSlot == MC.player.getInventory().selectedSlot;
	}
	
	public void selectBestRod()
	{
		InventoryUtils.selectItem(bestRodSlot);
	}
	
	private int getRodValue(ItemStack stack)
	{
		if(stack.isEmpty() || !(stack.getItem() instanceof FishingRodItem))
			return -1;
		
		int luckOTSLvl =
			EnchantmentHelper.getLevel(Enchantments.LUCK_OF_THE_SEA, stack);
		int lureLvl = EnchantmentHelper.getLevel(Enchantments.LURE, stack);
		int unbreakingLvl =
			EnchantmentHelper.getLevel(Enchantments.UNBREAKING, stack);
		int mendingBonus =
			EnchantmentHelper.getLevel(Enchantments.MENDING, stack);
		int noVanishBonus = EnchantmentHelper.hasVanishingCurse(stack) ? 0 : 1;
		
		return luckOTSLvl * 9 + lureLvl * 9 + unbreakingLvl * 2 + mendingBonus
			+ noVanishBonus;
	}
}
