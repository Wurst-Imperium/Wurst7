/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.ChatOutputListener;
import net.wurstclient.hack.Hack;

@SearchTags({"fancy chat"})
public final class FancyChatHack extends Hack implements ChatOutputListener
{
	private final String blacklist = "(){}[]|";
	
	public FancyChatHack()
	{
		super("Unicode消息", "用unicode字符替换发送的聊天消息中的ASCII字符\n可以用来绕过一些服务器上的脏话屏蔽器\n不能在禁用unicode字符的服务器上使用");
		setCategory(Category.CHAT);
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(ChatOutputListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(ChatOutputListener.class, this);
	}
	
	@Override
	public void onSentMessage(ChatOutputEvent event)
	{
		String message = event.getOriginalMessage();
		if(message.startsWith("/") || message.startsWith("."))
			return;
		
		String newMessage = convertString(message);
		event.setMessage(newMessage);
	}
	
	private String convertString(String input)
	{
		String output = "";
		for(char c : input.toCharArray())
			output += convertChar(c);
		
		return output;
	}
	
	private String convertChar(char c)
	{
		if(c < 0x21 || c > 0x80)
			return "" + c;
		
		if(blacklist.contains(Character.toString(c)))
			return "" + c;
		
		return new String(Character.toChars(c + 0xfee0));
	}
}
