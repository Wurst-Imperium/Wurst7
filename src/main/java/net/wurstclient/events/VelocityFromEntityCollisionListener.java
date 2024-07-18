/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.events;

import java.util.ArrayList;

import net.minecraft.entity.Entity;
import net.wurstclient.event.CancellableEvent;
import net.wurstclient.event.Listener;

public interface VelocityFromEntityCollisionListener extends Listener
{
	public void onVelocityFromEntityCollision(
		VelocityFromEntityCollisionEvent event);
	
	public static class VelocityFromEntityCollisionEvent
		extends CancellableEvent<VelocityFromEntityCollisionListener>
	{
		private final Entity entity;
		
		public VelocityFromEntityCollisionEvent(Entity entity)
		{
			this.entity = entity;
		}
		
		public Entity getEntity()
		{
			return entity;
		}
		
		@Override
		public void fire(
			ArrayList<VelocityFromEntityCollisionListener> listeners)
		{
			for(VelocityFromEntityCollisionListener listener : listeners)
			{
				listener.onVelocityFromEntityCollision(this);
				
				if(isCancelled())
					break;
			}
		}
		
		@Override
		public Class<VelocityFromEntityCollisionListener> getListenerType()
		{
			return VelocityFromEntityCollisionListener.class;
		}
	}
}
