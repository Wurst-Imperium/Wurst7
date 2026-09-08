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

public final class AnchorAuraFilterList extends EntityFilterList
{
	private AnchorAuraFilterList(List<EntityFilter> filters)
	{
		super(filters);
	}
	
	public static AnchorAuraFilterList create()
	{
		ArrayList<EntityFilter> builder = new ArrayList<>();
		
		builder.add(
			new FilterPlayersSetting(description("filter_players"), false));
		
		builder
			.add(new FilterHostileSetting(description("filter_hostile"), true));
		
		builder.add(new FilterNeutralSetting(description("filter_neutral"),
			AttackDetectingEntityFilter.Mode.ON));
		
		builder
			.add(new FilterPassiveSetting(description("filter_passive"), true));
		
		builder.add(new FilterPassiveWaterSetting(
			description("filter_passive_water"), true));
		
		builder.add(new FilterBatsSetting(description("filter_bats"), true));
		
		builder
			.add(new FilterSlimesSetting(description("filter_slimes"), true));
		
		builder.add(
			new FilterVillagersSetting(description("filter_villagers"), true));
		
		builder.add(new FilterZombieVillagersSetting(
			description("filter_zombie_villagers"), true));
		
		builder
			.add(new FilterGolemsSetting(description("filter_golems"), true));
		
		builder.add(new FilterPiglinsSetting(description("filter_piglins"),
			AttackDetectingEntityFilter.Mode.ON));
		
		builder.add(
			new FilterZombiePiglinsSetting(description("filter_zombie_piglins"),
				AttackDetectingEntityFilter.Mode.ON));
		
		builder.add(
			new FilterShulkersSetting(description("filter_shulkers"), true));
		
		builder
			.add(new FilterAllaysSetting(description("filter_allays"), true));
		
		builder.add(
			new FilterInvisibleSetting(description("filter_invisible"), false));
		
		builder.add(new FilterNamedSetting(description("filter_named"), false));
		
		builder.add(new FilterArmorStandsSetting(
			description("filter_armor_stands"), true));
		
		return new AnchorAuraFilterList(builder);
	}
	
	private static WText description(String key)
	{
		return WText.translated("description.wurst.setting.anchoraura." + key)
			.append("\n\n").append(WText.translated(
				"description.wurst.setting.anchoraura.filter_damage_warning"));
	}
}
