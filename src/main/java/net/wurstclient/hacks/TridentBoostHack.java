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
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.SliderSetting;
import net.wurstclient.settings.SliderSetting.ValueDisplay;

@SearchTags({"trident boost", "riptide", "trident speed"})
public final class TridentBoostHack extends Hack
{
	private final SliderSetting multiplier = new SliderSetting("Boost",
		"The amount of velocity multiplied by when using riptide.", 2, 0.1, 10,
		0.1, ValueDisplay.DECIMAL);
	
	private final CheckboxSetting allowOutOfWater = new CheckboxSetting(
		"Out-of-water", "Allow riptide to work out of water or rain.", true);
	
	public TridentBoostHack()
	{
		super("TridentBoost");
		setCategory(Category.MOVEMENT);
		addSetting(multiplier);
		addSetting(allowOutOfWater);
	}
	
	public double getMultiplier()
	{
		return isEnabled() ? multiplier.getValue() : 1;
	}
	
	public boolean isOutOfWaterAllowed()
	{
		return isEnabled() && allowOutOfWater.isChecked();
	}
	
	// See TridentItemMixin
}
