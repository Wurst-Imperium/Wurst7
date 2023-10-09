/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filters;

import java.util.function.Predicate;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.wurstclient.settings.EnumSetting;
import net.wurstclient.settings.Setting;
import net.wurstclient.settings.filterlists.EntityFilterList.EntityFilter;

public final class FilterZombiePiglinsSetting
	extends EnumSetting<FilterZombiePiglinsSetting.Mode> implements EntityFilter
{
	private FilterZombiePiglinsSetting(String description, Mode[] values,
		Mode selected)
	{
		super("Filter zombie piglins", description, values, selected);
	}
	
	public FilterZombiePiglinsSetting(String description, Mode selected)
	{
		this(description, Mode.values(), selected);
	}
	
	@Override
	public boolean test(Entity e)
	{
		return getSelected().predicate.test(e);
	}
	
	@Override
	public boolean isFilterEnabled()
	{
		return getSelected() != Mode.OFF;
	}
	
	@Override
	public Setting getSetting()
	{
		return this;
	}
	
	public static FilterZombiePiglinsSetting genericCombat(Mode selected)
	{
		return new FilterZombiePiglinsSetting("When set to \u00a7lOn\u00a7r,"
			+ " zombified piglins won't be attacked at all.\n\n"
			+ "When set to \u00a7lIf calm\u00a7r, zombified piglins won't be"
			+ " attacked until they attack first. Be warned that this filter"
			+ " cannot detect if the zombified piglins are attacking you or"
			+ " someone else.\n\n"
			+ "When set to \u00a7lOff\u00a7r, this filter does nothing and"
			+ " zombified piglins can be attacked.", selected);
	}
	
	public static FilterZombiePiglinsSetting genericVision(Mode selected)
	{
		return new FilterZombiePiglinsSetting("When set to \u00a7lOn\u00a7r,"
			+ " zombified piglins won't be shown at all.\n\n"
			+ "When set to \u00a7lIf calm\u00a7r, zombified piglins won't be"
			+ " shown until they attack something.\n\n"
			+ "When set to \u00a7lOff\u00a7r, this filter does nothing and"
			+ " zombified piglins can be shown.", selected);
	}
	
	public static FilterZombiePiglinsSetting onOffOnly(String description,
		boolean onByDefault)
	{
		Mode[] values = {Mode.ON, Mode.OFF};
		Mode selected = onByDefault ? Mode.ON : Mode.OFF;
		return new FilterZombiePiglinsSetting(description, values, selected);
	}
	
	public enum Mode
	{
		ON("On", e -> !(e instanceof ZombifiedPiglinEntity)),
		
		IF_CALM("If calm",
			e -> !(e instanceof ZombifiedPiglinEntity zpe)
				|| zpe.isAttacking()),
		
		OFF("Off", e -> true);
		
		private final String name;
		private final Predicate<Entity> predicate;
		
		private Mode(String name, Predicate<Entity> predicate)
		{
			this.name = name;
			this.predicate = predicate;
		}
		
		@Override
		public String toString()
		{
			return name;
		}
	}
}
