/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import java.util.Arrays;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.util.ChatUtils;

public final class ModifyCmd extends Command
{
	public ModifyCmd()
	{
		super("modify", "Allows you to modify NBT data of items.",
			".modify add <nbt_data>", ".modify set <nbt_data>",
			".modify remove <nbt_path>", "Use $ for colors, use $$ for $.", "",
			"Example:",
			".modify add {display:{Name:'{\"text\":\"$cRed Name\"}'}}",
			"(changes the item's name to \u00a7cRed Name\u00a7r)");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		ClientPlayerEntity player = MC.player;
		
		if(!player.abilities.creativeMode)
			throw new CmdError("Creative mode only.");
		
		if(args.length < 2)
			throw new CmdSyntaxError();
		
		ItemStack stack = player.inventory.getMainHandStack();
		
		if(stack == null)
			throw new CmdError("You must hold an item in your main hand.");
		
		switch(args[0].toLowerCase())
		{
			case "add":
			add(stack, args);
			break;
			
			case "set":
			set(stack, args);
			break;
			
			case "remove":
			remove(stack, args);
			break;
			
			default:
			throw new CmdSyntaxError();
		}
		
		MC.player.networkHandler
			.sendPacket(new CreativeInventoryActionC2SPacket(
				36 + player.inventory.selectedSlot, stack));
		
		ChatUtils.message("Item modified.");
	}
	
	private void add(ItemStack stack, String[] args) throws CmdError
	{
		String nbt = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
		nbt = nbt.replace("$", "\u00a7").replace("\u00a7\u00a7", "$");
		
		if(!stack.hasTag())
			stack.setTag(new CompoundTag());
		
		try
		{
			CompoundTag tag = StringNbtReader.parse(nbt);
			stack.getTag().copyFrom(tag);
			
		}catch(CommandSyntaxException e)
		{
			ChatUtils.message(e.getMessage());
			throw new CmdError("NBT data is invalid.");
		}
	}
	
	private void set(ItemStack stack, String[] args) throws CmdError
	{
		String nbt = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
		nbt = nbt.replace("$", "\u00a7").replace("\u00a7\u00a7", "$");
		
		try
		{
			CompoundTag tag = StringNbtReader.parse(nbt);
			stack.setTag(tag);
			
		}catch(CommandSyntaxException e)
		{
			ChatUtils.message(e.getMessage());
			throw new CmdError("NBT data is invalid.");
		}
	}
	
	private void remove(ItemStack stack, String[] args) throws CmdException
	{
		if(args.length > 2)
			throw new CmdSyntaxError();
		
		NbtPath path = parseNbtPath(stack.getTag(), args[1]);
		
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
			
			if(!base.contains(part) || !(base.get(part) instanceof CompoundTag))
				return null;
			
			base = base.getCompound(part);
		}
		
		if(!base.contains(parts[parts.length - 1]))
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
