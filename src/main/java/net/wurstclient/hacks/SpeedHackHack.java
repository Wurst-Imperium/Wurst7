/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.minecraft.util.math.Vec3d;
import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.events.UpdateListener;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"speed hack"})
public final class SpeedHackHack extends Hack implements UpdateListener
{
	public final EnumSetting<Mode> mode = new EnumSetting<>("Mode",
		"\u00a7lSimple\u00a7r mode increases all kinds of movements you do.\n\n"
			+ "\u00a7lMicroJump\u00a7r mode makes you do many small jumps.\n"
			+ "May help with some anticheat plugins, but only works while on ground.\n\n",
		Mode.values(), Mode.SIMPLE);

	public final SliderSetting speed = new SliderSetting(
	"Horizontal Speed", 0.66, 0.4, 5, 0.01, ValueDisplay.DECIMAL);

	public SpeedHackHack()
	{
		super("SpeedHack");
		setCategory(Category.MOVEMENT);
		addSetting(speed);
		addSetting(mode);
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
		// return if sneaking or not walking
		if(MC.player.isSneaking()
			|| MC.player.forwardSpeed == 0 && MC.player.sidewaysSpeed == 0)
			return;
		
		double maxSpeed = speed.getValue();
		Vec3d v = MC.player.getVelocity();

		// only use SpeedHack when on ground
		if(!MC.player.isOnGround())
			return;

		if (mode.getSelected() == Mode.MICROJUMP) {
			// activate sprint if walking forward
			if(MC.player.forwardSpeed > 0 && !MC.player.horizontalCollision && 
				mode.getSelected() == Mode.MICROJUMP)
				MC.player.setSprinting(true);
					
			double multiplier = maxSpeed / 0.66 * 1.8;

			MC.player.setVelocity(v.x * multiplier, v.y + 0.1, v.z * multiplier);
		
			v = MC.player.getVelocity();
		} else {
			double multiplier = maxSpeed / 0.66 * 1.8;

			MC.player.setVelocity(v.x * multiplier, v.y, v.z * multiplier);
		
			v = MC.player.getVelocity();
		}
		// limit movement speed
		double currentSpeed = Math.sqrt(Math.pow(v.x, 2) + Math.pow(v.z, 2));
		if(currentSpeed > maxSpeed)
			MC.player.setVelocity(v.x / currentSpeed * maxSpeed, v.y,
				v.z / currentSpeed * maxSpeed);
	}


	public static enum Mode
	{
		SIMPLE("Simple"),
		MICROJUMP("MicroJump");
		
		private final String name;

		private Mode(String name)
		{
			this.name = name;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}

}
