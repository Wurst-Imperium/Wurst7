/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.events;

import java.util.ArrayList;
import java.util.Objects;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.wurstclient.event.Event;
import net.wurstclient.event.Listener;

public interface ShouldDrawSideListener extends Listener
{
	public void onShouldDrawSide(ShouldDrawSideEvent event);
	
	public static class ShouldDrawSideEvent
		extends Event<ShouldDrawSideListener>
	{
		private final BlockState state;
		private final BlockPos pos;
		private Boolean rendered;
		
		public ShouldDrawSideEvent(BlockState state, BlockPos pos)
		{
			this.state = Objects.requireNonNull(state);
			this.pos = pos;
		}
		
		public BlockState getState()
		{
			return state;
		}
		
		public BlockPos getPos()
		{
			return pos;
		}
		
		public Boolean isRendered()
		{
			return rendered;
		}
		
		public void setRendered(boolean rendered)
		{
			this.rendered = rendered;
		}
		
		@Override
		public void fire(ArrayList<ShouldDrawSideListener> listeners)
		{
			for(ShouldDrawSideListener listener : listeners)
				listener.onShouldDrawSide(this);
		}
		
		@Override
		public Class<ShouldDrawSideListener> getListenerType()
		{
			return ShouldDrawSideListener.class;
		}
	}
}
