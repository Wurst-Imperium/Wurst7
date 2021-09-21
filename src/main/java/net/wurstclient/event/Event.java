/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.event;

import java.util.ArrayList;

public abstract class Event<T extends Listener>
{
	public abstract void fire(ArrayList<T> listeners);
	
	public abstract Class<T> getListenerType();
}
