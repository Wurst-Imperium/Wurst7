/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"jet pack", "AirJump", "air jump"})
public final class JetpackHack extends Hack implements UpdateListener
{
	public final SliderSetting verticalSpeed = new SliderSetting(
		"Vertical Speed",
		"The default 0.42 is vanilla jump speed",
		0.42, 0.05, 4, 0.01, ValueDisplay.DECIMAL);

	public JetpackHack()
	{
		super("Jetpack");
		
		setCategory(Category.MOVEMENT);
		addSetting(verticalSpeed);
	}
	
	@Override
	public void onEnable()
	{
		WURST.getHax().creativeFlightHack.setEnabled(false);
		WURST.getHax().flightHack.setEnabled(false);
		
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

		Vec3d velocity = player.getVelocity();
		
		if((player.isFallFlying() || velocity.y != 0) && MC.options.jumpKey.isPressed())
			player.setVelocity(velocity.x, verticalSpeed.getValue(),
				velocity.z);
	}
}
