/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class MinPriorityThreadFactory implements ThreadFactory
{
	private static final AtomicInteger poolNumber = new AtomicInteger(1);
	private final ThreadGroup group;
	private final AtomicInteger threadNumber = new AtomicInteger(1);
	private final String namePrefix;
	
	public MinPriorityThreadFactory()
	{
		SecurityManager s = System.getSecurityManager();
		group = s != null ? s.getThreadGroup()
			: Thread.currentThread().getThreadGroup();
		namePrefix = "pool-min-" + poolNumber.getAndIncrement() + "-thread-";
	}
	
	@Override
	public Thread newThread(Runnable r)
	{
		Thread t = new Thread(group, r,
			namePrefix + threadNumber.getAndIncrement(), 0);
		if(t.isDaemon())
			t.setDaemon(false);
		if(t.getPriority() != Thread.MIN_PRIORITY)
			t.setPriority(Thread.MIN_PRIORITY);
		return t;
	}
	
	public static ExecutorService newFixedThreadPool()
	{
		return Executors.newFixedThreadPool(
			Runtime.getRuntime().availableProcessors(),
			new MinPriorityThreadFactory());
	}
}
