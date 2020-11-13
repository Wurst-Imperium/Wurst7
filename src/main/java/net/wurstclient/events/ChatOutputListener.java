/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.events;

import java.util.ArrayList;
import java.util.Objects;

import net.wurstclient.event.CancellableEvent;
import net.wurstclient.event.Listener;

public interface ChatOutputListener extends Listener
{
	public void onSentMessage(ChatOutputEvent event);
	
	public static class ChatOutputEvent
		extends CancellableEvent<ChatOutputListener>
	{
		private final String originalMessage;
		private String message;
		
		public ChatOutputEvent(String message)
		{
			this.message = Objects.requireNonNull(message);
			originalMessage = message;
		}
		
		public String getMessage()
		{
			return message;
		}
		
		public void setMessage(String message)
		{
			this.message = message;
		}
		
		public String getOriginalMessage()
		{
			return originalMessage;
		}
		
		public boolean isModified()
		{
			return !originalMessage.equals(message);
		}
		
		@Override
		public void fire(ArrayList<ChatOutputListener> listeners)
		{
			for(ChatOutputListener listener : listeners)
			{
				listener.onSentMessage(this);
				
				if(isCancelled())
					break;
			}
		}
		
		@Override
		public Class<ChatOutputListener> getListenerType()
		{
			return ChatOutputListener.class;
		}
	}
}
