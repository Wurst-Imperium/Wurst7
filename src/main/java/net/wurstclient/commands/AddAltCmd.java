/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.util.ChatUtil;
import net.wurstclient.altmanager.Alt;
import net.wurstclient.altmanager.AltManager;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.util.ChatUtils;

public final class AddAltCmd extends Command
{
	public AddAltCmd()
	{
		super("addalt", "添加一个指定的玩家到你的alt列表\n(alt列表就是主页的AltManager,\n即账号管理器,用于选择并更换ID)", ".addalt <玩家>", "添加服务器中的所有玩家到Alt: .addalt all");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length != 1)
			throw new CmdSyntaxError();
		
		String name = args[0];
		
		switch(name)
		{
			case "all":
			addAll();
			break;
			
			default:
			add(name);
			break;
		}
	}
	
	private void add(String name)
	{
		if(name.equalsIgnoreCase("Alexander01998"))
			return;
		
		WURST.getAltManager().add(new Alt(name, null, null));
		ChatUtils.message("Added 1 alt.");
	}
	
	private void addAll()
	{
		int alts = 0;
		AltManager altManager = WURST.getAltManager();
		String playerName = MC.getSession().getProfile().getName();
		
		for(PlayerListEntry entry : MC.player.networkHandler.getPlayerList())
		{
			String name = entry.getProfile().getName();
			name = ChatUtil.stripTextFormat(name);
			
			if(altManager.contains(name))
				continue;
			
			if(name.equalsIgnoreCase(playerName)
				|| name.equalsIgnoreCase("Alexander01998"))
				continue;
			
			altManager.add(new Alt(name, null, null));
			alts++;
		}
		
		ChatUtils.message("Added " + alts + (alts == 1 ? " alt." : " alts."));
	}
}
