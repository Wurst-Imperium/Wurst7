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

public interface AirStrafingSpeedListener extends Listener
{
	public void onGetAirStrafingSpeed(AirStrafingSpeedEvent event);
	
	public static class AirStrafingSpeedEvent
		extends Event<AirStrafingSpeedListener>
	{
		private float airStrafingSpeed;
		private final float defaultSpeed;
		
		public AirStrafingSpeedEvent(float airStrafingSpeed)
		{
			this.airStrafingSpeed = airStrafingSpeed;
			defaultSpeed = airStrafingSpeed;
		}
		
		public float getSpeed()
		{
			return airStrafingSpeed;
		}
		
		public void setSpeed(float airStrafingSpeed)
		{
			this.airStrafingSpeed = airStrafingSpeed;
		}
		
		public float getDefaultSpeed()
		{
			return defaultSpeed;
		}
		
		@Override
		public void fire(ArrayList<AirStrafingSpeedListener> listeners)
		{
			for(AirStrafingSpeedListener listener : listeners)
				listener.onGetAirStrafingSpeed(this);
		}
		
		@Override
		public Class<AirStrafingSpeedListener> getListenerType()
		{
			return AirStrafingSpeedListener.class;
		}
	}
}
