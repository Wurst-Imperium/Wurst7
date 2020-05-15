/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayDeque;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.wurstclient.Category;
import net.wurstclient.events.PacketOutputListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.events.PacketOutputListener.PacketOutputEvent;
import net.wurstclient.hack.DontSaveState;
import net.wurstclient.hack.Hack;
import net.wurstclient.hacks.BlinkHack.TimedPacket;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.util.FakePlayerEntity;

@DontSaveState
public final class BlinkReplayHack extends Hack
	implements UpdateListener, PacketOutputListener
{	
	private TimedPacket[] packets;
	private int packetLength;
	private long recordingDuration;
	
	public BlinkReplayHack()
	{
		super("BlinkReplay", "Replays packets captured with blink to the server in real time.");
		setCategory(Category.MOVEMENT);
		this.packetLength = -1;
	}
	
	@Override
	public String getRenderName()
	{	
		if (packetLength == -1 || packets == null)
			return getName();
		return getName() + " [" + packets.length + "/" + packetLength + "]";
	}
	
	@Override
	public void onEnable()
	{
		BlinkHack blinkHack;
		try 
		{
			blinkHack = (BlinkHack)WURST.getHax().getHackByName("Blink");
		} 
		catch (Exception e) 
		{
			return;
		}
		
		packets = (TimedPacket[]) blinkHack.getPackets().toArray(new TimedPacket[0]);
		recordingDuration = System.currentTimeMillis() - blinkHack.startTime;
		packetLength = packets.length;
		
		blinkHack.sendPackets = false;
		blinkHack.setEnabled(false);
		
		EVENTS.add(PacketOutputListener.class, this);
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		packetLength = -1;
		
		EVENTS.remove(PacketOutputListener.class, this);
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		boolean sentAllPackets = true;
		long currentTime = System.currentTimeMillis();
		EVENTS.remove(PacketOutputListener.class, this);
		for (TimedPacket packet : packets) 
		{
			if (!packet.sent) {
				sentAllPackets = false;
				if (currentTime - packet.recordedTime >= recordingDuration) 
				{					MC.player.networkHandler.sendPacket(packet.packet);
					packet.sent = true;
				}
			}
			
		}
		EVENTS.add(PacketOutputListener.class, this);
		if (sentAllPackets)
			setEnabled(false);
	}
	
	@Override
	public void onSentPacket(PacketOutputEvent event)
	{
		if (event.getPacket() instanceof PlayerMoveC2SPacket)
			event.cancel();
	}
	
	public void cancel()
	{
		setEnabled(false);
	}
}
