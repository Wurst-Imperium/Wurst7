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

public interface PlayerMoveListener extends Listener
{
	public void onPlayerMove();
	
	public static class PlayerMoveEvent extends Event<PlayerMoveListener>
	{
		public static final PlayerMoveEvent INSTANCE = new PlayerMoveEvent();
		
		@Override
		public void fire(ArrayList<PlayerMoveListener> listeners)
		{
			for(PlayerMoveListener listener : listeners)
				listener.onPlayerMove();
		}
		
		@Override
		public Class<PlayerMoveListener> getListenerType()
		{
			return PlayerMoveListener.class;
		}
	}
}
