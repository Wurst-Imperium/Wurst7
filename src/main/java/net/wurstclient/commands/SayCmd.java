/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.commands;

import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.wurstclient.SearchTags;
import net.wurstclient.command.CmdException;
import net.wurstclient.command.CmdSyntaxError;
import net.wurstclient.command.Command;

@SearchTags({".legit", "dots in chat", "command bypass", "prefix"})
public final class SayCmd extends Command
{
	public SayCmd()
	{
		super("say",
			"发送给定的聊天消息,\n使你的消息可以以一个点开始\n如果不用.say指令,\n且发送的内容以点号开始,\n将会被判定为指令,\n一些服务器利用这点做出了反作弊插件,\n比如把登录指令改为\".l\"或\".login\"", ".say <消息>");
	}
	
	@Override
	public void call(String[] args) throws CmdException
	{
		if(args.length < 1)
			throw new CmdSyntaxError();
		
		String message = String.join(" ", args);
		ChatMessageC2SPacket packet = new ChatMessageC2SPacket(message);
		MC.getNetworkHandler().sendPacket(packet);
	}
}
