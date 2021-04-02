/*
 * Copyright (c) 2014-2020 Wurst-Imperium and contributors.
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
import net.wurstclient.WurstClient;

@SearchTags({"EntityControl", "entity control", "contorl entity", "saddle", "Saddle", "Entity Speed", "Speed", "speed hacks", "SpeedHacks"})
public final class EntityControlHack extends Hack implements UpdateListener
{
    private final SliderSetting speed = new SliderSetting("Speed",
    "The Speed that the entitys should move at.\n" + "(1 is fastest vanilla horse speed).\n",
    1, 0.05, 10, 0.05, v -> (float)v + "");

	public EntityControlHack()
	{
		super("EntityControl", "Allows you to control entitys without a saddle");
        setCategory(Category.MOVEMENT);
        addSetting(speed);
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
		
		// move
		Entity vehicle = MC.player.getVehicle();
        Vec3d velocity = vehicle.getVelocity();
        float playerRot = MC.player.headYaw;
        vehicle.setYaw(playerRot);
		vehicle.setHeadYaw(playerRot);
		double yVel = velocity.y;
		if(MC.options.keyJump.isPressed() && vehicle.isOnGround()){yVel=1.5;}
        if(MC.options.keyForward.isPressed()){vehicle.setVelocity(new Vec3d((0.7115*speed.getValue()) * Math.cos((playerRot+90) * 0.0174532925), yVel, (0.7115*speed.getValue()) * Math.sin((playerRot+90) * 0.0174532925)));}
		else{vehicle.setVelocity(0,yVel,0);}
	}
}
