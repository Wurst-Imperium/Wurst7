/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.List;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.util.Texts;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.ChatInputListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.util.MathUtils;

@SearchTags({"NoSpam", "ChatFilter", "anti spam", "no spam", "chat filter"})
public final class AntiSpamHack extends Hack implements ChatInputListener
{
	public AntiSpamHack()
	{
		super("AntiSpam",
			"Blocks chat spam by adding a counter to repeated\n" + "messages.");
		setCategory(Category.CHAT);
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(ChatInputListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(ChatInputListener.class, this);
	}
	
	@Override
	public void onReceivedMessage(ChatInputEvent event)
	{
		List<ChatHudLine> chatLines = event.getChatLines();
		if(chatLines.isEmpty())
			return;
		
		ChatHud chat = MC.inGameHud.getChatHud();
		int maxTextLength =
			MathHelper.floor(chat.getWidth() / chat.getChatScale());
		List<Text> newLines = Texts.wrapLines(event.getComponent(),
			maxTextLength, MC.textRenderer, false, false);
		
		int spamCounter = 1;
		int matchingLines = 0;
		
		for(int i = chatLines.size() - 1; i >= 0; i--)
		{
			String oldLine = chatLines.get(i).getText().getString();
			
			if(matchingLines <= newLines.size() - 1)
			{
				String newLine = newLines.get(matchingLines).getString();
				
				if(matchingLines < newLines.size() - 1)
				{
					if(oldLine.equals(newLine))
						matchingLines++;
					else
						matchingLines = 0;
					
					continue;
				}
				
				if(!oldLine.startsWith(newLine))
				{
					matchingLines = 0;
					continue;
				}
				
				if(i > 0 && matchingLines == newLines.size() - 1)
				{
					String twoLines =
						oldLine + chatLines.get(i - 1).getText().getString();
					String addedText = twoLines.substring(newLine.length());
					
					if(addedText.startsWith(" [x") && addedText.endsWith("]"))
					{
						String oldSpamCounter =
							addedText.substring(3, addedText.length() - 1);
						
						if(MathUtils.isInteger(oldSpamCounter))
						{
							spamCounter += Integer.parseInt(oldSpamCounter);
							matchingLines++;
							continue;
						}
					}
				}
				
				if(oldLine.length() == newLine.length())
					spamCounter++;
				else
				{
					String addedText = oldLine.substring(newLine.length());
					if(!addedText.startsWith(" [x") || !addedText.endsWith("]"))
					{
						matchingLines = 0;
						continue;
					}
					
					String oldSpamCounter =
						addedText.substring(3, addedText.length() - 1);
					if(!MathUtils.isInteger(oldSpamCounter))
					{
						matchingLines = 0;
						continue;
					}
					
					spamCounter += Integer.parseInt(oldSpamCounter);
				}
			}
			
			for(int i2 = i + matchingLines; i2 >= i; i2--)
				chatLines.remove(i2);
			matchingLines = 0;
		}
		
		if(spamCounter > 1)
			event.getComponent().append(" [x" + spamCounter + "]");
	}
}
