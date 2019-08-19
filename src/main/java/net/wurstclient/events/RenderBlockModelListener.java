/*
 * Copyright (C) 2014 - 2019 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.events;

import java.util.ArrayList;

import net.minecraft.block.BlockState;
import net.wurstclient.event.CancellableEvent;
import net.wurstclient.event.Listener;

public interface RenderBlockModelListener extends Listener
{
	public void onRenderBlockModel(RenderBlockModelEvent event);
	
	public static class RenderBlockModelEvent
		extends CancellableEvent<RenderBlockModelListener>
	{
		private final BlockState state;
		
		public RenderBlockModelEvent(BlockState state)
		{
			this.state = state;
		}
		
		public BlockState getState()
		{
			return state;
		}
		
		@Override
		public void fire(ArrayList<RenderBlockModelListener> listeners)
		{
			for(RenderBlockModelListener listener : listeners)
			{
				listener.onRenderBlockModel(this);
				
				if(isCancelled())
					break;
			}
		}
		
		@Override
		public Class<RenderBlockModelListener> getListenerType()
		{
			return RenderBlockModelListener.class;
		}
	}
}
