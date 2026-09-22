/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.cubemob.MagmaCube;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.wurstclient.util.text.WText;

public final class FilterSlimesSetting extends EntityFilterCheckbox
{
	public FilterSlimesSetting(WText description, boolean checked)
	{
		super("Filter slimes", description, checked);
	}
	
	@Override
	protected boolean filtersOut(Entity e)
	{
		return e instanceof Slime && !(e instanceof MagmaCube);
	}
	
	public static FilterSlimesSetting genericCombat(boolean checked)
	{
		return new FilterSlimesSetting(
			WText.translated(
				"description.wurst.setting.generic.filter_slimes_combat"),
			checked);
	}
	
	public static FilterSlimesSetting genericVision(boolean checked)
	{
		return new FilterSlimesSetting(
			WText.translated(
				"description.wurst.setting.generic.filter_slimes_vision"),
			checked);
	}
}
