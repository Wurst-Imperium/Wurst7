/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.CmdUtils;

public final class CopyItemCmd extends Command
{
	public CopyItemCmd()
	{
		super("copyitem",
			"Allows you to copy items that other people are holding\n"
				+ "or wearing. Requires creative mode.",
			".copyitem <player> <slot>",
			"Valid slots: hand, head, chest, legs, feet");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length != 2)
			throw new CmdSyntaxError();
		
		if(!MC.player.getAbilities().creativeMode)
			throw new CmdError("Creative mode only.");
		
		AbstractClientPlayerEntity player = getPlayer(args[0]);
		ItemStack item = getItem(player, args[1]);
		CmdUtils.giveItem(item);
		
		ChatUtils.message("Item copied.");
	}
	
	private AbstractClientPlayerEntity getPlayer(String name) throws CmdError
	{
		for(AbstractClientPlayerEntity player : MC.world.getPlayers())
		{
			if(!player.getName().getString().equalsIgnoreCase(name))
				continue;
			
			return player;
		}
		
		throw new CmdError("Player \"" + name + "\" could not be found.");
	}
	
	private ItemStack getItem(AbstractClientPlayerEntity player, String slot)
		throws CmdSyntaxError
	{
		switch(slot.toLowerCase())
		{
			case "hand":
			return player.getMainHandStack();
			
			case "head":
			return player.getEquippedStack(EquipmentSlot.HEAD);
			
			case "chest":
			return player.getEquippedStack(EquipmentSlot.CHEST);
			
			case "legs":
			return player.getEquippedStack(EquipmentSlot.LEGS);
			
			case "feet":
			return player.getEquippedStack(EquipmentSlot.FEET);
			
			default:
			throw new CmdSyntaxError();
		}
	}
}
