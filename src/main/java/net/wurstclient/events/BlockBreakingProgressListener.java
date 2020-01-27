/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.events;

import java.util.ArrayList;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.wurstclient.event.Event;
import net.wurstclient.event.Listener;

public interface BlockBreakingProgressListener extends Listener
{
	public void onBlockBreakingProgress(BlockBreakingProgressEvent event);
	
	public static class BlockBreakingProgressEvent
		extends Event<BlockBreakingProgressListener>
	{
		private final BlockPos blockPos;
		private final Direction direction;
		
		public BlockBreakingProgressEvent(BlockPos blockPos,
			Direction direction)
		{
			this.blockPos = blockPos;
			this.direction = direction;
		}
		
		@Override
		public void fire(ArrayList<BlockBreakingProgressListener> listeners)
		{
			for(BlockBreakingProgressListener listener : listeners)
				listener.onBlockBreakingProgress(this);
		}
		
		@Override
		public Class<BlockBreakingProgressListener> getListenerType()
		{
			return BlockBreakingProgressListener.class;
		}
		
		public BlockPos getBlockPos()
		{
			return blockPos;
		}
		
		public Direction getDirection()
		{
			return direction;
		}
	}
}
