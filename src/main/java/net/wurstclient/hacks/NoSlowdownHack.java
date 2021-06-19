/*
 * Copyright (c) 2014-2021 Wurst-Imperium and contributors.
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

@SearchTags({"no slowdown", "no slow down"})
public final class NoSlowdownHack extends Hack
{
	private final CheckboxSetting noIce = new CheckboxSetting(
			"No ice", "Prevent ice slipperiness\n",
			false);
	
	private final CheckboxSetting honeyJump = new CheckboxSetting(
			"Honey jump", "Allow jumping on honey\n",
			true);
	
	private final CheckboxSetting slimeUtil = new CheckboxSetting(
			"Slime utiltiy", "Removes bouncing and disables slowdown\n"
					+ "\u00A74Don't disable this while on a slime block\u00A7r\n"
					+ "Slime still negates falldamage",
			false);
	
	public NoSlowdownHack()
	{
		super("NoSlowdown", "Cancels slowness effects caused by\n"
			+ "honey, soul sand and using items.");
		setCategory(Category.MOVEMENT);
		addSetting(honeyJump);
		addSetting(noIce);
		addSetting(slimeUtil);
	}
	
	public boolean getNoIce()
	{
	if (noIce.isChecked())
		return true;
	    return false;
	}
	
	public boolean getHoneyJump()
	{
	if (honeyJump.isChecked())
		return true;
		return false;
	}
	
	public boolean getSlimeUtil()
	{
	if (slimeUtil.isChecked())
		return true;
		return false;
	}
}
