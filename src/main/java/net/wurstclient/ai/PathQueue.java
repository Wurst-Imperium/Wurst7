/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.ai;

import java.util.Iterator;
import java.util.PriorityQueue;

public class PathQueue
{
	private final PriorityQueue<PathQueue.Entry> queue =
		new PriorityQueue<>((e1, e2) -> {
			return Float.compare(e1.priority, e2.priority);
		});
	
	private class Entry
	{
		private PathPos pos;
		private float priority;
		
		public Entry(PathPos pos, float priority)
		{
			this.pos = pos;
			this.priority = priority;
		}
	}
	
	public boolean isEmpty()
	{
		return queue.isEmpty();
	}
	
	public boolean add(PathPos pos, float priority)
	{
		return queue.add(new Entry(pos, priority));
	}
	
	public PathPos[] toArray()
	{
		PathPos[] array = new PathPos[size()];
		Iterator<Entry> itr = queue.iterator();
		
		for(int i = 0; i < size() && itr.hasNext(); i++)
			array[i] = itr.next().pos;
		
		return array;
	}
	
	public int size()
	{
		return queue.size();
	}
	
	public void clear()
	{
		queue.clear();
	}
	
	public PathPos poll()
	{
		return queue.poll().pos;
	}
}
