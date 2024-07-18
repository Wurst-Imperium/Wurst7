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

public interface StopUsingItemListener extends Listener
{
	public void onStopUsingItem();
	
	public static class StopUsingItemEvent extends Event<StopUsingItemListener>
	{
		public static final StopUsingItemEvent INSTANCE =
			new StopUsingItemEvent();
		
		@Override
		public void fire(ArrayList<StopUsingItemListener> listeners)
		{
			for(StopUsingItemListener listener : listeners)
				listener.onStopUsingItem();
		}
		
		@Override
		public Class<StopUsingItemListener> getListenerType()
		{
			return StopUsingItemListener.class;
		}
	}
}
