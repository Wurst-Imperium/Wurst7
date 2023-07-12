/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;

@SearchTags({"no fall"})
public final class NoFallHack extends Hack implements UpdateListener
{
	private final CheckboxSetting allowElytra = new CheckboxSetting(
		"Allow elytra",
		"Also tries to prevent fall damage while you are flying with an elytra.\n\n"
			+ "\u00a7c\u00a7lWARNING:\u00a7r This can sometimes cause you to"
			+ " stop flying unexpectedly.",
		false);
	
	public NoFallHack()
	{
		super("NoFall");
		setCategory(Category.MOVEMENT);
		addSetting(allowElytra);
	}
	
	@Override
	public String getRenderName()
	{
		if(MC.player != null && MC.player.isFallFlying()
			&& !allowElytra.isChecked())
			return getName() + " (paused)";
		
		return getName();
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		ClientPlayerEntity player = MC.player;
		boolean fallFlying = player.isFallFlying();
		
		// pause when flying with elytra, unless allowed
		if(fallFlying && !allowElytra.isChecked())
			return;
		
		// ignore small falls that can't cause damage
		if(player.fallDistance <= (fallFlying ? 1 : 2))
			return;
		
		// attempt to fix elytra weirdness, if allowed
		if(fallFlying && player.isSneaking()
			&& !isFallingFastEnoughToCauseDamage(player))
			return;
		
		// send packet to stop fall damage
		player.networkHandler.sendPacket(new OnGroundOnly(true));
	}
	
	private boolean isFallingFastEnoughToCauseDamage(ClientPlayerEntity player)
	{
		return player.getVelocity().y < -0.5;
	}
}
