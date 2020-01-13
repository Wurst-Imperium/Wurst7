/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.packet.ClientCommandC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;

@SearchTags({"EasyElytra", "extra elytra", "easy elytra"})
public final class ExtraElytraHack extends Hack implements UpdateListener
{
	private final CheckboxSetting instantFly =
		new CheckboxSetting("Instant fly", true);
	
	private final CheckboxSetting easyFly =
		new CheckboxSetting("Easy fly", false);
	
	private final CheckboxSetting stopInWater =
		new CheckboxSetting("Stop flying in water", true);
	
	private int timer;
	
	public ExtraElytraHack()
	{
		super("ExtraElytra", "Eases the use of the Elytra.");
		setCategory(Category.MOVEMENT);
		addSetting(instantFly);
		addSetting(easyFly);
		addSetting(stopInWater);
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		timer = 0;
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		if(timer > 0)
			timer--;
		
		ClientPlayNetworkHandler netHandler = MC.player.networkHandler;
		
		ItemStack chest = MC.player.getEquippedStack(EquipmentSlot.CHEST);
		if(chest == null || chest.getItem() != Items.ELYTRA)
			return;
		
		if(MC.player.isFallFlying())
		{
			if(stopInWater.isChecked() && MC.player.isInWater())
			{
				netHandler.sendPacket(new ClientCommandC2SPacket(MC.player,
					ClientCommandC2SPacket.Mode.START_FALL_FLYING));
				return;
			}
			
			if(easyFly.isChecked())
			{
				Vec3d v = MC.player.getVelocity();
				
				if(MC.options.keyJump.isPressed())
					MC.player.setVelocity(v.x, v.y + 0.08, v.z);
				else if(MC.options.keySneak.isPressed())
					MC.player.setVelocity(v.x, v.y - 0.04, v.z);
				
				if(MC.options.keyForward.isPressed())
				{
					float yaw = (float)Math.toRadians(MC.player.yaw);
					Vec3d forward = new Vec3d(-MathHelper.sin(yaw) * 0.05, 0,
						MathHelper.cos(yaw) * 0.05);
					MC.player.setVelocity(v.add(forward));
					
				}else if(MC.options.keyBack.isPressed())
				{
					float yaw = (float)Math.toRadians(MC.player.yaw);
					Vec3d forward = new Vec3d(-MathHelper.sin(yaw) * 0.05, 0,
						MathHelper.cos(yaw) * 0.05);
					MC.player.setVelocity(v.subtract(forward));
				}
			}
		}else if(instantFly.isChecked() && ElytraItem.isUsable(chest)
			&& MC.options.keyJump.isPressed())
		{
			if(timer <= 0)
			{
				timer = 20;
				MC.player.setJumping(false);
				MC.player.setSprinting(true);
				MC.player.jump();
			}
			
			netHandler.sendPacket(new ClientCommandC2SPacket(MC.player,
				ClientCommandC2SPacket.Mode.START_FALL_FLYING));
		}
	}
}
