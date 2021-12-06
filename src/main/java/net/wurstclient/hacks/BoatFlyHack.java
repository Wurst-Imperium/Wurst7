/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
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
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"BoatFlight", "boat fly", "boat flight"})
public final class BoatFlyHack extends Hack implements UpdateListener
{
	private final SliderSetting moveSpeed=new SliderSetting("Move speed", "Provides an additional horizontal speed to increase movement speed"//
			+"\n提供一个额外的水平速度，提高移动速度"//Chinese description, can be deleted
			, 0, 0, 2, 0.01, ValueDisplay.DECIMAL);
	private final SliderSetting flySpeed=new SliderSetting("Fly speed", "Set vertical speed"//
			+"\n设定垂直速度"//Chinese description, can be deleted
			, 0.3, 0, 2, 0.01, null);
	public BoatFlyHack()
	{
		super("BoatFly");
		setCategory(Category.MOVEMENT);
		addSetting(moveSpeed);
		addSetting(flySpeed);
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
		double motionY = MC.options.keyJump.isPressed() ? this.flySpeed.getValue() : 0;
		double speed   = velocity.horizontalLength();
		speed = (speed + this.moveSpeed.getValue()) / speed;
		vehicle.setVelocity(new Vec3d(velocity.x * speed, motionY, velocity.z * speed));
	}
}
