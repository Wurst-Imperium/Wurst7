/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
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
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"speed hack"})
public final class SpeedHackHack extends Hack implements UpdateListener
{

	private final CheckboxSetting goNCP = new CheckboxSetting("Abide by NoCheatPlus", true);
	private final SliderSetting speedMultiplier = new SliderSetting("Speed Multiplier", 2, 1, 10, .5, ValueDisplay.INTEGER);

	public SpeedHackHack()
	{
		super("SpeedHack");
		setCategory(Category.MOVEMENT);
		addSetting(goNCP);
		addSetting(speedMultiplier);
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
		
		// activate sprint if walking forward
		if(MC.player.forwardSpeed > 0 && !MC.player.horizontalCollision)
			MC.player.setSprinting(true);
		
		// activate mini jump if on ground
		if(!MC.player.isOnGround())
			return;
		
		Vec3d v = MC.player.getVelocity();
		// MC.player.setVelocity(v.x * 1.8, v.y + 0.1, v.z * 1.8); // old
		MC.player.setVelocity(v.x * speedMultiplier.getValueI(), v.y + 0.1, v.z * speedMultiplier.getValueI());
		
		v = MC.player.getVelocity();
		double currentSpeed = Math.sqrt(Math.pow(v.x, 2) + Math.pow(v.z, 2));
		
		// limit speed to highest value that works on NoCheat+ version
		// 3.13.0-BETA-sMD5NET-b878
		// UPDATE: Patched in NoCheat+ version 3.13.2-SNAPSHOT-sMD5NET-b888
		double maxSpeed = 0.66F;
		
		if(currentSpeed > maxSpeed && goNCP.isChecked())
			MC.player.setVelocity(v.x / currentSpeed * maxSpeed, v.y,
				v.z / currentSpeed * maxSpeed);
	}
}
