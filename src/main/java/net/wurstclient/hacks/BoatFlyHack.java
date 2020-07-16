/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | Jakiki6 | All rights reserved.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.MovementType;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;
import net.wurstclient.settings.CheckboxSetting;

@SearchTags({"BoatFlight", "boat fly", "boat flight"})
public final class BoatFlyHack extends Hack implements UpdateListener
{
	public final SliderSetting speed_up =
		new SliderSetting("Speed up", 1, 0.05, 4, 0.05, ValueDisplay.DECIMAL);
	public final SliderSetting speed_dir =
                new SliderSetting("Speed diagonal", 5, 0.05, 32, 0.05, ValueDisplay.DECIMAL);
	public final SliderSetting speed_down =
                new SliderSetting("Speed down", 0.0005, -1, 1, 0.0005, ValueDisplay.DECIMAL);
	public final CheckboxSetting out_of_v =
		new CheckboxSetting("Without boat", "Hack without any vehicle", false);

	public BoatFlyHack()
	{
		super("BoatFly", "Allows you to fly with boats");
		setCategory(Category.MOVEMENT);
		addSetting(speed_up);
		addSetting(speed_dir);
		addSetting(speed_down);
		addSetting(out_of_v);
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
		if(!MC.player.hasVehicle() && !out_of_v.isChecked())
			return;
		// fly
		Entity vehicle = MC.player.getVehicle();
		if (vehicle == null)
			vehicle = MC.player;
		double maxSpeed = speed_dir.getValue();
		Vec3d v = vehicle.getVelocity();

		double add = MC.player.age % 2 == 0 ? v.y: -v.y;
		v = new Vec3d(v.x * maxSpeed, MC.options.keyJump.isPressed() ? speed_up.getValue() : (MC.player.age % 2 == 0 ? speed_down.getValue() : -speed_down.getValue()), v.z * maxSpeed);
		vehicle.move(MovementType.SELF, v);
		v = vehicle.getVelocity();
		vehicle.setVelocity(new Vec3d(v.x, 0, v.z));

	}
}
