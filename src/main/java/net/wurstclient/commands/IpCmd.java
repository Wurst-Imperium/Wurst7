/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import net.minecraft.client.network.ServerInfo;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;
import net.wurstclient.util.ChatUtils;
import net.wurstclient.util.LastServerRememberer;

public final class IpCmd extends Command
{
	public IpCmd()
	{
		super("ip",
			"显示当前连接的服务器的IP,\n或将其IP复制到剪贴板",
			".ip", "复制到剪贴板: .ip c");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		String ip = getIP();
		
		switch(String.join(" ", args).toLowerCase())
		{
			case "":
			ChatUtils.message("IP: " + ip);
			break;
			
			case "c":
			MC.keyboard.setClipboard(ip);
			ChatUtils.message("IP 已复制到剪贴板.");
			break;
			
			default:
			throw new CmdSyntaxError();
		}
	}
	
	private String getIP()
	{
		ServerInfo lastServer = LastServerRememberer.getLastServer();
		if(lastServer == null || MC.isIntegratedServerRunning())
			return "127.0.0.1:25565";
		
		String ip = lastServer.address;
		if(!ip.contains(":"))
			ip += ":25565";
		
		return ip;
	}
	
	@Override
	public String getPrimaryAction()
	{
		return "Get IP";
	}
	
	@Override
	public void doPrimaryAction()
	{
		WURST.getCmdProcessor().process("ip");
	}
}
