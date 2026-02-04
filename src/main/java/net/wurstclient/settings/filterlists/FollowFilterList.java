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

public final class FollowFilterList extends EntityFilterList
{
	private FollowFilterList(List<EntityFilter> filters)
	{
		super(filters);
	}
	
	public static FollowFilterList create()
	{
		ArrayList<EntityFilter> builder = new ArrayList<>();
		
		builder.add(new FilterPlayersSetting(
			"description.wurst.setting.follow.filter_players", false));
		
		builder.add(new FilterSleepingSetting(
			"description.wurst.setting.follow.filter_sleeping", false));
		
		builder.add(new FilterFlyingSetting(
			"description.wurst.setting.follow.filter_flying", 0));
		
		builder.add(new FilterHostileSetting(
			"Won't follow hostile mobs like zombies and creepers.", true));
		
		builder.add(FilterNeutralSetting.onOffOnly(
			"description.wurst.setting.follow.filter_neutral", true));
		
		builder.add(new FilterPassiveSetting(
			"Won't follow animals like pigs and cows, ambient mobs like bats, and water mobs like fish, squid and dolphins.",
			true));
		
		builder.add(new FilterPassiveWaterSetting(
			"Won't follow passive water mobs like fish, squid, dolphins and axolotls.",
			true));
		
		builder.add(new FilterBabiesSetting(
			"Won't follow baby pigs, baby villagers, etc.", true));
		
		builder.add(new FilterBatsSetting(
			"description.wurst.setting.follow.filter_bats", true));
		
		builder.add(new FilterSlimesSetting("Won't follow slimes.", true));
		
		builder.add(new FilterPetsSetting(
			"description.wurst.setting.follow.filter_pets", true));
		
		builder.add(new FilterVillagersSetting(
			"description.wurst.setting.follow.filter_villagers", true));
		
		builder.add(new FilterZombieVillagersSetting(
			"description.wurst.setting.follow.filter_zombie_villagers", true));
		
		builder.add(new FilterGolemsSetting(
			"description.wurst.setting.follow.filter_golems", true));
		
		builder
			.add(FilterPiglinsSetting.onOffOnly("Won't follow piglins.", true));
		
		builder.add(FilterZombiePiglinsSetting.onOffOnly(
			"description.wurst.setting.follow.filter_zombie_piglins", true));
		
		builder.add(FilterEndermenSetting.onOffOnly(
			"description.wurst.setting.follow.filter_endermen", true));
		
		builder.add(new FilterShulkersSetting(
			"description.wurst.setting.follow.filter_shulkers", true));
		
		builder.add(new FilterAllaysSetting(
			"description.wurst.setting.follow.filter_allays", true));
		
		builder.add(new FilterInvisibleSetting(
			"description.wurst.setting.follow.filter_invisible", false));
		
		builder.add(new FilterArmorStandsSetting(
			"description.wurst.setting.follow.filter_armor_stands", true));
		
		builder.add(new FilterMinecartsSetting(
			"description.wurst.setting.follow.filter_minecarts", true));
		
		return new FollowFilterList(builder);
	}
}
