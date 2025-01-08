/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.events;

import java.util.ArrayList;

import net.minecraft.client.Mouse;
import net.wurstclient.event.Event;
import net.wurstclient.event.Listener;

/**
 * Fired at the beginning of {@link Mouse#tick()}.
 * This is the ideal time to simulate mouse input.
 */
public interface MouseUpdateListener extends Listener
{
	/**
	 * Fired at the beginning of {@link Mouse#tick()}.
	 * This is the ideal time to simulate mouse input.
	 */
	public void onMouseUpdate(MouseUpdateEvent event);
	
	/**
	 * Fired at the beginning of {@link Mouse#tick()}.
	 * This is the ideal time to simulate mouse input.
	 */
	public static class MouseUpdateEvent extends Event<MouseUpdateListener>
	{
		private double deltaX;
		private double deltaY;
		private final double defaultDeltaX;
		private final double defaultDeltaY;
		
		public MouseUpdateEvent(double deltaX, double deltaY)
		{
			this.deltaX = deltaX;
			this.deltaY = deltaY;
			defaultDeltaX = deltaX;
			defaultDeltaY = deltaY;
		}
		
		@Override
		public void fire(ArrayList<MouseUpdateListener> listeners)
		{
			for(MouseUpdateListener listener : listeners)
				listener.onMouseUpdate(this);
		}
		
		@Override
		public Class<MouseUpdateListener> getListenerType()
		{
			return MouseUpdateListener.class;
		}
		
		public double getDeltaX()
		{
			return deltaX;
		}
		
		public void setDeltaX(double deltaX)
		{
			this.deltaX = deltaX;
		}
		
		public double getDeltaY()
		{
			return deltaY;
		}
		
		public void setDeltaY(double deltaY)
		{
			this.deltaY = deltaY;
		}
		
		public double getDefaultDeltaX()
		{
			return defaultDeltaX;
		}
		
		public double getDefaultDeltaY()
		{
			return defaultDeltaY;
		}
	}
}
