/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.logging.Logger;

import net.minecraft.network.Packet;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Position;
import net.wurstclient.Category;
import net.wurstclient.events.PacketOutputListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.DontSaveState;
import net.wurstclient.hack.Hack;
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
	
	private final ArrayDeque<TimedPacket> packets = new ArrayDeque<>();
	protected FakePlayerEntity fakePlayer;
	public long startTime;
	public boolean sendPackets;
	public ArrayList<PlayerMoveC2SPacket> packetBlackList = new ArrayList<>();

	public ArrayDeque<TimedPacket> getPackets() {
		return packets;
	}
	
	public BlinkHack()
	{
		super("Blink", "Suspends all motion updates while enabled.");
		setCategory(Category.MOVEMENT);
		addSetting(limit);
	}
	
	@Override
	public String getRenderName()
	{
		if(limit.getValueI() == 0)
			return getName() + " [" + packets.size() + "]";
		else
			return getName() + " [" + packets.size() + "/" + limit.getValueI()
				+ "]";
	}
	
	@Override
	public void onEnable()
	{
		fakePlayer = new FakePlayerEntity();
		startTime = System.currentTimeMillis();
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(PacketOutputListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(PacketOutputListener.class, this);

		fakePlayer.despawn();
		if (sendPackets) { // Sendpackets is used as a flag because setEnable(false) can't have additional args
			packets.forEach(p -> MC.player.networkHandler.sendPacket(p.packet));
		}
		sendPackets = false;
		packets.clear();
	}
	
	@Override
	public void onUpdate()
	{
		if(limit.getValueI() == 0)
			return;
		
		if(packets.size() >= limit.getValueI())
		{
			setEnabled(false);
			setEnabled(true);
		}
	}
	
	@Override
	public void onSentPacket(PacketOutputEvent event)
	{
		Packet p = event.getPacket();

		if (packetBlackList.contains(p))
			return;
		else if (	(p instanceof PlayerInputC2SPacket) || // These are responsible sprinting and sneaking
					(p instanceof ClientCommandC2SPacket)) {
			event.cancel();
			return; // Don't log these
		} else if (!(p instanceof PlayerMoveC2SPacket))
			return;

		event.cancel();
		
		TimedPacket timedPacket = new TimedPacket((PlayerMoveC2SPacket) p);
		TimedPacket prevTimedPacket = packets.peekLast();
		
		PlayerMoveC2SPacket prevPacket = null;
		if (prevTimedPacket != null)
			prevPacket = prevTimedPacket.packet;
		PlayerMoveC2SPacket packet = timedPacket.packet;
		
		if(prevPacket != null && packet.isOnGround() == prevPacket.isOnGround()
			&& packet.getYaw(-1) == prevPacket.getYaw(-1)
			&& packet.getPitch(-1) == prevPacket.getPitch(-1)
			&& packet.getX(-1) == prevPacket.getX(-1)
			&& packet.getY(-1) == prevPacket.getY(-1)
			&& packet.getZ(-1) == prevPacket.getZ(-1))
			return;
		
		packets.addLast(timedPacket);
	}
	
	public void cancel()
	{
		packets.clear();
		fakePlayer.resetPlayerPosition();
		setEnabled(false);
	}

	public final class TimedPacket implements Packet<ServerPlayPacketListener> {
		public long recordedTime;
		public boolean sent;
		public PlayerMoveC2SPacket packet;
		
		public TimedPacket(PlayerMoveC2SPacket packet) {
			this.packet = packet;
			this.recordedTime = System.currentTimeMillis();
			this.sent = false;
		}

		@Override
		public void read(PacketByteBuf buf) throws IOException {
			packet.read(buf);
		}

		@Override
		public void write(PacketByteBuf buf) throws IOException {
			packet.write(buf);
		}

		@Override
		public void apply(ServerPlayPacketListener listener) {
			packet.apply(listener);
		}
	}
}
