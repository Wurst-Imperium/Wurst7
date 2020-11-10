/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
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

public interface TesselateBlockListener extends Listener
{
	public void onTesselateBlock(TesselateBlockEvent event);
	
	public static class TesselateBlockEvent
		extends CancellableEvent<TesselateBlockListener>
	{
		private final BlockState state;
		
		public TesselateBlockEvent(BlockState state)
		{
			this.state = state;
		}
		
		public BlockState getState()
		{
			return state;
		}
		
		@Override
		public void fire(ArrayList<TesselateBlockListener> listeners)
		{
			for(TesselateBlockListener listener : listeners)
			{
				listener.onTesselateBlock(this);
				
				if(isCancelled())
					break;
			}
		}
		
		@Override
		public Class<TesselateBlockListener> getListenerType()
		{
			return TesselateBlockListener.class;
		}
	}
}
