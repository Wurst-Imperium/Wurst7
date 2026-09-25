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

public interface FlyingSpeedListener extends Listener
{
	public void onGetFlyingSpeed(FlyingSpeedEvent event);
	
	public static class FlyingSpeedEvent extends Event<FlyingSpeedListener>
	{
		private float flyingSpeed;
		private final float defaultSpeed;
		
		public FlyingSpeedEvent(float flyingSpeed)
		{
			this.flyingSpeed = flyingSpeed;
			defaultSpeed = flyingSpeed;
		}
		
		public float getSpeed()
		{
			return flyingSpeed;
		}
		
		public void setSpeed(float flyingSpeed)
		{
			this.flyingSpeed = flyingSpeed;
		}
		
		public float getDefaultSpeed()
		{
			return defaultSpeed;
		}
		
		@Override
		public void fire(ArrayList<FlyingSpeedListener> listeners)
		{
			for(FlyingSpeedListener listener : listeners)
				listener.onGetFlyingSpeed(this);
		}
		
		@Override
		public Class<FlyingSpeedListener> getListenerType()
		{
			return FlyingSpeedListener.class;
		}
	}
}
