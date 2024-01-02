/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

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
import net.wurstclient.settings.BlockSetting;
import net.wurstclient.settings.Setting;
import net.wurstclient.util.CmdUtils;
import net.wurstclient.util.MathUtils;

@DontBlock
public final class SetBlockCmd extends Command
{
	public SetBlockCmd()
	{
		super("setblock",
			"Changes a block setting of a feature. Allows you\n"
				+ "to change these settings through keybinds.",
			".setblock <feature> <setting> <block>",
			".setblock <feature> <setting> reset",
			"Example: .setblock Nuker ID dirt");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length != 3)
			throw new CmdSyntaxError();
		
		Feature feature = CmdUtils.findFeature(args[0]);
		Setting setting = CmdUtils.findSetting(feature, args[1]);
		BlockSetting blockSetting = getAsBlockSetting(feature, setting);
		setBlock(blockSetting, args[2]);
	}
	
	private BlockSetting getAsBlockSetting(Feature feature, Setting setting)
		throws CmdError
	{
		if(!(setting instanceof BlockSetting))
			throw new CmdError(feature.getName() + " " + setting.getName()
				+ " is not a block setting.");
		
		return (BlockSetting)setting;
	}
	
	private void setBlock(BlockSetting setting, String value)
		throws CmdSyntaxError
	{
		if(value.toLowerCase().equals("reset"))
		{
			setting.resetToDefault();
			return;
		}
		
		Block block = getBlockFromNameOrID(value);
		if(block == null)
			throw new CmdSyntaxError("\"" + value + "\" is not a valid block.");
		
		setting.setBlock(block);
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
