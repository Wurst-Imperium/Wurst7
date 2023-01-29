/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
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

@SearchTags({"no push", "anticollide", "anti collision"})
public final class PlayerCollisionHack extends Hack
{
	private final SliderSetting reduction =
		new SliderSetting("Reduction Percentage",
			"Reduces collision by the specified percent.\n"
			+ "Values over 100% will cause you to move towards the entity.",
			1, 0, 4, 0.05, ValueDisplay.PERCENTAGE);
	
	public PlayerCollisionHack()
	{
		super("PlayerCollision");
		setCategory(Category.OTHER);
		addSetting(reduction);
	}
	
	public double getCollisionReduction()
	{
		return isEnabled() ? reduction.getValue() : 0;
	}
	
	// See EntityMixin.adjustPlayerCollision()
}
