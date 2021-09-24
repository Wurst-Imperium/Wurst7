/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
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
	private final EnumSetting<Mode> mode = new EnumSetting<>("模式",
		"§l数据包§r:发送数据包,不需要真正地跳跃\n§l小跳§r:进行一个微小的跳跃,这足以打出暴击伤害\n§l全跳§r:使你正常跳跃",
		Mode.values(), Mode.PACKET);
	
	public CriticalsHack()
	{
		super("暴击", "刀刀暴击");
		setCategory(Category.COMBAT);
		addSetting(mode);
	}
	
	@Override
	public String getRenderName()
	{
		return getName() + " [" + mode.getSelected() + "]";
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
		
		if(!MC.player.isOnGround())
			return;
		
		if(MC.player.isTouchingWater() || MC.player.isInLava())
			return;
		
		switch(mode.getSelected())
		{
			case PACKET:
			doPacketJump();
			break;
			
			case MINI_JUMP:
			doMiniJump();
			break;
			
			case FULL_JUMP:
			doFullJump();
			break;
		}
	}
	
	private void doPacketJump()
	{
		double posX = MC.player.getX();
		double posY = MC.player.getY();
		double posZ = MC.player.getZ();
		
		sendPos(posX, posY + 0.0625D, posZ, true);
		sendPos(posX, posY, posZ, false);
		sendPos(posX, posY + 1.1E-5D, posZ, false);
		sendPos(posX, posY, posZ, false);
	}
	
	private void sendPos(double x, double y, double z, boolean onGround)
	{
		MC.player.networkHandler.sendPacket(
			new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, onGround));
	}
	
	private void doMiniJump()
	{
		MC.player.addVelocity(0, 0.1, 0);
		MC.player.fallDistance = 0.1F;
		MC.player.setOnGround(false);
	}
	
	private void doFullJump()
	{
		MC.player.jump();
	}
	
	private enum Mode
	{
		PACKET("数据包"),
		MINI_JUMP("小跳"),
		FULL_JUMP("全跳");
		
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
