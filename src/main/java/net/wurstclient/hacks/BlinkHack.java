/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayDeque;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.wurstclient.Category;
import net.wurstclient.events.PacketOutputListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.DontSaveState;
import net.wurstclient.hack.Hack;
import net.wurstclient.mixinterface.IPlayerMoveC2SPacket;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.util.FakePlayerEntity;

@DontSaveState
public final class BlinkHack extends Hack
		implements UpdateListener, PacketOutputListener
{
	private final SliderSetting limit = new SliderSetting("Limit",
			"Automatically restarts Blink once\n" + "the given number of packets\n"
					+ "have been suspended.\n\n" + "0 = no limit",
			0, 0, 500, 1, v -> v == 0 ? "disabled" : (int) v + "");
	
	private final ArrayDeque<PacketContainer> blinkedMovementPackets = new ArrayDeque<>();
	private final ArrayDeque<PacketContainer> blinkedOtherPackets = new ArrayDeque<>();
	private final ArrayDeque<PacketContainer> replayingPackets = new ArrayDeque<>();
	
	private FakePlayerEntity blinkPlayer;
	private FakePlayerEntity replayPlayer;
	
	private int packetsSent;
	private long startTime;
	private long recordingDuration;
	private boolean replaying;
	
	public BlinkHack()
	{
		super("Blink", "Suspends all motion updates while enabled.");
		setCategory(Category.MOVEMENT);
		addSetting(limit);
		this.replaying = false;
	}
	
	public int getBlinkedPacketsSize()
	{
		return blinkedMovementPackets.size() + blinkedOtherPackets.size();
	}
	
	@Override
	public String getRenderName()
	{
		// Blink
		String format = "%s [%s";
		if(limit.getValueI() > 0)
			format += "/%s";
		format += "]";
		String output = String.format(format, getName(), getBlinkedPacketsSize(), limit.getValueI());
		
		// Replay
		if(replaying)
			output += String.format(" [%s/%s]", packetsSent, replayingPackets.size());
		
		return output;
	}
	
	@Override
	public void onEnable()
	{
		replaying = false; // Just started, so cant be replying
		enable();
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(PacketOutputListener.class, this);
	}
	
	private void enable()
	{
		blinkPlayer = new FakePlayerEntity();
		blinkPlayer.setName("Blinking...");
		startTime = System.currentTimeMillis();
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(PacketOutputListener.class, this);
		
		replaying = false;
		
		if(replayPlayer != null)
			replayPlayer.despawn();
		disable(true);
	}
	
	private void disable(boolean flushPackets)
	{
		if(blinkPlayer != null)
			blinkPlayer.despawn();
		if(flushPackets)
		{
			blinkedMovementPackets.forEach(p -> MC.player.networkHandler.sendPacket(p.packet));
			blinkedOtherPackets.forEach(p -> MC.player.networkHandler.sendPacket(p.packet));
		}
		blinkedMovementPackets.clear();
		blinkedOtherPackets.clear();
	}
	
	@Override
	public void onUpdate()
	{
		if(replaying)
			sendTimedPackets();
		
		if(replayPlayer != null)
			System.out.println(replayPlayer.removed);
		
		if(limit.getValueI() != 0 && getBlinkedPacketsSize() >= limit.getValueI())
		{
			disable(true);
			enable();
		}
	}
	
	@Override
	public void onSentPacket(PacketOutputEvent event)
	{
		Packet packet = event.getPacket();
		
		if(packet instanceof PlayerMoveC2SPacket)
		{
			PlayerMoveC2SPacket movementPacket = (PlayerMoveC2SPacket) packet;
			PacketContainer prevPacket = blinkedMovementPackets.peekLast();
			
			// To save on packets, only add if the player has moved
			if(prevPacket == null || !movementPacketsChanged(movementPacket, (PlayerMoveC2SPacket) prevPacket.packet))
				blinkedMovementPackets.addLast(new PacketContainer(movementPacket));
		} else if(packet instanceof ClientCommandC2SPacket)
			blinkedOtherPackets.addLast(new PacketContainer(packet));
		else return;
		
		// Only cancel if it is a movement packet or it is whitelisted
		event.cancel();
	}
	
	private void sendTimedPackets()
	{
		boolean sentAllPackets = true;
		long currentTime = System.currentTimeMillis();
		EVENTS.remove(PacketOutputListener.class, this);
		for(PacketContainer container : replayingPackets)
		{
			if(!container.sent)
			{ // Not sent
				sentAllPackets = false;
				if(currentTime - container.sendTime >= recordingDuration)
				{ // The packet is due to be sent
					Packet packet = container.packet;
					container.sent = true;
					packetsSent++;
					MC.player.networkHandler.sendPacket(packet);
					if(packet instanceof PlayerMoveC2SPacket) // Visualize the packet using the fake player
						updateFakePlayerPos(replayPlayer, (PlayerMoveC2SPacket) packet);
				}
			}
		}
		EVENTS.add(PacketOutputListener.class, this);
		if(sentAllPackets)
		{
			replaying = false;
			replayingPackets.clear();
			packetsSent = 0;
			replayPlayer.despawn();
		}
	}
	
	private boolean movementPacketsChanged(PlayerMoveC2SPacket packet, PlayerMoveC2SPacket prevPacket)
	{
		return packet.isOnGround() == prevPacket.isOnGround()
				&& packet.getYaw(-1) == prevPacket.getYaw(-1)
				&& packet.getPitch(-1) == prevPacket.getPitch(-1)
				&& packet.getX(-1) == prevPacket.getX(-1)
				&& packet.getY(-1) == prevPacket.getY(-1)
				&& packet.getZ(-1) == prevPacket.getZ(-1);
	}
	
	// .blink reset
	public void reset()
	{
		blinkedMovementPackets.clear();
		blinkedOtherPackets.clear();
		blinkPlayer.resetPlayerPosition();
	}
	
	public void cancel()
	{
		blinkedMovementPackets.clear();
		blinkedOtherPackets.clear();
		replayingPackets.clear();
		blinkPlayer.resetPlayerPosition();
		setEnabled(false);
	}
	
	public void replay()
	{
		// Add the packets
		replayingPackets.addAll(blinkedMovementPackets);
		replayingPackets.addAll(blinkedOtherPackets);
		
		if(replayingPackets.size() == 0)
			return;
		
		if(!replaying) // Dont override if already replaying
			recordingDuration = System.currentTimeMillis() - startTime;
		
		if(replayPlayer == null || replayPlayer.removed)
			replayPlayer = new FakePlayerEntity(blinkPlayer);
		
		replayPlayer.setName("Replaying...");
		
		replaying = true;
		disable(false);
		enable();
	}
	
	private void updateFakePlayerPos(FakePlayerEntity fakePlayer, PlayerMoveC2SPacket cringePacket)
	{
		IPlayerMoveC2SPacket packet = (IPlayerMoveC2SPacket) cringePacket;
		
		double x = packet.getX();
		double y = packet.getY();
		double z = packet.getZ();
		float yaw = packet.getYaw();
		float pitch = packet.getPitch();
		
		if(yaw == 0 && pitch == 0)
		{
			yaw = fakePlayer.getYaw(1.0F);
			pitch = fakePlayer.getPitch(1.0F);
		}
		
		if(x == 0 && y == 0 && z == 0)
		{
			x = fakePlayer.getX();
			y = fakePlayer.getY();
			z = fakePlayer.getZ();
		}
		
		fakePlayer.updateTrackedPositionAndAngles(x, y, z, yaw, pitch, 3, true);
		fakePlayer.updateTrackedHeadRotation(yaw, 3);
	}
	
	private final class PacketContainer
	{
		protected long sendTime;
		protected boolean sent;
		protected Packet packet;
		
		public PacketContainer(Packet packetIn)
		{
			this.packet = packetIn;
			this.sendTime = System.currentTimeMillis();
			this.sent = false;
		}
	}
}
