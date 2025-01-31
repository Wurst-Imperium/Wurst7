/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.events;

import java.util.ArrayList;

import net.minecraft.client.MinecraftClient;
import net.wurstclient.event.Event;
import net.wurstclient.event.Listener;

/**
 * Fired at the beginning of {@link MinecraftClient#handleInputEvents()}.
 * This is the ideal time to simulate keyboard input.
 */
public interface HandleInputListener extends Listener
{
	/**
	 * Fired at the beginning of {@link MinecraftClient#handleInputEvents()}.
	 * This is the ideal time to simulate keyboard input.
	 */
	public void onHandleInput();
	
	/**
	 * Fired at the beginning of {@link MinecraftClient#handleInputEvents()}.
	 * This is the ideal time to simulate keyboard input.
	 */
	public static class HandleInputEvent extends Event<HandleInputListener>
	{
		public static final HandleInputEvent INSTANCE = new HandleInputEvent();
		
		@Override
		public void fire(ArrayList<HandleInputListener> listeners)
		{
			for(HandleInputListener listener : listeners)
				listener.onHandleInput();
		}
		
		@Override
		public Class<HandleInputListener> getListenerType()
		{
			return HandleInputListener.class;
		}
	}
}
