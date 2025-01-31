/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EnchantmentTags;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.util.ChatUtils;

public final class EnchantCmd extends Command
{
	public EnchantCmd()
	{
		super("enchant", "Enchants an item with everything,\n"
			+ "except for silk touch and curses.", ".enchant");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		if(!MC.player.getAbilities().creativeMode)
			throw new CmdError("Creative mode only.");
		
		if(args.length > 1)
			throw new CmdSyntaxError();
		
		enchant(getHeldItem(), 127);
		ChatUtils.message("Item enchanted.");
	}
	
	private ItemStack getHeldItem() throws CmdError
	{
		ItemStack stack = MC.player.getMainHandStack();
		
		if(stack.isEmpty())
			stack = MC.player.getOffHandStack();
		
		if(stack.isEmpty())
			throw new CmdError("There is no item in your hand.");
		
		return stack;
	}
	
	private void enchant(ItemStack stack, int level)
	{
		DynamicRegistryManager drm = MC.world.getRegistryManager();
		Registry<Enchantment> registry =
			drm.getOrThrow(RegistryKeys.ENCHANTMENT);
		
		for(RegistryEntry<Enchantment> entry : registry.getIndexedEntries())
		{
			// Skip curses
			if(entry.isIn(EnchantmentTags.CURSE))
				continue;
			
			// Skip Silk Touch so it doesn't remove Fortune
			if(entry.getKey().orElse(null) == Enchantments.SILK_TOUCH)
				continue;
			
			// Limit Quick Charge to level 5 so it doesn't break
			if(entry.getKey().orElse(null) == Enchantments.QUICK_CHARGE)
			{
				stack.addEnchantment(entry, Math.min(level, 5));
				continue;
			}
			
			stack.addEnchantment(entry, level);
		}
	}
	
	@Override
	public String getPrimaryAction()
	{
		return "Enchant Held Item";
	}
	
	@Override
	public void doPrimaryAction()
	{
		WURST.getCmdProcessor().process("enchant");
	}
}
