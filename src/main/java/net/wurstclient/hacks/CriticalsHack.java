/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.packet.PlayerMoveC2SPacket;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.LeftClickListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.EnumSetting;

@SearchTags({"Crits"})
public final class CriticalsHack extends Hack implements LeftClickListener
{
	private final EnumSetting<Mode> mode =
		new EnumSetting<>("Mode", Mode.values(), Mode.PACKET);
	
	public CriticalsHack()
	{
		super("Criticals", "Changes all your hits to critical hits.");
		setCategory(Category.COMBAT);
		addSetting(mode);
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(LeftClickListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(LeftClickListener.class, this);
	}
	
	@Override
	public void onLeftClick(LeftClickEvent event)
	{
		if(MC.crosshairTarget == null
			|| MC.crosshairTarget.getType() != HitResult.Type.ENTITY
			|| !(((EntityHitResult)MC.crosshairTarget)
				.getEntity() instanceof LivingEntity))
			return;
		
		doCritical();
	}
	
	public void doCritical()
	{
		if(!isEnabled())
			return;
		
		if(!MC.player.onGround)
			return;
		
		if(MC.player.isTouchingWater() || MC.player.isInLava())
			return;
		
		switch(mode.getSelected())
		{
			case JUMP:
			MC.player.addVelocity(0, 0.1, 0);
			MC.player.fallDistance = 0.1F;
			MC.player.onGround = false;
			break;
			
			case PACKET:
			double posX = MC.player.getX();
			double posY = MC.player.getY();
			double posZ = MC.player.getZ();
			
			sendPos(posX, posY + 0.0625D, posZ, true);
			sendPos(posX, posY, posZ, false);
			sendPos(posX, posY + 1.1E-5D, posZ, false);
			sendPos(posX, posY, posZ, false);
			break;
		}
	}
	
	private void sendPos(double x, double y, double z, boolean onGround)
	{
		MC.player.networkHandler.sendPacket(
			new PlayerMoveC2SPacket.PositionOnly(x, y, z, onGround));
	}
	
	private enum Mode
	{
		JUMP("Jump"),
		
		PACKET("Packet");
		
		private final String name;
		
		private Mode(String name)
		{
			this.name = name;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
