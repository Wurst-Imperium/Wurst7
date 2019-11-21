/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.events;

import java.util.ArrayList;

import net.wurstclient.event.Event;
import net.wurstclient.event.Listener;

public interface HitResultRayTraceListener extends Listener
{
	public void onHitResultRayTrace(float float_1);
	
	public static class HitResultRayTraceEvent
		extends Event<HitResultRayTraceListener>
	{
		private float float_1;
		
		public HitResultRayTraceEvent(float float_1)
		{
			this.float_1 = float_1;
		}
		
		@Override
		public void fire(ArrayList<HitResultRayTraceListener> listeners)
		{
			for(HitResultRayTraceListener listener : listeners)
				listener.onHitResultRayTrace(float_1);
		}
		
		@Override
		public Class<HitResultRayTraceListener> getListenerType()
		{
			return HitResultRayTraceListener.class;
		}
	}
}
