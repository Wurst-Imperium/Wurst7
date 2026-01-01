/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;

@SearchTags({"boat fly", "BoatFlight", "boat flight", "EntitySpeed",
	"entity speed"})
public final class BoatFlyHack extends Hack implements UpdateListener
{
	private final CheckboxSetting changeForwardSpeed = new CheckboxSetting(
		"Change Forward Speed",
		"Allows \u00a7eForward Speed\u00a7r to be changed, disables smooth acceleration.",
		false);
	
	private final SliderSetting forwardSpeed = new SliderSetting(
		"Forward Speed", 1, 0.05, 5, 0.05, SliderSetting.ValueDisplay.DECIMAL);
	
	private final SliderSetting upwardSpeed = new SliderSetting("Upward Speed",
		0.3, 0, 5, 0.05, SliderSetting.ValueDisplay.DECIMAL);
	
	public BoatFlyHack()
	{
		super("BoatFly");
		setCategory(Category.MOVEMENT);
		addSetting(changeForwardSpeed);
		addSetting(forwardSpeed);
		addSetting(upwardSpeed);
	}
	
	@Override
	protected void onEnable()
	{
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
		// check if riding
		if(!MC.player.isPassenger())
			return;
		
		Entity vehicle = MC.player.getVehicle();
		Vec3 velocity = vehicle.getDeltaMovement();
		
		// default motion
		double motionX = velocity.x;
		double motionY = 0;
		double motionZ = velocity.z;
		
		// up/down
		if(MC.options.keyJump.isDown())
			motionY = upwardSpeed.getValue();
		else if(MC.options.keySprint.isDown())
			motionY = velocity.y;
		
		// forward
		if(MC.options.keyUp.isDown() && changeForwardSpeed.isChecked())
		{
			double speed = forwardSpeed.getValue();
			float yawRad = vehicle.getYRot() * Mth.DEG_TO_RAD;
			
			motionX = Mth.sin(-yawRad) * speed;
			motionZ = Mth.cos(yawRad) * speed;
		}
		
		// apply motion
		vehicle.setDeltaMovement(motionX, motionY, motionZ);
	}
}
