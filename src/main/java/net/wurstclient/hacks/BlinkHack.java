/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import java.util.ArrayDeque;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.PacketOutputListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.DontSaveState;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.util.FakePlayerEntity;

@DontSaveState
@SearchTags({"LagSwitch", "lag switch"})
public final class BlinkHack extends Hack
	implements UpdateListener, PacketOutputListener
{
	private final SliderSetting limit = new SliderSetting("Limit",
		"Automatically restarts Blink once the given number of packets have been suspended.\n\n"
			+ "0 = no limit",
		0, 0, 500, 1, ValueDisplay.INTEGER.withLabel(0, "disabled"));
	
	private final ArrayDeque<ServerboundMovePlayerPacket> packets =
		new ArrayDeque<>();
	private FakePlayerEntity fakePlayer;
	
	public BlinkHack()
	{
		super("Blink");
		setCategory(Category.MOVEMENT);
		addSetting(limit);
	}
	
	@Override
	public String getRenderName()
	{
		if(limit.getValueI() == 0)
			return getName() + " [" + packets.size() + "]";
		return getName() + " [" + packets.size() + "/" + limit.getValueI()
			+ "]";
	}
	
	@Override
	protected void onEnable()
	{
		fakePlayer = new FakePlayerEntity();
		
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(PacketOutputListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(PacketOutputListener.class, this);
		
		fakePlayer.despawn();
		packets.forEach(p -> MC.player.connection.send(p));
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
		if(!(event.getPacket() instanceof ServerboundMovePlayerPacket))
			return;
		
		event.cancel();
		
		ServerboundMovePlayerPacket packet =
			(ServerboundMovePlayerPacket)event.getPacket();
		ServerboundMovePlayerPacket prevPacket = packets.peekLast();
		
		if(prevPacket != null && packet.isOnGround() == prevPacket.isOnGround()
			&& packet.getYRot(-1) == prevPacket.getYRot(-1)
			&& packet.getXRot(-1) == prevPacket.getXRot(-1)
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
