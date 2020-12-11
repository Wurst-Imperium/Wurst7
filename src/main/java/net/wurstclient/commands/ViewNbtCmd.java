/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
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
			"Copy to clipboard: .viewnbt copy");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		ClientPlayerEntity player = MC.player;
		ItemStack stack = player.inventory.getMainHandStack();
		if(stack.isEmpty())
			throw new CmdError("You must hold an item in your main hand.");
		
		CompoundTag tag = stack.getTag();
		String nbt = tag == null ? "" : tag.asString();
		
		switch(String.join(" ", args).toLowerCase())
		{
			case "":
			ChatUtils.message("NBT: " + nbt);
			break;
			
			case "copy":
			MC.keyboard.setClipboard(nbt);
			ChatUtils.message("NBT data copied to clipboard.");
			break;
			
			default:
			throw new CmdSyntaxError();
		}
	}
}
