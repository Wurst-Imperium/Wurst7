/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.entity.passive.AbstractDonkeyEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.InteractionType;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.PacketOutputListener;
import net.wurstclient.hack.Hack;

@SearchTags({"mount bypass", "donkey chest", "AutoMount", "auto mount"})
public final class MountBypassHack extends Hack implements PacketOutputListener
{
	public MountBypassHack()
	{
		super("MountBypass",
			"Allows you to mount chests on donkeys, llamas\n"
				+ "and mules on servers that disable it,\n"
				+ "allowing for a donkey chest duplication glitch.");
		setCategory(Category.OTHER);
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(PacketOutputListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(PacketOutputListener.class, this);
	}
	
	@Override
	public void onSentPacket(PacketOutputEvent event)
	{
		if(!(event.getPacket() instanceof PlayerInteractEntityC2SPacket))
			return;
		
		PlayerInteractEntityC2SPacket packet =
			(PlayerInteractEntityC2SPacket)event.getPacket();
		
		if(!(packet.getEntity(MC.world) instanceof AbstractDonkeyEntity))
			return;
		
		if(packet.getType() != InteractionType.INTERACT_AT)
			return;
		
		event.cancel();
	}
}
