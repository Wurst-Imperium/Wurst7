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
import org.apache.logging.log4j.Logger;

@DontSaveState
public final class BlinkHack extends Hack
	implements UpdateListener, PacketOutputListener
{
	private final SliderSetting limit = new SliderSetting("Limit",
		"Automatically restarts Blink once\n" + "the given number of packets\n"
			+ "have been suspended.\n\n" + "0 = no limit",
		0, 0, 500, 1, v -> v == 0 ? "disabled" : (int)v + "");

	private final ArrayDeque<PacketContainer> blinkedMovementPackets = new ArrayDeque<>();
	private final ArrayDeque<PacketContainer> blinkedOtherPackets = new ArrayDeque<>();
	private final ArrayList<PacketContainer> replayingPackets = new ArrayList<>();

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

		if (replayPlayer != null)
			replayPlayer.despawn();
		disable(true);
	}

	private void disable(boolean flushPackets) {
		if (blinkPlayer != null)
			blinkPlayer.despawn();
		if (flushPackets) {
			blinkedMovementPackets.forEach(p -> MC.player.networkHandler.sendPacket(p.packet));
			blinkedOtherPackets.forEach(p -> MC.player.networkHandler.sendPacket(p.packet));
		}
		blinkedMovementPackets.clear();
		blinkedOtherPackets.clear();
	}
	
	@Override
	public void onUpdate()
	{
		if (replaying)
			sendTimedPackets();

		if (replayPlayer != null)
			System.out.println(replayPlayer.removed);

		if (limit.getValueI() != 0 && getBlinkedPacketsSize() >= limit.getValueI())
		{
			reset(true);
		}
	}

	@Override
	public void onSentPacket(PacketOutputEvent event)
	{
		Packet packet = event.getPacket();

		if (packet instanceof PlayerMoveC2SPacket)
		{
			PlayerMoveC2SPacket movementPacket = (PlayerMoveC2SPacket) packet;
			PacketContainer prevPacket = blinkedMovementPackets.peekLast();

			// To save on packets, only add if the player has moved
			if (prevPacket == null || !movementPacketsChanged(movementPacket, (PlayerMoveC2SPacket) prevPacket.packet))
			{
				blinkedMovementPackets.addLast(new PacketContainer(movementPacket));
			}
		} else if (packet instanceof ClientCommandC2SPacket) {
			blinkedOtherPackets.addLast(new PacketContainer(packet));
		} else return;

		// Only cancel if it is a movement packet or it is whitelisted
		event.cancel();
	}

	private void sendTimedPackets() {
		boolean sentAllPackets = true;
		long currentTime = System.currentTimeMillis();
		EVENTS.remove(PacketOutputListener.class, this);
		for (PacketContainer container : replayingPackets)
		{
			if (!container.sent)
			{ // Not sent
				sentAllPackets = false;
				if (currentTime - container.sendTime >= recordingDuration)
				{ // The packet is due to be sent
					Packet packet = container.packet;
					container.sent = true;
					packetsSent++;
					MC.player.networkHandler.sendPacket(packet);
					if (packet instanceof PlayerMoveC2SPacket) // Visualize the packet using the fake player
						updateFakePlayerPos(replayPlayer, (PlayerMoveC2SPacket) packet);
				}
			}
		}
		EVENTS.add(PacketOutputListener.class, this);
		if (sentAllPackets) {
			replaying = false;
			replayingPackets.clear();
			packetsSent = 0;
		}
	}

	private boolean movementPacketsChanged(PlayerMoveC2SPacket packet, PlayerMoveC2SPacket prevPacket) {
		return packet.isOnGround() == prevPacket.isOnGround()
				&& packet.getYaw(-1) == prevPacket.getYaw(-1)
				&& packet.getPitch(-1) == prevPacket.getPitch(-1)
				&& packet.getX(-1) == prevPacket.getX(-1)
				&& packet.getY(-1) == prevPacket.getY(-1)
				&& packet.getZ(-1) == prevPacket.getZ(-1);
	}

	private void reset(boolean flushPackets) {
		disable(flushPackets);
		enable();
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

		if (replayingPackets.size() == 0)
			return;

		if (!replaying) // Dont override if already replaying
			recordingDuration = System.currentTimeMillis() - startTime;

		if (replayPlayer == null || replayPlayer.removed)
			replayPlayer = new FakePlayerEntity(blinkPlayer);

		replayPlayer.setName("Replaying...");

		replaying = true;
		reset(false); // Restart blink at the current position
	}

	private void updateFakePlayerPos(FakePlayerEntity fakePlayer, PlayerMoveC2SPacket cringePacket)
	{
		IPlayerMoveC2SPacket packet = (IPlayerMoveC2SPacket) cringePacket;

		double x = packet.getX();
		double y = packet.getY();
		double z = packet.getZ();
		float yaw = packet.getYaw();
		float pitch = packet.getPitch();

		if (cringePacket instanceof PlayerMoveC2SPacket.PositionOnly)
			fakePlayer.updatePosition(x, y, z);
		else if (cringePacket instanceof PlayerMoveC2SPacket.LookOnly)
			fakePlayer.setRotation(yaw, pitch); // setRotation is protected (cringe)
		else
			fakePlayer.updateTrackedPositionAndAngles(x, y, z, yaw, pitch, 3, true);
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
