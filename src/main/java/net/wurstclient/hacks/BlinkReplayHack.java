///*
// * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
// *
// * This source code is subject to the terms of the GNU General Public
// * License, version 3. If a copy of the GPL was not distributed with this
// * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
// */
//package net.wurstclient.hacks;
//
//import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
//import net.wurstclient.Category;
//import net.wurstclient.events.PacketOutputListener;
//import net.wurstclient.events.UpdateListener;
//import net.wurstclient.hack.DontSaveState;
//import net.wurstclient.hack.Hack;
//import net.wurstclient.hacks.BlinkHack;
//import net.wurstclient.mixinterface.IPlayerMoveC2SPacket;
//import net.wurstclient.util.FakePlayerEntity;
//import org.apache.commons.lang3.reflect.FieldUtils;
//
//import java.util.ArrayList;
//
//@DontSaveState
//public final class BlinkReplayHack extends Hack
//	implements UpdateListener, PacketOutputListener
//{
//	private ArrayList<PacketContainer> packets;
//	private FakePlayerEntity fakePlayer;
//	private int packetsSent;
//	private int packetLength;
//	private long recordingDuration;
//	private BlinkHack blinkHack;
//
//	public BlinkReplayHack()
//	{
//		super("BlinkReplay", "Replays packets captured with blink to the server in real time.", false);
//		setCategory(Category.MOVEMENT);
//		this.packetLength = 0;
//		this.packets = new ArrayList<>();
//		addPossibleKeybind(this.getName(), "Trigger " + this.getName());
//	}
//
//	@Override
//	public String getRenderName()
//	{
//		if (packetLength == 0 || packets == null)
//			return getName();
//		else
//			return getName() + " [" + packetsSent + "/" + packetLength + "]";
//	}
//
//	@Override
//	public void onEnable()
//	{
//		try {
//			blinkHack = WURST.getHax().blinkHack;
//		} catch (NullPointerException e) {
//			this.setEnabled(false);
//			return;
//		}
//
//		if (!blinkHack.isEnabled()) {
//			this.setEnabled(false);
//			return;
//		}
//
//		recordingDuration = System.currentTimeMillis() - blinkHack.startTime;
//		this.appendPackets();
//		if (packetLength == 0)
//			return;
//
//		fakePlayer = new FakePlayerEntity(blinkHack.blinkPlayer);
//		fakePlayer.setName("Replaying...");
//
//		EVENTS.add(PacketOutputListener.class, this);
//		EVENTS.add(UpdateListener.class, this);
//	}
//
//	@Override
//	public void onDisable()
//	{
//		packetLength = 0;
//		packetsSent = 0;
//
//		if (fakePlayer != null)
//			fakePlayer.despawn();
//
//		EVENTS.remove(PacketOutputListener.class, this);
//		EVENTS.remove(UpdateListener.class, this);
//	}
//
//	@Override
//	public void onUpdate()
//	{
//		boolean sentAllPackets = true;
//		long currentTime = System.currentTimeMillis();
//		packetsSent = 0;
//		EVENTS.remove(PacketOutputListener.class, this);
//		for (PacketContainer packet : packets)
//		{
//			if (!packet.sent) {
//				sentAllPackets = false;
//				if (currentTime - packet.recordedTime >= recordingDuration)
//				{
//					blinkHack.packetBlackList.add(packet.packet);
//					MC.player.networkHandler.sendPacket(packet.packet);
//					packet.sent = true;
//					packetsSent++;
//					updateFakePlayerPos(packet.packet, true);
//				} else {
//					packetsSent++;
//				}
//			}
//		}
//		EVENTS.add(PacketOutputListener.class, this);
//		if (sentAllPackets)
//			setEnabled(false);
//		fakePlayer.animateLimbs();
//	}
//
//	@Override
//	public void onSentPacket(PacketOutputEvent event)
//	{
//		if (!blinkHack.isEnabled()) {
//			if (event.getPacket() instanceof PlayerMoveC2SPacket)
//				event.cancel();
//		}
//	}
//
//	@Override
//	public final String getPrimaryAction()
//	{
//		return "Trigger";
//	}
//
//	@Override
//	public final void doPrimaryAction()
//	{
//		if (this.isEnabled()) {
//			appendPackets();
//		} else {
//			this.setEnabled(true);
//		}
//	}
//
//	public void cancel()
//	{
//		setEnabled(false);
//	}
//
//
//
//
//}
