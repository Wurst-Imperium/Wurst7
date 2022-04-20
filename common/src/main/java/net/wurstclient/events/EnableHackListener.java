/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.events;

import java.util.ArrayList;

import net.wurstclient.event.Event;
import net.wurstclient.event.Listener;
import net.wurstclient.hack.Hack;

public interface EnableHackListener extends Listener
{
	void onToggle(EnableHackEvent enableHackEvent);

	class EnableHackEvent extends Event<EnableHackListener>
	{
		private final Hack toggledHack;

		public EnableHackEvent(Hack toggledHack)
		{
			this.toggledHack = toggledHack;
		}

		@Override
		public void fire(ArrayList<EnableHackListener> listeners)
		{
			for(EnableHackListener listener : listeners)
				listener.onToggle(this);
		}

		@Override
		public Class<EnableHackListener> getListenerType()
		{
			return EnableHackListener.class;
		}

		public Hack getToggledHack()
		{
			return toggledHack;
		}

	}
}
