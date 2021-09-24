/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.PacketOutputListener;
import net.wurstclient.hack.Hack;

@SearchTags({"potion saver"})
public final class PotionSaverHack extends Hack implements PacketOutputListener
{
	public PotionSaverHack()
	{
		super("PotionSaver",
			"Freezes all potion effects while you are standing still.");
		setCategory(Category.OTHER);
	}
	
	@Override
	protected void onEnable()
	{
		EVENTS.add(PacketOutputListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(PacketOutputListener.class, this);
	}
	
	@Override
	public void onSentPacket(PacketOutputEvent event)
	{
		if(!isFrozen())
			return;
		
		if(event.getPacket() instanceof PlayerMoveC2SPacket)
			event.cancel();
	}
	
	public boolean isFrozen()
	{
		return isEnabled() && MC.player != null
			&& !MC.player.getActiveStatusEffects().isEmpty()
			&& MC.player.getVelocity().x == 0 && MC.player.getVelocity().z == 0;
	}
}
