/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.Component;
import net.minecraft.item.ItemStack;
import net.wurstclient.SearchTags;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.util.ChatUtils;

@SearchTags({"view components", "ComponentViewer", "component viewer"})
public final class ViewCompCmd extends Command
{
	public ViewCompCmd()
	{
		super("viewcomp", "Shows you the component data of an item.",
			".viewcomp", "Copy to clipboard: .viewcomp copy");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		ClientPlayerEntity player = MC.player;
		ItemStack stack = player.getInventory().getSelectedStack();
		if(stack.isEmpty())
			throw new CmdError("You must hold an item in your main hand.");
		
		String compString =
			stack.getComponents().stream().map(Component::toString).reduce("",
				(a, b) -> a.replace("minecraft:", "") + "\n"
					+ b.replace("minecraft:", ""));
		
		switch(String.join(" ", args).toLowerCase())
		{
			case "":
			ChatUtils.message("Components: " + compString);
			break;
			
			case "copy":
			MC.keyboard.setClipboard(compString);
			ChatUtils.message("Component data copied to clipboard.");
			break;
			
			default:
			throw new CmdSyntaxError();
		}
	}
}
