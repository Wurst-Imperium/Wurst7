/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.events;

import java.util.ArrayList;

import net.wurstclient.event.Event;
import net.wurstclient.event.Listener;

public interface MouseButtonPressListener extends Listener
{
	public void onMouseButtonPress(MouseButtonPressEvent event);
	
	public static class MouseButtonPressEvent
		extends Event<MouseButtonPressListener>
	{
		private final int button;
		private final int action;
		
		public MouseButtonPressEvent(int button, int action)
		{
			this.button = button;
			this.action = action;
		}
		
		@Override
		public void fire(ArrayList<MouseButtonPressListener> listeners)
		{
			for(MouseButtonPressListener listener : listeners)
				listener.onMouseButtonPress(this);
		}
		
		@Override
		public Class<MouseButtonPressListener> getListenerType()
		{
			return MouseButtonPressListener.class;
		}
		
		public int getButton()
		{
			return button;
		}
		
		public int getAction()
		{
			return action;
		}
	}
}
