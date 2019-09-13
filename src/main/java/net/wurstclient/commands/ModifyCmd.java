/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.server.network.packet.CreativeInventoryActionC2SPacket;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.util.ChatUtils;

public final class ModifyCmd extends Command
{
	public ModifyCmd()
	{
		super("modify", "Allows you to modify NBT data of items.", "add <nbt>",
			"set <nbt>", "remove <nbt_path>");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		ClientPlayerEntity player = MC.player;
		
		if(!player.abilities.creativeMode)
			throw new CmdError("Creative mode only.");
		
		if(args.length < 2)
			throw new CmdSyntaxError();
		
		ItemStack item = player.inventory.getMainHandStack();
		
		if(item == null)
			throw new CmdError("You must hold an item in your main hand.");
		
		switch(args[0].toLowerCase())
		{
			case "add":
			add(item, args);
			break;
			
			case "set":
			set(item, args);
			break;
			
			case "remove":
			remove(item, args);
			break;
			
			default:
			throw new CmdSyntaxError();
		}
		
		MC.player.networkHandler
			.sendPacket(new CreativeInventoryActionC2SPacket(
				36 + player.inventory.selectedSlot, item));
		
		ChatUtils.message("Item modified.");
	}
	
	private void add(ItemStack item, String[] args) throws CmdError
	{
		String v = "";
		for(int i = 1; i < args.length; i++)
			v += args[i] + " ";
		
		if(!item.hasTag())
			item.setTag(new CompoundTag());
		
		try
		{
			CompoundTag value = StringNbtReader.parse(v);
			item.getTag().copyFrom(value);
			
		}catch(CommandSyntaxException e)
		{
			ChatUtils.message(e.getMessage());
			throw new CmdError("NBT data is invalid.");
		}
	}
	
	private void set(ItemStack item, String[] args) throws CmdError
	{
		String v = "";
		for(int i = 1; i < args.length; i++)
			v += args[i] + " ";
		
		try
		{
			CompoundTag value = StringNbtReader.parse(v);
			item.setTag(value);
			
		}catch(CommandSyntaxException e)
		{
			ChatUtils.message(e.getMessage());
			throw new CmdError("NBT data is invalid.");
		}
	}
	
	private void remove(ItemStack item, String[] args) throws CmdException
	{
		if(args.length > 2)
			throw new CmdSyntaxError();
		
		NbtPath path = parseNbtPath(item.getTag(), args[1]);
		
		if(path == null)
			throw new CmdError("The path does not exist.");
		
		path.base.remove(path.key);
	}
	
	private NbtPath parseNbtPath(CompoundTag tag, String path)
	{
		String[] parts = path.split("\\.");
		
		CompoundTag base = tag;
		if(base == null)
			return null;
		
		for(int i = 0; i < parts.length - 1; i++)
		{
			String part = parts[i];
			
			if(!base.containsKey(part)
				|| !(base.getTag(part) instanceof CompoundTag))
				return null;
			
			base = base.getCompound(part);
		}
		
		if(!base.containsKey(parts[parts.length - 1]))
			return null;
		
		return new NbtPath(base, parts[parts.length - 1]);
	}
	
	private static class NbtPath
	{
		public CompoundTag base;
		public String key;
		
		public NbtPath(CompoundTag base, String key)
		{
			this.base = base;
			this.key = key;
		}
	}
}
