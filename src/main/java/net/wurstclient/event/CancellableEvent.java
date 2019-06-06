/*
 * Copyright (C) 2014 - 2018 | Wurst-Imperium | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.event;

public abstract class CancellableEvent<T extends Listener> extends Event<T>
{
	private boolean cancelled = false;
	
	public void cancel()
	{
		cancelled = true;
	}
	
	public boolean isCancelled()
	{
		return cancelled;
	}
}
