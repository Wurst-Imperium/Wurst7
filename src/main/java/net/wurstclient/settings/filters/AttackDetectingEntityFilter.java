/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import java.util.function.Supplier;

import net.minecraft.entity.Entity;
import net.wurstclient.settings.CheckboxSetting;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.Setting;
import net.wurstclient.settings.filterlists.EntityFilterList.EntityFilter;

public abstract class AttackDetectingEntityFilter implements EntityFilter
{
	private final Setting setting;
	private final Supplier<Mode> mode;
	
	protected AttackDetectingEntityFilter(String name, String description,
		Mode selected, boolean checked)
	{
		if(selected == null)
		{
			CheckboxSetting cbSetting =
				new CheckboxSetting(name, description, checked);
			setting = cbSetting;
			mode = () -> cbSetting.isChecked() ? Mode.ON : Mode.OFF;
			
		}else
		{
			EnumSetting<Mode> enumSetting =
				new EnumSetting<>(name, description, Mode.values(), selected);
			setting = enumSetting;
			mode = () -> enumSetting.getSelected();
		}
	}
	
	public abstract boolean onTest(Entity e);
	
	public abstract boolean ifCalmTest(Entity e);
	
	@Override
	public final boolean test(Entity e)
	{
		return mode.get() == Mode.IF_CALM ? ifCalmTest(e) : onTest(e);
	}
	
	@Override
	public final boolean isFilterEnabled()
	{
		return mode.get() != Mode.OFF;
	}
	
	@Override
	public final Setting getSetting()
	{
		return setting;
	}
	
	public enum Mode
	{
		ON("On"),
		IF_CALM("If calm"),
		OFF("Off");
		
		private final String name;
		
		private Mode(String name)
		{
			this.name = name;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
