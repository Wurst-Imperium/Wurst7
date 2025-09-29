/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.events;

import net.minecraft.entity.Entity;
import net.wurstclient.event.CancellableEvent;
import net.wurstclient.event.Listener;

import java.util.ArrayList;

public interface GetPlayerDepthStriderListener extends Listener
{
	public void onGetPlayerDepthStrider(GetPlayerDepthStriderEvent event);
	
	public static class GetPlayerDepthStriderEvent
		extends CancellableEvent<GetPlayerDepthStriderListener>
	{
		private final Entity entity;
		
		public GetPlayerDepthStriderEvent(Entity entity)
		{
			this.entity = entity;
		}
		
		public Entity getEntity()
		{
			return entity;
		}
		
		@Override
		public void fire(ArrayList<GetPlayerDepthStriderListener> listeners)
		{
			for(GetPlayerDepthStriderListener listener : listeners)
			{
				listener.onGetPlayerDepthStrider(this);
				if(isCancelled())
					break;
			}
		}
		
		@Override
		public Class<GetPlayerDepthStriderListener> getListenerType()
		{
			return GetPlayerDepthStriderListener.class;
		}
	}
}
