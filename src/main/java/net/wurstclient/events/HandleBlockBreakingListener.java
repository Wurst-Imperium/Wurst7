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
import net.wurstclient.event.CancellableEvent;
import net.wurstclient.event.Listener;

/**
 * Fired at the beginning of
 * {@link MinecraftClient#handleBlockBreaking(boolean)}.
 * Allows you to cancel vanilla block breaking and replace it with your own.
 */
public interface HandleBlockBreakingListener extends Listener
{
	/**
	 * Fired at the beginning of
	 * {@link MinecraftClient#handleBlockBreaking(boolean)}.
	 * Allows you to cancel vanilla block breaking and replace it with your own.
	 */
	public void onHandleBlockBreaking(HandleBlockBreakingEvent event);
	
	/**
	 * Fired at the beginning of
	 * {@link MinecraftClient#handleBlockBreaking(boolean)}.
	 * Allows you to cancel vanilla block breaking and replace it with your own.
	 */
	public static class HandleBlockBreakingEvent
		extends CancellableEvent<HandleBlockBreakingListener>
	{
		@Override
		public void fire(ArrayList<HandleBlockBreakingListener> listeners)
		{
			for(HandleBlockBreakingListener listener : listeners)
			{
				listener.onHandleBlockBreaking(this);
				
				if(isCancelled())
					break;
			}
		}
		
		@Override
		public Class<HandleBlockBreakingListener> getListenerType()
		{
			return HandleBlockBreakingListener.class;
		}
	}
}
