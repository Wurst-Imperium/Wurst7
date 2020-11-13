/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.events;

import java.util.ArrayList;

import net.minecraft.network.Packet;
import net.wurstclient.event.CancellableEvent;
import net.wurstclient.event.Listener;
import net.wurstclient.events.PacketOutputListener.PacketOutputEvent;

/**
 * Similar to {@link PacketOutputListener}, but also captures packets that are
 * sent before the client has finished connecting to the server. Most hacks
 * should use {@link PacketOutputListener} instead.
 */
public interface ConnectionPacketOutputListener extends Listener
{
	public void onSentConnectionPacket(ConnectionPacketOutputEvent event);
	
	/**
	 * Similar to {@link PacketOutputEvent}, but also captures packets that are
	 * sent before the client has finished connecting to the server. Most hacks
	 * should use {@link PacketOutputEvent} instead.
	 */
	public static class ConnectionPacketOutputEvent
		extends CancellableEvent<ConnectionPacketOutputListener>
	{
		private Packet<?> packet;
		
		public ConnectionPacketOutputEvent(Packet<?> packet)
		{
			this.packet = packet;
		}
		
		public Packet<?> getPacket()
		{
			return packet;
		}
		
		public void setPacket(Packet<?> packet)
		{
			this.packet = packet;
		}
		
		@Override
		public void fire(ArrayList<ConnectionPacketOutputListener> listeners)
		{
			for(ConnectionPacketOutputListener listener : listeners)
			{
				listener.onSentConnectionPacket(this);
				
				if(isCancelled())
					break;
			}
		}
		
		@Override
		public Class<ConnectionPacketOutputListener> getListenerType()
		{
			return ConnectionPacketOutputListener.class;
		}
	}
}
