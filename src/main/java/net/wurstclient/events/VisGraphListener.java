/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.events;

import java.util.ArrayList;

import net.wurstclient.event.CancellableEvent;
import net.wurstclient.event.Listener;

public interface VisGraphListener extends Listener
{
	/**
	 * Cancel this event to turn off the visibility graph, making things like
	 * caves become visible that would normally be hidden behind other blocks
	 * and thus skipped for better rendering performance.
	 */
	public void onVisGraph(VisGraphEvent event);
	
	public static class VisGraphEvent extends CancellableEvent<VisGraphListener>
	{
		@Override
		public void fire(ArrayList<VisGraphListener> listeners)
		{
			for(VisGraphListener listener : listeners)
			{
				listener.onVisGraph(this);
				
				if(isCancelled())
					break;
			}
		}
		
		@Override
		public Class<VisGraphListener> getListenerType()
		{
			return VisGraphListener.class;
		}
	}
}
