/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import java.util.Arrays;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.CmdUtils;
import net.wurstclient.util.MathUtils;

public final class GiveCmd extends Command
{
	public GiveCmd()
	{
		super("give",
			"Gives you an item with custom NBT data.\n"
				+ "Requires creative mode.",
			".give <item> [<amount>] [<nbt>]", ".give <id> [<amount>] [<nbt>]");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		// validate input
		if(args.length < 1)
			throw new CmdSyntaxError();
		
		if(!MC.player.getAbilities().creativeMode)
			throw new CmdError("Creative mode only.");
		
		// id/name
		Item item = CmdUtils.parseItem(args[0]);
		
		// amount
		int amount = 1;
		if(args.length >= 2)
		{
			if(!MathUtils.isInteger(args[1]))
				throw new CmdSyntaxError("Not a number: " + args[1]);
			
			amount = Integer.parseInt(args[1]);
			
			if(amount < 1)
				throw new CmdError("Amount cannot be less than 1.");
			
			if(amount > 64)
				throw new CmdError("Amount cannot be more than 64.");
		}
		
		// nbt data
		String nbt = null;
		if(args.length >= 3)
			nbt = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
		
		// generate item
		ItemStack stack = new ItemStack(item, amount);
		if(nbt != null)
			try
			{
				NbtCompound tag = StringNbtReader.readCompound(nbt);
				NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, tag);
				
			}catch(CommandSyntaxException e)
			{
				ChatUtils.message(e.getMessage());
				throw new CmdSyntaxError("NBT data is invalid.");
			}
		
		// give item
		CmdUtils.giveItem(stack);
		ChatUtils.message("Item" + (amount > 1 ? "s" : "") + " created.");
	}
}
