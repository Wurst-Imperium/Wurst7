/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.Component;
import net.minecraft.item.ItemStack;
import net.wurstclient.SearchTags;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.json.JsonUtils;

@SearchTags({"view components", "ComponentViewer", "component viewer"})
public final class ViewCompCmd extends Command
{
	public ViewCompCmd()
	{
		super("viewcomp", "Shows you the component data of an item.",
			".viewcomp", ".viewcomp type <query>",
			"Copy to clipboard: .viewcomp copy",
			"Example: .viewcomp type name");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		ClientPlayerEntity player = MC.player;
		ItemStack stack = player.getInventory().getSelectedStack();
		if(stack.isEmpty())
			throw new CmdError("You must hold an item in your main hand.");
		
		String query = null;
		boolean copy = false;
		
		if(args.length >= 1)
			switch(args[0].toLowerCase())
			{
				case "copy":
				if(args.length != 1)
					throw new CmdSyntaxError();
				copy = true;
				break;
				
				case "type":
				if(args.length != 2)
					throw new CmdSyntaxError();
				query = args[1];
				break;
				
				default:
				throw new CmdSyntaxError();
			}
		
		String compString = getComponentString(stack, query);
		if(copy)
		{
			MC.keyboard.setClipboard(compString);
			ChatUtils.message("Component data copied to clipboard.");
		}else
			ChatUtils.message("Components: " + compString);
	}
	
	private String getComponentString(ItemStack stack, String query)
	{
		String compString = "";
		for(Component<?> c : getMatchingComponents(stack, query))
		{
			compString +=
				"\n" + c.type().toString().replace("minecraft:", "") + " => ";
			DataResult<JsonElement> result = c.encode(
				MC.player.getRegistryManager().getOps(JsonOps.INSTANCE));
			JsonElement json =
				result.resultOrPartial().orElse(JsonNull.INSTANCE);
			compString += JsonUtils.GSON.toJson(json).replace("$", "$$")
				.replace("\u00a7", "$").replace("minecraft:", "");
		}
		return compString;
	}
	
	private List<Component<?>> getMatchingComponents(ItemStack stack,
		String query)
	{
		if(query == null)
			return stack.getComponents().stream().toList();
		
		String queryLower = query.toLowerCase();
		return stack.getComponents().stream()
			.filter(c -> c.type().toString().toLowerCase().contains(queryLower))
			.toList();
	}
}
