/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
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

public interface RightClickListener extends Listener
{
	/**
	 * Fired in {@link MinecraftClient#doItemUse()} after the
	 * {@code interactionManager.isBreakingBlock()} check, but before the
	 * item use cooldown is increased.
	 */
	public void onRightClick(RightClickEvent event);
	
	public static class RightClickEvent
		extends CancellableEvent<RightClickListener>
	{
		@Override
		public void fire(ArrayList<RightClickListener> listeners)
		{
			for(RightClickListener listener : listeners)
			{
				listener.onRightClick(this);
				
				if(isCancelled())
					break;
			}
		}
		
		@Override
		public Class<RightClickListener> getListenerType()
		{
			return RightClickListener.class;
		}
	}
}
