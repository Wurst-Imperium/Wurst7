/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.stream.Stream;

import net.minecraft.network.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
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
		0, 0, 500, 1, v -> v == 0 ? "disabled" : (int)v + "");

	private final ArrayList<Class> packetWhitelist = new ArrayList<>();
	private final ArrayDeque<PlayerMoveC2SPacket> blinkedMovementPackets = new ArrayDeque<>();
	private final ArrayDeque<Packet> blinkedOtherPackets = new ArrayDeque<>();
	private final ArrayList<ReplayPacketContainer> replayingPackets = new ArrayList<>();
	private ArrayList<Packet> sentPackets = new ArrayList<>();

	private FakePlayerEntity blinkPlayer;
	private FakePlayerEntity replayPlayer;
	private ArrayList<FakePlayerEntity> checkpointPlayers;

	private int packetsSent;
	private long startTime;
	private long recordingDuration;

	private boolean replaying;

	public int getBlinkedPacketsSize() { return blinkedMovementPackets.size() + blinkedOtherPackets.size(); }

	public BlinkHack()
	{
		super("Blink", "Suspends all motion updates while enabled.");
		setCategory(Category.MOVEMENT);
		addSetting(limit);
		this.replaying = false;

		// Packet types to operate on
		this.packetWhitelist.add(ClientCommandC2SPacket.class);
		this.packetWhitelist.add(PlayerInputC2SPacket.class);
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
		if (replaying)
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

	private void enable() {
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
		disable(true);
	}

	private void disable(boolean flushPackets) {
		if (blinkPlayer != null)
			blinkPlayer.despawn();
		if (replayPlayer != null)
			replayPlayer.despawn();
		if (flushPackets) {
			blinkedMovementPackets.forEach(p -> MC.player.networkHandler.sendPacket(p));
			blinkedOtherPackets.forEach(p -> MC.player.networkHandler.sendPacket(p));
		}
		blinkedMovementPackets.clear();
		blinkedOtherPackets.clear();
	}
	
	@Override
	public void onUpdate()
	{
		if (replaying)
		{
			sendTimedPackets();
			replayPlayer.animateLimbs();
		}

		if (limit.getValueI() != 0 &&getBlinkedPacketsSize() >= limit.getValueI())
		{
			reset();
		}
	}
	
	@Override
	public void onSentPacket(PacketOutputEvent event)
	{
		Packet packet = event.getPacket();

		if (packet instanceof PlayerMoveC2SPacket)
		{
			PlayerMoveC2SPacket movementPacket = (PlayerMoveC2SPacket) packet;
			PlayerMoveC2SPacket prevMovementPacket = blinkedMovementPackets.peekLast();

			// To save on packets, only add if the player has moved
			if (prevMovementPacket == null || !movementPacketsChanged(movementPacket, prevMovementPacket))
			{
				blinkedMovementPackets.addLast(movementPacket);
			}
		}
		else if (packetWhitelist.contains(packet.getClass())) {
			blinkedOtherPackets.addLast(packet);
		} else return;

		// Only cancel if it is a movement packet or it is whitelisted
		event.cancel();
	}

	private void sendTimedPackets() {
		boolean sentAllPackets = true;
		long currentTime = System.currentTimeMillis();
		EVENTS.remove(PacketOutputListener.class, this);
		for (ReplayPacketContainer container : replayingPackets)
		{
			if (!container.sent)
			{ // Not sent
				System.out.println("not sent");
				sentAllPackets = false;
				if (currentTime - container.sendTime >= recordingDuration) { // The packet is due to be sent
					MC.player.networkHandler.sendPacket(container.packet);
					container.sent = true;
					if (container.packet instanceof PlayerMoveC2SPacket) // Visualize the packet using the fake player
						updateFakePlayerPos(replayPlayer, (PlayerMoveC2SPacket) container.packet, true);
				}
				packetsSent++;
			}
		}
		EVENTS.add(PacketOutputListener.class, this);
		if (sentAllPackets)
			replaying = false;
	}

	private boolean movementPacketsChanged(PlayerMoveC2SPacket packet, PlayerMoveC2SPacket prevPacket) {
		return packet.isOnGround() == prevPacket.isOnGround()
				&& packet.getYaw(-1) == prevPacket.getYaw(-1)
				&& packet.getPitch(-1) == prevPacket.getPitch(-1)
				&& packet.getX(-1) == prevPacket.getX(-1)
				&& packet.getY(-1) == prevPacket.getY(-1)
				&& packet.getZ(-1) == prevPacket.getZ(-1);
	}

	public void reset() { reset(true); } // Flush by default
	public void reset(boolean flushPackets) {
		disable(flushPackets);
		enable();
	}
	
	public void cancel()
	{
		blinkedMovementPackets.clear();
		blinkPlayer.resetPlayerPosition();
		setEnabled(false);
	}

	public void replay()
	{
		replaying = true;
		recordingDuration = System.currentTimeMillis() - startTime;

		// Add the packets
		for (Packet packet : blinkedMovementPackets)
		{
			ReplayPacketContainer container = new ReplayPacketContainer(packet);
			replayingPackets.add(container);
		}
		for (Packet packet : blinkedOtherPackets)
		{
			ReplayPacketContainer container = new ReplayPacketContainer(packet);
			replayingPackets.add(container);
		}

		replayPlayer = new FakePlayerEntity(blinkPlayer);
		replayPlayer.setName("Replaying...");
		reset(false); // Restart blink at the current position
	}

	private void updateFakePlayerPos(FakePlayerEntity player, PlayerMoveC2SPacket cringePacket, boolean interpolate) {

		IPlayerMoveC2SPacket packet = (IPlayerMoveC2SPacket) cringePacket;

		double x = packet.getX();
		double y = packet.getY();
		double z = packet.getZ();

		float yaw = packet.getYaw();
		float pitch = packet.getPitch();

		if (x == 0 && y == 0 && z == 0)
			return; // Don't animate

		player.updateTrackedPositionAndAngles(x, y, z, yaw, pitch, 3, true);
		player.updateTrackedHeadRotation(yaw, 3);
	}

	private final class ReplayPacketContainer
	{
		protected long sendTime;
		protected boolean sent;
		protected Packet packet;
		public ReplayPacketContainer (Packet packetIn)
		{
			this.packet = packetIn;
			this.sendTime = System.currentTimeMillis();
			this.sent = false;
		}
	}
}
