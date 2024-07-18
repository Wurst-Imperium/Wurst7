/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.events;

import java.util.ArrayList;

import net.wurstclient.event.Event;
import net.wurstclient.event.Listener;

public interface IsPlayerInLavaListener extends Listener
{
	public void onIsPlayerInLava(IsPlayerInLavaEvent event);
	
	public static class IsPlayerInLavaEvent
		extends Event<IsPlayerInLavaListener>
	{
		private boolean inLava;
		private final boolean normallyInLava;
		
		public IsPlayerInLavaEvent(boolean inLava)
		{
			this.inLava = inLava;
			normallyInLava = inLava;
		}
		
		public boolean isInLava()
		{
			return inLava;
		}
		
		public void setInLava(boolean inLava)
		{
			this.inLava = inLava;
		}
		
		public boolean isNormallyInLava()
		{
			return normallyInLava;
		}
		
		@Override
		public void fire(ArrayList<IsPlayerInLavaListener> listeners)
		{
			for(IsPlayerInLavaListener listener : listeners)
				listener.onIsPlayerInLava(this);
		}
		
		@Override
		public Class<IsPlayerInLavaListener> getListenerType()
		{
			return IsPlayerInLavaListener.class;
		}
	}
}
