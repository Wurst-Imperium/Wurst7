/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;

@SearchTags({"BoatFlight", "boat fly", "boat flight"})
public final class BoatFlyHack extends Hack implements UpdateListener
{
	private final CheckboxSetting changeforwardSpeed =
			new CheckboxSetting("Change Forward Speed", "Allows Forward Speed to be changed, disables smooth acceleration.", false);
	public final SliderSetting forwardSpeed =
			new SliderSetting("Forward Speed", 1, 0.05, 5, 0.05, SliderSetting.ValueDisplay.DECIMAL);
	public final SliderSetting upwardSpeed =
			new SliderSetting("Upward Speed", 0.3, 0, 5, 0.05, SliderSetting.ValueDisplay.DECIMAL);
	public BoatFlyHack()
	{
		super("BoatFly");
		setCategory(Category.MOVEMENT);
		addSetting(changeforwardSpeed);
		addSetting(forwardSpeed);
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
		// check is boat
		Entity vehicle = MC.player.getVehicle();
		if (!(vehicle instanceof BoatEntity)) return;

		// fly
		Vec3d velocity = vehicle.getVelocity();
		double motionY = MC.options.jumpKey.isPressed() ? upwardSpeed.getValue() : 0;
		if (changeforwardSpeed.isChecked() && MC.options.forwardKey.isPressed()) {
			double f = forwardSpeed.getValue();
			vehicle.setVelocity(MathHelper.sin(-vehicle.getYaw() * ((float)Math.PI / 180)) * f, motionY, MathHelper.cos(vehicle.getYaw() * ((float)Math.PI / 180)) * f);
		}
		else {
			vehicle.setVelocity(new Vec3d(velocity.x, motionY, velocity.z));
		}
	}
}
