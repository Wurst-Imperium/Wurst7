/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.events;

import java.util.ArrayList;

import net.wurstclient.event.CancellableEvent;
import net.wurstclient.event.Listener;

public interface ChatOutputListener extends Listener
{
	public void onSentMessage(ChatOutputEvent event);
	
	public static class ChatOutputEvent
		extends CancellableEvent<ChatOutputListener>
	{
		private String message;
		private boolean automatic;
		
		public ChatOutputEvent(String message, boolean automatic)
		{
			this.message = message;
			this.automatic = automatic;
		}
		
		public String getMessage()
		{
			return message;
		}
		
		public void setMessage(String message)
		{
			this.message = message;
		}
		
		public boolean isAutomatic()
		{
			return automatic;
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
