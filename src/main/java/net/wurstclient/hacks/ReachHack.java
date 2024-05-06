/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
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

// TODO: Remove this hack in 1.20.5, as it seems to be patched.
@SearchTags({"range"})
public final class ReachHack extends Hack
{
	private final SliderSetting range =
		new SliderSetting("Range", 6, 1, 10, 0.05, ValueDisplay.DECIMAL);
	
	public ReachHack()
	{
		super("Reach");
		setCategory(Category.OTHER);
		addSetting(range);
	}
	
	public double getReachDistance()
	{
		return range.getValue();
	}
	
	// See ClientPlayerEntityMixin.method_55754() and
	// ClientPlayerEntityMixin.method_55755()
}
