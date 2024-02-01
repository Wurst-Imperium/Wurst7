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

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.wurstclient.DontBlock;
import net.wurstclient.Feature;
import net.wurstclient.command.CmdError;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.settings.BlockListSetting;
import net.wurstclient.settings.Setting;
import net.wurstclient.util.BlockUtils;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.CmdUtils;
import net.wurstclient.util.MathUtils;

@DontBlock
public final class BlockListCmd extends Command
{
	public BlockListCmd()
	{
		super("blocklist",
			"Changes a BlockList setting of a feature. Allows you\n"
				+ "to change these settings through keybinds.",
			".blocklist <feature> <setting> add <block>",
			".blocklist <feature> <setting> remove <block>",
			".blocklist <feature> <setting> list [<page>]",
			".blocklist <feature> <setting> reset",
			"Example: .blocklist Nuker MultiID_List add gravel");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length < 3 || args.length > 4)
			throw new CmdSyntaxError();
		
		Feature feature = CmdUtils.findFeature(args[0]);
		Setting abstractSetting = CmdUtils.findSetting(feature, args[1]);
		BlockListSetting setting =
			getAsBlockListSetting(feature, abstractSetting);
		
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
	
	private void add(Feature feature, BlockListSetting setting, String[] args)
		throws CmdException
	{
		if(args.length != 4)
			throw new CmdSyntaxError();
		
		String inputBlockName = args[3];
		Block block = getBlockFromNameOrID(inputBlockName);
		if(block == null)
			throw new CmdSyntaxError(
				"\"" + inputBlockName + "\" is not a valid block.");
		
		String blockName = BlockUtils.getName(block);
		int index =
			Collections.binarySearch(setting.getBlockNames(), blockName);
		if(index >= 0)
			throw new CmdError(feature.getName() + " " + setting.getName()
				+ " already contains " + blockName);
		
		setting.add(block);
	}
	
	private void remove(Feature feature, BlockListSetting setting,
		String[] args) throws CmdException
	{
		if(args.length != 4)
			throw new CmdSyntaxError();
		
		String inputBlockName = args[3];
		Block block = getBlockFromNameOrID(inputBlockName);
		if(block == null)
			throw new CmdSyntaxError(
				"\"" + inputBlockName + "\" is not a valid block.");
		
		String blockName = BlockUtils.getName(block);
		int index =
			Collections.binarySearch(setting.getBlockNames(), blockName);
		if(index < 0)
			throw new CmdError(feature.getName() + " " + setting.getName()
				+ " does not contain " + blockName);
		
		setting.remove(index);
	}
	
	private void list(Feature feature, BlockListSetting setting, String[] args)
		throws CmdException
	{
		if(args.length > 4)
			throw new CmdSyntaxError();
		
		List<String> blocks = setting.getBlockNames();
		int page = parsePage(args);
		int pages = (int)Math.ceil(blocks.size() / 8.0);
		pages = Math.max(pages, 1);
		
		if(page > pages || page < 1)
			throw new CmdSyntaxError("Invalid page: " + page);
		
		String total = "Total: " + blocks.size() + " block";
		total += blocks.size() != 1 ? "s" : "";
		ChatUtils.message(total);
		
		int start = (page - 1) * 8;
		int end = Math.min(page * 8, blocks.size());
		
		ChatUtils.message(feature.getName() + " " + setting.getName()
			+ " (page " + page + "/" + pages + ")");
		for(int i = start; i < end; i++)
			ChatUtils.message(blocks.get(i).toString());
	}
	
	private int parsePage(String[] args) throws CmdSyntaxError
	{
		if(args.length < 4)
			return 1;
		
		if(!MathUtils.isInteger(args[3]))
			throw new CmdSyntaxError("Not a number: " + args[3]);
		
		return Integer.parseInt(args[3]);
	}
	
	private BlockListSetting getAsBlockListSetting(Feature feature,
		Setting setting) throws CmdError
	{
		if(!(setting instanceof BlockListSetting))
			throw new CmdError(feature.getName() + " " + setting.getName()
				+ " is not a BlockList setting.");
		
		return (BlockListSetting)setting;
	}
	
	private Block getBlockFromNameOrID(String nameOrId)
	{
		if(MathUtils.isInteger(nameOrId))
		{
			BlockState state = Block.STATE_IDS.get(Integer.parseInt(nameOrId));
			if(state == null)
				return null;
			
			return state.getBlock();
		}
		
		try
		{
			return Registries.BLOCK.getOrEmpty(new Identifier(nameOrId))
				.orElse(null);
			
		}catch(InvalidIdentifierException e)
		{
			return null;
		}
	}
}
