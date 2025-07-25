/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import java.util.Arrays;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.InventoryUtils;

public final class ModifyCmd extends Command
{
	public ModifyCmd()
	{
		super("modify", "Allows you to modify component data of items.",
			".modify set <type> <value>", ".modify remove <type>",
			"Use $ for colors, use $$ for $.", "", "Example:",
			".modify set custom_name {\"text\":\"$cRed Name\"}",
			"(changes the item's name to \u00a7cRed Name\u00a7r)");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		ClientPlayerEntity player = MC.player;
		if(!player.getAbilities().creativeMode)
			throw new CmdError("Creative mode only.");
		if(args.length < 2)
			throw new CmdSyntaxError();
		
		PlayerInventory inventory = player.getInventory();
		int slot = inventory.getSelectedSlot();
		ItemStack stack = inventory.getSelectedStack();
		if(stack == null)
			throw new CmdError("You must hold an item in your main hand.");
		
		switch(args[0].toLowerCase())
		{
			case "set":
			set(stack, args);
			break;
			
			case "remove":
			remove(stack, args);
			break;
			
			default:
			throw new CmdSyntaxError();
		}
		
		InventoryUtils.setCreativeStack(slot, stack);
		ChatUtils.message("Item modified.");
	}
	
	private void set(ItemStack stack, String[] args) throws CmdException
	{
		if(args.length < 3)
			throw new CmdSyntaxError();
		
		ComponentType<?> type = parseComponentType(args[1]);
		
		String valueString =
			String.join(" ", Arrays.copyOfRange(args, 2, args.length))
				.replace("$", "\u00a7").replace("\u00a7\u00a7", "$");
		JsonElement valueJson = parseJson(valueString);
		DataResult<?> valueResult = type.getCodec().parse(
			MC.player.getRegistryManager().getOps(JsonOps.INSTANCE), valueJson);
		Object value = valueResult.resultOrPartial().orElse(null);
		
		ComponentMap.Builder builder = ComponentMap.builder();
		builder.put(type, value);
		stack.applyComponentsFrom(builder.build());
	}
	
	private void remove(ItemStack stack, String[] args) throws CmdException
	{
		if(args.length > 2)
			throw new CmdSyntaxError();
		
		stack.set(parseComponentType(args[1]), null);
	}
	
	private ComponentType<?> parseComponentType(String typeName) throws CmdError
	{
		ComponentType<?> type =
			Registries.DATA_COMPONENT_TYPE.get(Identifier.tryParse(typeName));
		
		if(type == null)
			throw new CmdError(
				"Component type \"" + typeName + "\" does not exist.");
		
		return type;
	}
	
	private JsonElement parseJson(String jsonString) throws CmdError
	{
		try
		{
			return JsonParser.parseString(jsonString);
			
		}catch(JsonParseException e)
		{
			if(e.getCause() != null)
				throw new CmdError(e.getCause().getMessage());
			throw new CmdError(e.getMessage());
		}
	}
}
