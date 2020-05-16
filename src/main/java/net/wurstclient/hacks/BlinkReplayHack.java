/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.network.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.wurstclient.Category;
import net.wurstclient.events.PacketOutputListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.DontSaveState;
import net.wurstclient.hack.Hack;
import net.wurstclient.hacks.BlinkHack.TimedPacket;
import net.wurstclient.util.FakePlayerEntity;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;

import static jdk.nashorn.internal.runtime.regexp.joni.Config.log;

@DontSaveState
public final class BlinkReplayHack extends Hack
	implements UpdateListener, PacketOutputListener
{
	private ArrayList<TimedPacket> packets;
	private FakePlayerEntity fakePlayer;
	private int packetsSent;
	private int packetLength;
	private long recordingDuration;
	private BlinkHack blinkHack;
	
	public BlinkReplayHack()
	{
		super("BlinkReplay", "Replays packets captured with blink to the server in real time.");
		setCategory(Category.MOVEMENT);
		this.packetLength = 0;
		this.packets = new ArrayList<>();
	}
	
	@Override
	public String getRenderName()
	{	
		if (packetLength == 0 || packets == null)
			return getName();
		else
			return getName() + " [" + packetsSent + "/" + packetLength + "]";
	}
	
	@Override
	public void onEnable()
	{
		try {
			blinkHack = WURST.getHax().blinkHack;
		} catch (NullPointerException e) {
			this.setEnabled(false);
			return;
		}

		if (!blinkHack.isEnabled()) {
			this.setEnabled(false);
			return;
		}

		recordingDuration = System.currentTimeMillis() - blinkHack.startTime;
		this.appendPackets();
		if (packetLength == 0)
			return;

		fakePlayer = new FakePlayerEntity(blinkHack.fakePlayer);

		blinkHack.packetBlackList = new ArrayList<>(); // Clear the blacklist
		blinkHack.sendPackets = false;
		blinkHack.setEnabled(false);
		blinkHack.setEnabled(true);

		EVENTS.add(PacketOutputListener.class, this);
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		packetLength = 0;
		packetsSent = 0;

		fakePlayer.despawn();

		EVENTS.remove(PacketOutputListener.class, this);
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		boolean sentAllPackets = true;
		long currentTime = System.currentTimeMillis();
		packetsSent = 0;
		EVENTS.remove(PacketOutputListener.class, this);
		for (TimedPacket packet : packets) 
		{
			if (!packet.sent) {
				sentAllPackets = false;
				if (currentTime - packet.recordedTime >= recordingDuration) 
				{
					blinkHack.packetBlackList.add(packet.packet);
					MC.player.networkHandler.sendPacket(packet.packet);
					packet.sent = true;
					packetsSent++;
					updateFakePlayerPos(packet.packet, true);
				} else {
					packetsSent++;
				}
			}
		}
		EVENTS.add(PacketOutputListener.class, this);
		if (sentAllPackets)
			setEnabled(false);
		fakePlayer.animateLimbs();
	}
	
	@Override
	public void onSentPacket(PacketOutputEvent event)
	{
		if (!blinkHack.isEnabled()) {
			if (event.getPacket() instanceof PlayerMoveC2SPacket)
				event.cancel();
		}
	}
	
	public void cancel()
	{
		setEnabled(false);
	}

	public void appendPackets() {
		for (TimedPacket packet : blinkHack.getPackets()) {
			packets.add(packet);
		}
		packetLength += blinkHack.getPackets().size();
	}

	private void updateFakePlayerPos(PlayerMoveC2SPacket packet, boolean interpolate) {
		try
		{ // Cringe packet has protected fields, forcing me to disgrace the name of inheritance.
			Class c = new PlayerMoveC2SPacket().getClass();
			c.getDeclaredField("x").setAccessible(true);
			c.getDeclaredField("y").setAccessible(true);
			c.getDeclaredField("z").setAccessible(true);
			c.getDeclaredField("pitch").setAccessible(true);
			c.getDeclaredField("yaw").setAccessible(true);
		}
		catch (NoSuchFieldException e) {

		}

		double x;
		double y;
		double z;
		float yaw;
		float pitch;

		try
		{
			x = (Double) FieldUtils.readField(packet, "x", true);
			y = (Double) FieldUtils.readField(packet, "y", true);
			z = (Double) FieldUtils.readField(packet, "z", true);
			yaw = (Float) FieldUtils.readField(packet, "yaw", true);
			pitch = (Float) FieldUtils.readField(packet, "pitch", true);

		}
		catch (IllegalAccessException e) { return; }
		catch (IllegalArgumentException e) { return ; }

		if (x == 0 && y == 0 && z == 0)
			return; // Don't animate

		fakePlayer.updateTrackedPositionAndAngles(x, y, z, yaw, pitch, 3, true);
		fakePlayer.updateTrackedHeadRotation(yaw, 3);
	}
}
