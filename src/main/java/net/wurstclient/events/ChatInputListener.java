/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.events;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.Text;
import net.wurstclient.event.CancellableEvent;
import net.wurstclient.event.Listener;

public interface ChatInputListener extends Listener
{
	public void onReceivedMessage(ChatInputEvent event);
	
	public static class ChatInputEvent
		extends CancellableEvent<ChatInputListener>
	{
		private Text component;
		private List<ChatHudLine> chatLines;
		
		public ChatInputEvent(Text component, List<ChatHudLine> chatLines)
		{
			this.component = component;
			this.chatLines = chatLines;
		}
		
		public Text getComponent()
		{
			return component;
		}
		
		public void setComponent(Text component)
		{
			this.component = component;
		}
		
		public List<ChatHudLine> getChatLines()
		{
			return chatLines;
		}
		
		@Override
		public void fire(ArrayList<ChatInputListener> listeners)
		{
			for(ChatInputListener listener : listeners)
			{
				listener.onReceivedMessage(this);
				
				if(isCancelled())
					break;
			}
		}
		
		@Override
		public Class<ChatInputListener> getListenerType()
		{
			return ChatInputListener.class;
		}
	}
}
