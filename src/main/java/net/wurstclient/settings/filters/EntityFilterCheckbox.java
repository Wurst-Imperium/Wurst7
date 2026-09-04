/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import net.minecraft.world.entity.Entity;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.Setting;
import net.wurstclient.settings.filterlists.EntityFilterList.EntityFilter;

public abstract class EntityFilterCheckbox extends CheckboxSetting
	implements EntityFilter
{
	public EntityFilterCheckbox(String name, String description,
		boolean checked)
	{
		super(name, description, checked);
	}
	
	@Override
	public final boolean test(Entity e)
	{
		return !isChecked() || !filtersOut(e);
	}
	
	@Override
	public final Setting getSetting()
	{
		return this;
	}
	
	protected abstract boolean filtersOut(Entity e);
}
