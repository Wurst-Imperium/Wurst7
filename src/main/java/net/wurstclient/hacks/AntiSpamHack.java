/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.List;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.util.ChatMessages;
import net.minecraft.text.CharacterVisitor;
import net.minecraft.text.LiteralText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
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
		List<ChatHudLine<OrderedText>> chatLines = event.getChatLines();
		if(chatLines.isEmpty())
			return;
		
		/**
		 * A {@link CharacterVisitor} to completely bypass Mojang's visitor
		 * system and just get the damn {@link String} out of a
		 * {@link ChatHudLine}.
		 *
		 * <p>
		 * Is this seriously the replacement for <code>getString()</code>?
		 * What were they thinking?!
		 */
		class JustGiveMeTheStringVisitor implements CharacterVisitor
		{
			StringBuilder sb = new StringBuilder();
			
			@Override
			public boolean accept(int index, Style style, int codePoint)
			{
				sb.appendCodePoint(codePoint);
				return true;
			}
			
			@Override
			public String toString()
			{
				return sb.toString();
			}
		}
		
		ChatHud chat = MC.inGameHud.getChatHud();
		int maxTextLength =
			MathHelper.floor(chat.getWidth() / chat.getChatScale());
		List<OrderedText> newLines = ChatMessages.breakRenderedChatMessageLines(
			event.getComponent(), maxTextLength, MC.textRenderer);
		
		int spamCounter = 1;
		int matchingLines = 0;
		
		for(int i = chatLines.size() - 1; i >= 0; i--)
		{
			JustGiveMeTheStringVisitor oldLineVS =
				new JustGiveMeTheStringVisitor();
			chatLines.get(i).getText().accept(oldLineVS);
			String oldLine = oldLineVS.toString();
			
			if(matchingLines <= newLines.size() - 1)
			{
				JustGiveMeTheStringVisitor newLineVS =
					new JustGiveMeTheStringVisitor();
				newLines.get(matchingLines).accept(newLineVS);
				String newLine = newLineVS.toString();
				
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
					JustGiveMeTheStringVisitor nextOldLineVS =
						new JustGiveMeTheStringVisitor();
					chatLines.get(i - 1).getText().accept(nextOldLineVS);
					String nextOldLine = nextOldLineVS.toString();
					
					String twoLines = oldLine + nextOldLine;
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
			event.setComponent(new LiteralText(
				event.getComponent().getString() + " [x" + spamCounter + "]"));
	}
}
