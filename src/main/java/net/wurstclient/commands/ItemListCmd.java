/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import java.util.Collections;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.wurstclient.DontBlock;
import net.wurstclient.Feature;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.settings.ItemListSetting;
import net.wurstclient.settings.Setting;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.CmdUtils;
import net.wurstclient.util.ItemUtils;
import net.wurstclient.util.MathUtils;

@DontBlock
public final class ItemListCmd extends Command
{
	public ItemListCmd()
	{
		super("itemlist",
			"Changes a ItemList setting of a feature. Allows you\n"
				+ "to change these settings through keybinds.",
			".itemlist <feature> <setting> add <item>",
			".itemlist <feature> <setting> remove <item>",
			".itemlist <feature> <setting> list [<page>]",
			".itemlist <feature> <setting> reset",
			"Example: .itemlist AutoDrop Items add dirt");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length < 3 || args.length > 4)
			throw new CmdSyntaxError();
		
		Feature feature = CmdUtils.findFeature(args[0]);
		Setting abstractSetting = CmdUtils.findSetting(feature, args[1]);
		ItemListSetting setting =
			getAsItemListSetting(feature, abstractSetting);
		
		switch(args[2].toLowerCase())
		{
			case "add":
			add(feature, setting, args);
			break;
			
			case "remove":
			remove(feature, setting, args);
			break;
			
			case "list":
			list(feature, setting, args);
			break;
			
			case "reset":
			setting.resetToDefaults();
			break;
			
			default:
			throw new CmdSyntaxError();
		}
	}
	
	private void add(Feature feature, ItemListSetting setting, String[] args)
		throws CmdException
	{
		if(args.length != 4)
			throw new CmdSyntaxError();
		
		String inputItemName = args[3];
		Item item = ItemUtils.getItemFromNameOrID(inputItemName);
		if(item == null)
			throw new CmdSyntaxError(
				"\"" + inputItemName + "\" is not a valid item.");
		
		String itemName = Registries.ITEM.getId(item).toString();
		int index = Collections.binarySearch(setting.getItemNames(), itemName);
		if(index >= 0)
			throw new CmdError(feature.getName() + " " + setting.getName()
				+ " already contains " + itemName);
		
		setting.add(item);
	}
	
	private void remove(Feature feature, ItemListSetting setting, String[] args)
		throws CmdException
	{
		if(args.length != 4)
			throw new CmdSyntaxError();
		
		String inputItemName = args[3];
		Item item = ItemUtils.getItemFromNameOrID(inputItemName);
		if(item == null)
			throw new CmdSyntaxError(
				"\"" + inputItemName + "\" is not a valid item.");
		
		String itemName = Registries.ITEM.getId(item).toString();
		int index = Collections.binarySearch(setting.getItemNames(), itemName);
		if(index < 0)
			throw new CmdError(feature.getName() + " " + setting.getName()
				+ " does not contain " + itemName);
		
		setting.remove(index);
	}
	
	private void list(Feature feature, ItemListSetting setting, String[] args)
		throws CmdException
	{
		if(args.length > 4)
			throw new CmdSyntaxError();
		
		List<String> items = setting.getItemNames();
		int page = parsePage(args);
		int pages = (int)Math.ceil(items.size() / 8.0);
		pages = Math.max(pages, 1);
		
		if(page > pages || page < 1)
			throw new CmdSyntaxError("Invalid page: " + page);
		
		String total = "Total: " + items.size() + " item";
		total += items.size() != 1 ? "s" : "";
		ChatUtils.message(total);
		
		int start = (page - 1) * 8;
		int end = Math.min(page * 8, items.size());
		
		ChatUtils.message(feature.getName() + " " + setting.getName()
			+ " (page " + page + "/" + pages + ")");
		for(int i = start; i < end; i++)
			ChatUtils.message(items.get(i).toString());
	}
	
	private int parsePage(String[] args) throws CmdSyntaxError
	{
		if(args.length < 4)
			return 1;
		
		if(!MathUtils.isInteger(args[3]))
			throw new CmdSyntaxError("Not a number: " + args[3]);
		
		return Integer.parseInt(args[3]);
	}
	
	private ItemListSetting getAsItemListSetting(Feature feature,
		Setting setting) throws CmdError
	{
		if(!(setting instanceof ItemListSetting))
			throw new CmdError(feature.getName() + " " + setting.getName()
				+ " is not a ItemList setting.");
		
		return (ItemListSetting)setting;
	}
}
