/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.events;

import java.util.ArrayList;

import net.wurstclient.event.CancellableEvent;
import net.wurstclient.event.Listener;

public interface VelocityFromFluidListener extends Listener
{
	public void onVelocityFromFluid(VelocityFromFluidEvent event);
	
	public static class VelocityFromFluidEvent
		extends CancellableEvent<VelocityFromFluidListener>
	{
		@Override
		public void fire(ArrayList<VelocityFromFluidListener> listeners)
		{
			for(VelocityFromFluidListener listener : listeners)
			{
				listener.onVelocityFromFluid(this);
				
				if(isCancelled())
					break;
			}
		}
		
		@Override
		public Class<VelocityFromFluidListener> getListenerType()
		{
			return VelocityFromFluidListener.class;
		}
	}
}
