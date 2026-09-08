/*
 * Copyright (c) 2014-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filterlists;

import java.util.ArrayList;
import java.util.List;

import net.wurstclient.settings.filters.*;
import net.wurstclient.util.text.WText;

public final class FollowFilterList extends EntityFilterList
{
	private FollowFilterList(List<EntityFilter> filters)
	{
		super(filters);
	}
	
	public static FollowFilterList create()
	{
		ArrayList<EntityFilter> builder = new ArrayList<>();
		
		builder.add(
			new FilterPlayersSetting(description("filter_players"), false));
		
		builder.add(
			new FilterSleepingSetting(description("filter_sleeping"), false));
		
		builder.add(new FilterFlyingSetting(description("filter_flying"), 0));
		
		builder
			.add(new FilterHostileSetting(description("filter_hostile"), true));
		
		builder.add(FilterNeutralSetting
			.onOffOnly(description("filter_neutral"), true));
		
		builder
			.add(new FilterPassiveSetting(description("filter_passive"), true));
		
		builder.add(new FilterPassiveWaterSetting(
			description("filter_passive_water"), true));
		
		builder
			.add(new FilterBabiesSetting(description("filter_babies"), true));
		
		builder.add(new FilterBatsSetting(description("filter_bats"), true));
		
		builder
			.add(new FilterSlimesSetting(description("filter_slimes"), true));
		
		builder.add(new FilterPetsSetting(description("filter_pets"), true));
		
		builder.add(
			new FilterVillagersSetting(description("filter_villagers"), true));
		
		builder.add(new FilterZombieVillagersSetting(
			description("filter_zombie_villagers"), true));
		
		builder
			.add(new FilterGolemsSetting(description("filter_golems"), true));
		
		builder.add(FilterPiglinsSetting
			.onOffOnly(description("filter_piglins"), true));
		
		builder.add(FilterZombiePiglinsSetting
			.onOffOnly(description("filter_zombie_piglins"), true));
		
		builder.add(FilterEndermenSetting
			.onOffOnly(description("filter_endermen"), true));
		
		builder.add(
			new FilterShulkersSetting(description("filter_shulkers"), true));
		
		builder
			.add(new FilterAllaysSetting(description("filter_allays"), true));
		
		builder.add(
			new FilterInvisibleSetting(description("filter_invisible"), false));
		
		builder.add(new FilterArmorStandsSetting(
			description("filter_armor_stands"), true));
		
		builder.add(
			new FilterMinecartsSetting(description("filter_minecarts"), true));
		
		return new FollowFilterList(builder);
	}
	
	private static WText description(String key)
	{
		return WText.translated("description.wurst.setting.follow." + key);
	}
}
