/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.events;

import java.util.ArrayList;

import net.wurstclient.event.CancellableEvent;
import net.wurstclient.event.Listener;
import net.minecraft.entity.Entity;

public interface LeftClickListener extends Listener
{
	public void onLeftClick(LeftClickEvent event);
	
	public static class LeftClickEvent extends CancellableEvent<LeftClickListener>
	{
		private Entity entity;
		public LeftClickEvent(Entity entity)
		{
			this.entity = entity;
		}
		
		public getEntity()
		{
			return this.entity;
		}
		
		@Override
		public void fire(ArrayList<LeftClickListener> listeners)
		{
			for(LeftClickListener listener : listeners)
			{
				listener.onLeftClick(this);
				
				if(isCancelled())
					break;
			}
		}
		
		@Override
		public Class<LeftClickListener> getListenerType()
		{
			return LeftClickListener.class;
		}
	}
}
