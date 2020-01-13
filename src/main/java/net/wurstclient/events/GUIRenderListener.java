/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.events;

import java.util.ArrayList;

import net.wurstclient.event.Event;
import net.wurstclient.event.Listener;

public interface GUIRenderListener extends Listener
{
	public void onRenderGUI(float partialTicks);
	
	public static class GUIRenderEvent extends Event<GUIRenderListener>
	{
		private final float partialTicks;
		
		public GUIRenderEvent(float partialTicks)
		{
			this.partialTicks = partialTicks;
		}
		
		@Override
		public void fire(ArrayList<GUIRenderListener> listeners)
		{
			for(GUIRenderListener listener : listeners)
				listener.onRenderGUI(partialTicks);
		}
		
		@Override
		public Class<GUIRenderListener> getListenerType()
		{
			return GUIRenderListener.class;
		}
	}
}
