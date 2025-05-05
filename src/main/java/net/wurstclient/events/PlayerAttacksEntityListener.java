/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.events;

import java.util.ArrayList;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.wurstclient.event.Event;
import net.wurstclient.event.Listener;

/**
 * Fired at the beginning of
 * {@link ClientPlayerInteractionManager#attackEntity(PlayerEntity, Entity)}.
 */
public interface PlayerAttacksEntityListener extends Listener
{
	/**
	 * Fired at the beginning of
	 * {@link ClientPlayerInteractionManager#attackEntity(PlayerEntity, Entity)}.
	 */
	public void onPlayerAttacksEntity(Entity target);
	
	/**
	 * Fired at the beginning of
	 * {@link ClientPlayerInteractionManager#attackEntity(PlayerEntity, Entity)}.
	 */
	public static class PlayerAttacksEntityEvent
		extends Event<PlayerAttacksEntityListener>
	{
		private final Entity target;
		
		public PlayerAttacksEntityEvent(Entity target)
		{
			this.target = target;
		}
		
		@Override
		public void fire(ArrayList<PlayerAttacksEntityListener> listeners)
		{
			for(PlayerAttacksEntityListener listener : listeners)
				listener.onPlayerAttacksEntity(target);
		}
		
		@Override
		public Class<PlayerAttacksEntityListener> getListenerType()
		{
			return PlayerAttacksEntityListener.class;
		}
	}
}
