/*
 * Copyright (c) 2014-2025 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Items;
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
		"Allow elytra", "description.wurst.setting.nofall.allow_elytra", false);
	
	private final CheckboxSetting pauseForMace =
		new CheckboxSetting("Pause for mace",
			"description.wurst.setting.nofall.pause_for_mace", false);
	
	public NoFallHack()
	{
		super("NoFall");
		setCategory(Category.MOVEMENT);
		addSetting(allowElytra);
		addSetting(pauseForMace);
	}
	
	@Override
	public String getRenderName()
	{
		ClientPlayerEntity player = MC.player;
		if(player == null)
			return getName();
		
		if(player.isGliding() && !allowElytra.isChecked())
			return getName() + " (paused)";
		
		if(player.getAbilities().creativeMode)
			return getName() + " (paused)";
		
		if(pauseForMace.isChecked() && isHoldingMace(player))
			return getName() + " (paused)";
		
		return getName();
	}
	
	@Override
	protected void onEnable()
	{
		WURST.getHax().antiHungerHack.setEnabled(false);
		EVENTS.add(UpdateListener.class, this);
	}
	
	@Override
	protected void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		// do nothing in creative mode, since there is no fall damage anyway
		ClientPlayerEntity player = MC.player;
		if(player.getAbilities().creativeMode)
			return;
		
		// pause when flying with elytra, unless allowed
		boolean fallFlying = player.isGliding();
		if(fallFlying && !allowElytra.isChecked())
			return;
		
		// pause when holding a mace, if enabled
		if(pauseForMace.isChecked() && isHoldingMace(player))
			return;
		
		// attempt to fix elytra weirdness, if allowed
		if(fallFlying && player.isSneaking()
			&& !isFallingFastEnoughToCauseDamage(player))
			return;
		
		// send packet to stop fall damage
		player.networkHandler
			.sendPacket(new OnGroundOnly(true, MC.player.horizontalCollision));
	}
	
	private boolean isHoldingMace(ClientPlayerEntity player)
	{
		return player.getMainHandStack().isOf(Items.MACE);
	}
	
	private boolean isFallingFastEnoughToCauseDamage(ClientPlayerEntity player)
	{
		return player.getVelocity().y < -0.5;
	}
}
