/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.util.registry.Registry;
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
		if(!MC.player.abilities.creativeMode)
			throw new CmdError("Creative mode only.");
		
		if(args.length > 1)
			throw new CmdSyntaxError();
		
		ItemStack stack = getHeldItem();
		enchant(stack);
		
		ChatUtils.message("Item enchanted.");
	}
	
	private ItemStack getHeldItem() throws CmdError
	{
		ItemStack stack = MC.player.inventory.getMainHandStack();
		
		if(stack.isEmpty())
			throw new CmdError("There is no item in your hand.");
		
		return stack;
	}
	
	private void enchant(ItemStack stack)
	{
		for(Enchantment enchantment : Registry.ENCHANTMENT)
		{
			if(enchantment == Enchantments.SILK_TOUCH)
				continue;
			
			if(enchantment.isCursed())
				continue;
			
			if(enchantment == Enchantments.QUICK_CHARGE)
			{
				stack.addEnchantment(enchantment, 5);
				continue;
			}
			
			stack.addEnchantment(enchantment, 127);
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
