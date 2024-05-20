/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks.autofish;

import java.util.stream.IntStream;
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
	
	private final CheckboxSetting stopWhenInvFull = new CheckboxSetting(
		"Stop when inv full",
		"If enabled, AutoFish will turn itself off when your inventory is full.",
		false);
	
	private final AutoFishHack autoFish;
	private int bestRodSlot;
	
	public AutoFishRodSelector(AutoFishHack autoFish)
	{
		this.autoFish = autoFish;
	}
	
	public Stream<Setting> getSettings()
	{
		return Stream.of(stopWhenOutOfRods, stopWhenInvFull);
	}
	
	public void reset()
	{
		bestRodSlot = -1;
	}
	
	public boolean isOutOfRods()
	{
		return bestRodSlot == -1;
	}
	
	/**
	 * Reevaluates the player's fishing rods, checks for any inventory-related
	 * issues and updates the selected rod if necessary.
	 *
	 * @return true if it's OK to proceed with fishing in the same tick
	 */
	public boolean update()
	{
		PlayerInventory inventory = MC.player.getInventory();
		int selectedSlot = inventory.selectedSlot;
		ItemStack selectedStack = inventory.getStack(selectedSlot);
		
		// evaluate selected rod (or lack thereof)
		int bestRodValue = getRodValue(selectedStack);
		bestRodSlot = bestRodValue > -1 ? selectedSlot : -1;
		
		// create a stream of all slots that we want to search
		IntStream stream = IntStream.range(0, 36);
		stream = IntStream.concat(stream, IntStream.of(40));
		
		// search inventory for better rod
		for(int slot : stream.toArray())
		{
			ItemStack stack = inventory.getStack(slot);
			int rodValue = getRodValue(stack);
			
			if(rodValue > bestRodValue)
			{
				bestRodValue = rodValue;
				bestRodSlot = slot;
			}
		}
		
		// wait for AutoEat to finish eating
		if(WurstClient.INSTANCE.getHax().autoEatHack.isEating())
			return false;
		
		// stop if out of rods
		if(stopWhenOutOfRods.isChecked() && bestRodSlot == -1)
		{
			ChatUtils.message("AutoFish has run out of fishing rods.");
			autoFish.setEnabled(false);
			return false;
		}
		
		// stop if inventory is full
		if(stopWhenInvFull.isChecked() && inventory.getEmptySlot() == -1)
		{
			ChatUtils.message(
				"AutoFish has stopped because your inventory is full.");
			autoFish.setEnabled(false);
			return false;
		}
		
		// check if selected rod is still the best one
		if(MC.player.getInventory().selectedSlot == bestRodSlot)
			return true;
		
		// change selected rod and wait until the next tick
		InventoryUtils.selectItem(bestRodSlot);
		return false;
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
