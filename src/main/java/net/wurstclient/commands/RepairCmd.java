/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.InventoryUtils;

public final class RepairCmd extends Command
{
	public RepairCmd()
	{
		super("repair", "Repairs the held item. Requires creative mode.",
			".repair");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length > 0)
			throw new CmdSyntaxError();
		
		ClientPlayerEntity player = MC.player;
		
		if(!player.getAbilities().creativeMode)
			throw new CmdError("Creative mode only.");
		
		int slot = player.getInventory().getSelectedSlot();
		ItemStack stack = getHeldStack(player);
		stack.setDamage(0);
		InventoryUtils.setCreativeStack(slot, stack);
		
		ChatUtils.message("Item repaired.");
	}
	
	private ItemStack getHeldStack(ClientPlayerEntity player) throws CmdError
	{
		ItemStack stack = player.getInventory().getSelectedStack();
		
		if(stack.isEmpty())
			throw new CmdError("You need an item in your hand.");
		
		if(!stack.isDamageable())
			throw new CmdError("This item can't take damage.");
		
		if(!stack.isDamaged())
			throw new CmdError("This item is not damaged.");
		
		return stack;
	}
	
	@Override
	public String getPrimaryAction()
	{
		return "Repair Current Item";
	}
	
	@Override
	public void doPrimaryAction()
	{
		WURST.getCmdProcessor().process("repair");
	}
}
