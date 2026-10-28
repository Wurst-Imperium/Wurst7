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

@SearchTags({"anti water push", "NoWaterPush", "no water push"})
public final class AntiWaterPushHack extends Hack
{
	private final CheckboxSetting preventSlowdown = new CheckboxSetting(
		"Prevent slowdown", "Allows you to walk underwater at full speed.\n"
			+ "Some servers consider this a speedhack.",
		false);
	
	public AntiWaterPushHack()
	{
		super("AntiWaterPush");
		setCategory(Category.MOVEMENT);
		addSetting(preventSlowdown);
	}
	
	public boolean isPreventingSlowdown()
	{
		return isEnabled() && preventSlowdown.isChecked();
	}
}
