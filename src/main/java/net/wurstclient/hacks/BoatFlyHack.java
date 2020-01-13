/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;

@SearchTags({"BoatFlight", "boat fly", "boat flight"})
public final class BoatFlyHack extends Hack implements UpdateListener
{
	public BoatFlyHack()
	{
		super("BoatFly", "Allows you to fly with boats");
		setCategory(Category.MOVEMENT);
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
		// check if riding
		if(!MC.player.hasVehicle())
			return;
		
		// fly
		Entity vehicle = MC.player.getVehicle();
		Vec3d velocity = vehicle.getVelocity();
		double motionY = MC.options.keyJump.isPressed() ? 0.3 : 0;
		vehicle.setVelocity(new Vec3d(velocity.x, motionY, velocity.z));
	}
}
