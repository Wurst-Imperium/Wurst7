/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.TextFormat;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.IsPlayerInWaterListener;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"FlyHack", "fly hack", "flying"})
public final class FlightHack extends Hack
	implements UpdateListener, IsPlayerInWaterListener
{
	public final SliderSetting speed =
		new SliderSetting("Speed", 1, 0.05, 5, 0.05, ValueDisplay.DECIMAL);
	
	public FlightHack()
	{
		super("Flight",
			"Allows you to you fly.\n\n" + TextFormat.RED + TextFormat.BOLD
				+ "WARNING:" + TextFormat.RESET
				+ " You will take fall damage if you don't use NoFall.");
		setCategory(Category.MOVEMENT);
		addSetting(speed);
	}
	
	@Override
	public void onEnable()
	{
		EVENTS.add(UpdateListener.class, this);
		EVENTS.add(IsPlayerInWaterListener.class, this);
	}
	
	@Override
	public void onDisable()
	{
		EVENTS.remove(UpdateListener.class, this);
		EVENTS.remove(IsPlayerInWaterListener.class, this);
	}
	
	@Override
	public void onUpdate()
	{
		ClientPlayerEntity player = MC.player;
		
		player.abilities.flying = false;
		player.flyingSpeed = speed.getValueF();
		
		player.setVelocity(0, 0, 0);
		Vec3d velcity = player.getVelocity();
		
		if(MC.options.keyJump.isPressed())
			player.setVelocity(velcity.add(0, speed.getValue(), 0));
		
		if(MC.options.keySneak.isPressed())
			player.setVelocity(velcity.subtract(0, speed.getValue(), 0));
	}
	
	@Override
	public void onIsPlayerInWater(IsPlayerInWaterEvent event)
	{
		event.setInWater(false);
	}
}
