/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.minecraft.util.math.BlockPos;

public final class BlockBreakingCache
{
	private final ArrayDeque<Set<BlockPos>> prevBlocks = new ArrayDeque<>();
	
	/**
	 * Waits 5 ticks before trying to break the same block again, which
	 * makes it much more likely that the server will accept the block
	 * breaking packets.
	 */
	public ArrayList<BlockPos> filterOutRecentBlocks(Stream<BlockPos> stream)
	{
		for(Set<BlockPos> set : prevBlocks)
			stream = stream.filter(pos -> !set.contains(pos));
		
		ArrayList<BlockPos> blocks =
			stream.collect(Collectors.toCollection(ArrayList::new));
		
		prevBlocks.addLast(new HashSet<>(blocks));
		while(prevBlocks.size() > 5)
			prevBlocks.removeFirst();
		
		return blocks;
	}
	
	public void reset()
	{
		prevBlocks.clear();
	}
}
