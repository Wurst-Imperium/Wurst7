/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.events;

import java.util.ArrayList;

import net.wurstclient.event.CancellableEvent;
import net.wurstclient.event.Listener;

public interface SetOpaqueCubeListener extends Listener
{
	public void onSetOpaqueCube(SetOpaqueCubeEvent event);
	
	public static class SetOpaqueCubeEvent
		extends CancellableEvent<SetOpaqueCubeListener>
	{
		@Override
		public void fire(ArrayList<SetOpaqueCubeListener> listeners)
		{
			for(SetOpaqueCubeListener listener : listeners)
			{
				listener.onSetOpaqueCube(this);
				
				if(isCancelled())
					break;
			}
		}
		
		@Override
		public Class<SetOpaqueCubeListener> getListenerType()
		{
			return SetOpaqueCubeListener.class;
		}
	}
}
