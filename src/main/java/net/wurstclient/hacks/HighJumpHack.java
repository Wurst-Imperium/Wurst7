/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.hacks;

import net.wurstclient.Category;
import net.wurstclient.SearchTags;
import net.wurstclient.hack.Hack;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"high jump"})
public final class HighJumpHack extends Hack
{
	private final SliderSetting height = new SliderSetting("Height",
		"Jump height in blocks.", 6, 1, 100, 1, ValueDisplay.INTEGER);
	
	public HighJumpHack()
	{
		super("HighJump");
		setCategory(Category.MOVEMENT);
		addSetting(height);
	}
	
	public float getAdditionalJumpMotion()
	{
		if(!isEnabled())
			return 0;
		
		double targetHeight = height.getValue();
		double lower = 0;
		double upper = targetHeight;
		
		// Binary search using Minecraft's normal per-tick gravity and drag
		for(int i = 0; i < 32; i++)
		{
			double motion = (lower + upper) / 2;
			double jumpHeight = 0;
			for(double velocity = motion; velocity >= 0.003; velocity =
				(velocity - 0.08) * 0.98F)
				jumpHeight += velocity;
			
			if(jumpHeight < targetHeight)
				lower = motion;
			else
				upper = motion;
		}
		
		// Subtract vanilla power, but round up to avoid falling short of ledges
		return Math.nextUp((float)upper) - 0.42F;
	}
}
