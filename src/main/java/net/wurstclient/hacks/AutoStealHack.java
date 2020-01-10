/*
 * Copyright (C) 2014 - 2020 | Alexander01998 | All rights reserved.
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

import java.util.Random;

@SearchTags({"auto steal", "ChestStealer", "chest stealer",
	"steal store buttons", "Steal/Store buttons"})
public final class AutoStealHack extends Hack
{
	private final SliderSetting delay = new SliderSetting("Delay",
		"Delay between moving stacks of items.\n"
			+ "Should be at least 70ms for NoCheat+ servers.",
		100, 0, 500, 10, v -> (int)v + "ms");

	private final SliderSetting randomDelay = new SliderSetting("Delay random",
		"Randomly delay moving stacks of items by +/- value to help avoid anti-cheats.\n"
			+ "Do not set this value greater than delay.",
		20, 0, 200, 10, v -> (int)v + "ms");
	
	private final CheckboxSetting buttons =
		new CheckboxSetting("Steal/Store buttons", true);

	private final Random random = new Random();
	
	public AutoStealHack()
	{
		super("AutoSteal", "Automatically steals everything\n"
			+ "from all chests that you open.");
		setCategory(Category.ITEMS);
		addSetting(buttons);
		addSetting(delay);
		addSetting(randomDelay);
	}
	
	public boolean areButtonsVisible()
	{
		return buttons.isChecked();
	}
	
	public long getDelay()
	{
		return delay.getValueI() + getRandom();
	}

	private long getRandom()
	{
		return random.nextInt(randomDelay.getValueI()) * 2 - (randomDelay.getValueI());
	}
}
