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
	
	private final ArrayDeque<PlayerMoveC2SPacket> packets = new ArrayDeque<>();
	private FakePlayerEntity fakePlayer;
	
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
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(PacketOutputListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(PacketOutputListener.class, this);
		
		fakePlayer.despawn();
		packets.forEach(p -> MC.player.networkHandler.sendPacket(p));
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
		if(!(event.getPacket() instanceof PlayerMoveC2SPacket))
			return;
		
		event.cancel();
		
		PlayerMoveC2SPacket packet = (PlayerMoveC2SPacket)event.getPacket();
		PlayerMoveC2SPacket prevPacket = packets.peekLast();
		
		if(prevPacket != null && packet.isOnGround() == prevPacket.isOnGround()
			&& packet.getYaw(-1) == prevPacket.getYaw(-1)
			&& packet.getPitch(-1) == prevPacket.getPitch(-1)
			&& packet.getX(-1) == prevPacket.getX(-1)
			&& packet.getY(-1) == prevPacket.getY(-1)
			&& packet.getZ(-1) == prevPacket.getZ(-1))
			return;
		
		packets.addLast(packet);
	}
	
	public void cancel()
	{
		packets.clear();
		fakePlayer.resetPlayerPosition();
		setEnabled(false);
	}
}
