/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;

@SearchTags({"boat fly", "BoatFlight", "boat flight", "EntitySpeed",
	"entity speed", "horse jump"})
public final class VehicleHack extends Hack implements UpdateListener
{
	private final CheckboxSetting jump =
		new CheckboxSetting("Horse Jump",
			"Force horses to always make the highest jump.", true);
	
	private final CheckboxSetting boatControl =
		new CheckboxSetting("Boat Control",
			"Allows you to steer the boat in the direction you are facing.", true);
	
	private final CheckboxSetting swim =
		new CheckboxSetting("Auto Swim",
			"Prevents your vehicle from sinking into the water by forcing it to float.", true);
	
	private final SliderSetting swimSpeed = new SliderSetting(
		"Swim Speed", "Speed to automatically swim up when in water, if \u00a7eAuto Swim\u00a7r is enabled.",
		0.04, 0.04, 0.2, 0.01, SliderSetting.ValueDisplay.DECIMAL);
	
	private final CheckboxSetting allowGlide = new CheckboxSetting(
		"Allow Gliding",
		"Prevents you from falling down while in mid-air. Press the sprint key to move down faster.",
		false);
	
	private final CheckboxSetting changeForwardSpeed = new CheckboxSetting(
		"Change Forward Speed",
		"Allows \u00a7eForward Speed\u00a7r to be changed, disables smooth acceleration.",
		false);
	
	private final SliderSetting forwardSpeed = new SliderSetting(
		"Forward Speed", 1, 0.05, 5, 0.05, SliderSetting.ValueDisplay.DECIMAL);
	
	private final CheckboxSetting allowFlight = new CheckboxSetting(
		"Allow Flight",
		"Allows for flight while in your vehicle. You can adjust this using the \u00a7eUpward Speed\u00a7r slider.",
		false);
	
	private final SliderSetting upwardSpeed = new SliderSetting("Upward Speed",
		0.3, 0, 5, 0.05, SliderSetting.ValueDisplay.DECIMAL);
	
	public VehicleHack()
	{
		super("Vehicle");
		setCategory(Category.MOVEMENT);
		addSetting(jump);
		addSetting(boatControl);
		addSetting(swim);
		addSetting(swimSpeed);
		addSetting(allowGlide);
		addSetting(changeForwardSpeed);
		addSetting(forwardSpeed);
		addSetting(allowFlight);
		addSetting(upwardSpeed);
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
		
		Entity vehicle = MC.player.getVehicle();
		Vec3d velocity = vehicle.getVelocity();
		
		// default motion
		double motionX = velocity.x;
		double motionY = 0;
		double motionZ = velocity.z;
		
		// up/down
		if(allowFlight.isChecked() && MC.options.jumpKey.isPressed())
			motionY = upwardSpeed.getValue();
		else if(!allowGlide.isChecked() || MC.options.sprintKey.isPressed())
			motionY = velocity.y;
		
		// prevent vehicle from sinking in water
		if(swim.isChecked() && vehicle.isTouchingWater() && vehicle instanceof LivingEntity)
			motionY += swimSpeed.getValue();
		
		if(boatControl.isChecked() && vehicle instanceof BoatEntity)
			vehicle.setYaw(MC.player.getYaw());
		
		// forward
		if(MC.options.forwardKey.isPressed() && changeForwardSpeed.isChecked())
		{
			double speed = forwardSpeed.getValue();
			float yawRad = vehicle.getYaw() * MathHelper.RADIANS_PER_DEGREE;
			
			motionX = MathHelper.sin(-yawRad) * speed;
			motionZ = MathHelper.cos(yawRad) * speed;
		}
		
		// apply motion
		vehicle.setVelocity(motionX, motionY, motionZ);
	}
	
	public boolean forceHighestJump()
	{
		return isEnabled() && jump.isChecked();
	}
}
