/*
 * Copyright (c) 2014-2024 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wurstclient.settings.filterlists;

import java.util.ArrayList;
import java.util.List;

import net.wurstclient.settings.filters.*;

public final class RemoteViewFilterList extends EntityFilterList
{
	private RemoteViewFilterList(List<EntityFilter> filters)
	{
		super(filters);
	}
	
	public static RemoteViewFilterList create()
	{
		ArrayList<EntityFilter> builder = new ArrayList<>();
		
		builder
			.add(new FilterPlayersSetting("Won't view other players.", false));
		
		builder.add(
			new FilterSleepingSetting("Won't view sleeping players.", false));
		
		builder.add(new FilterFlyingSetting(
			"Won't view players that are at least the given distance above ground.",
			0));
		
		builder.add(new FilterHostileSetting(
			"Won't view hostile mobs like zombies and creepers.", true));
		
		builder.add(FilterNeutralSetting.onOffOnly(
			"Won't view neutral mobs like endermen and wolves.", true));
		
		builder.add(new FilterPassiveSetting("Won't view animals like pigs and"
			+ " cows, ambient mobs like bats, and water mobs like fish, squid"
			+ " and dolphins.", true));
		
		builder.add(new FilterPassiveWaterSetting("Won't view passive water"
			+ " mobs like fish, squid, dolphins and axolotls.", true));
		
		builder.add(new FilterBabiesSetting(
			"Won't view baby pigs, baby villagers, etc.", true));
		
		builder.add(new FilterBatsSetting("Won't view bats and any other"
			+ " \"ambient\" mobs that might be added by mods.", true));
		
		builder.add(new FilterSlimesSetting("Won't view slimes.", true));
		
		builder.add(new FilterPetsSetting(
			"Won't view tamed wolves, tamed horses, etc.", true));
		
		builder.add(new FilterVillagersSetting(
			"Won't view villagers and wandering traders.", true));
		
		builder.add(new FilterZombieVillagersSetting(
			"Won't view zombified villagers.", true));
		
		builder.add(new FilterGolemsSetting(
			"Won't view iron golems and snow golems.", true));
		
		builder
			.add(FilterPiglinsSetting.onOffOnly("Won't view piglins.", true));
		
		builder.add(FilterZombiePiglinsSetting
			.onOffOnly("Won't view zombified piglins.", true));
		
		builder
			.add(FilterEndermenSetting.onOffOnly("Won't view endermen.", true));
		
		builder.add(new FilterShulkersSetting("Won't view shulkers.", true));
		
		builder.add(new FilterAllaysSetting("Won't view allays.", true));
		
		builder.add(new FilterInvisibleSetting("Won't view invisible entities.",
			false));
		
		builder.add(
			new FilterArmorStandsSetting("Won't view armor stands.", true));
		
		return new RemoteViewFilterList(builder);
	}
}
