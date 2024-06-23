/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import java.util.Iterator;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.Component;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.wurstclient.SearchTags;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.util.ChatUtils;

@SearchTags({"view nbt", "NBTViewer", "nbt viewer"})
public final class ViewNbtCmd extends Command
{
	public ViewNbtCmd()
	{
		super("viewnbt", "Shows you the NBT data of an item.", ".viewnbt",
			"Copy to clipboard: .viewnbt copy",
			"See all NBT fields: .viewnbt all",
			"See specific NBT field: .viewnbt field <FIELD>");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		ClientPlayerEntity player = MC.player;
		ItemStack stack = player.getInventory().getMainHandStack();
		if(stack.isEmpty())
			throw new CmdError("You must hold an item in your main hand.");
		
		NbtCompound tag = stack
			.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT)
			.copyNbt();
		String nbtString = tag.asString();
		
		String argString = String.join(" ", args).toLowerCase();
		
		if(argString.equals(""))
		{
			ChatUtils.message("NBT: " + nbtString);
		}else if(argString.equals("copy"))
		{
			MC.keyboard.setClipboard(nbtString);
			ChatUtils.message("NBT data copied to clipboard.");
		}else if(argString.equals("all"))
		{ // print all component keys
			ComponentMap cm = stack.getComponents();
			Iterator<Component<?>> it = cm.iterator();
			String chatstr = "";
			while(it.hasNext())
			{
				Component<?> component = it.next();
				// Get all component types excluding the "minecraft:"
				chatstr += component.type().toString().substring(10) + '\n';
			}
			ChatUtils.message(chatstr);
		}else if(args[0].equals("field"))
		{
			ComponentMap cm = stack.getComponents();
			Iterator<Component<?>> it = cm.iterator();
			boolean found = false;
			while(it.hasNext())
			{
				Component<?> component = it.next();
				// Get all component types excluding the "minecraft:"
				String componentType =
					component.type().toString().substring(10);
				if(componentType.equals(args[1]))
				{
					found = true;
					ChatUtils.message(
						componentType + "=" + component.value().toString());
					break;
				}
			}
			if(!found)
			{
				ChatUtils.message("NBT " + args[1] + " not found");
			}
		}else
		{
			throw new CmdSyntaxError();
		}
	}
}
