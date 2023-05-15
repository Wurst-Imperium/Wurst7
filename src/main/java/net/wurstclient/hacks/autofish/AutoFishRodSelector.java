/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.autofish;

import java.util.stream.Stream;

import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.wurstclient.WurstClient;
import net.wurstclient.hacks.AutoFishHack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.Setting;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.InventoryUtils;

public final class AutoFishRodSelector
{
	private static final MinecraftClient MC = WurstClient.MC;
	
	private final CheckboxSetting stopWhenOutOfRods = new CheckboxSetting(
		"Stop when out of rods",
		"If enabled, AutoFish will turn itself off when it runs out of fishing rods.",
		false);
	
	private final AutoFishHack autoFish;
	private int bestRodValue;
	private int bestRodSlot;
	
	public AutoFishRodSelector(AutoFishHack autoFish)
	{
		this.autoFish = autoFish;
	}
	
	public Stream<Setting> getSettings()
	{
		return Stream.of(stopWhenOutOfRods);
	}
	
	public void reset()
	{
		bestRodValue = -1;
		bestRodSlot = -1;
	}
	
	public boolean hasARod()
	{
		return bestRodSlot != -1;
	}
	
	public boolean isBestRodAlreadySelected()
	{
		PlayerInventory inventory = MC.player.getInventory();
		int selectedSlot = inventory.selectedSlot;
		ItemStack selectedStack = inventory.getStack(selectedSlot);
		
		// evaluate selected rod (or lack thereof)
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
		
		// return true if selected rod is best rod
		return bestRodSlot == selectedSlot;
	}
	
	public void selectBestRod()
	{
		if(bestRodSlot == -1 && stopWhenOutOfRods.isChecked())
		{
			ChatUtils.message("AutoFish has run out of fishing rods.");
			autoFish.setEnabled(false);
			return;
		}
		
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
