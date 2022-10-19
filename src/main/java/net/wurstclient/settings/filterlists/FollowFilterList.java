/*
 * Copyright (c) 2014-2022 Wurst-Imperium and contributors.
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
		
		builder.add(
			new FilterPlayersSetting("Won't follow other players.", false));
		
		builder.add(
			new FilterSleepingSetting("Won't follow sleeping players.", false));
		
		builder.add(new FilterFlyingSetting(
			"Won't follow players that are at least the given distance above ground.",
			0));
		
		builder.add(new FilterMonstersSetting(
			"Won't follow zombies, creepers, etc.", true));
		
		builder
			.add(new FilterPigmenSetting("Won't follow zombie pigmen.", true));
		
		builder.add(new FilterEndermenSetting("Won't follow endermen.", true));
		
		builder.add(
			new FilterAnimalsSetting("Won't follow pigs, cows, etc.", true));
		
		builder.add(new FilterBabiesSetting(
			"Won't follow baby pigs, baby villagers, etc.", true));
		
		builder.add(new FilterPetsSetting(
			"Won't follow tamed wolves, tamed horses, etc.", true));
		
		builder.add(new FilterTradersSetting(
			"Won't follow villagers, wandering traders, etc.", true));
		
		builder.add(new FilterGolemsSetting(
			"Won't follow iron golems, snow golems and shulkers.", true));
		
		builder.add(new FilterAllaysSetting(
				"Won't follow allays.", true));
			
		builder.add(new FilterInvisibleSetting(
			"Won't follow invisible entities.", false));
		
		builder.add(
			new FilterArmorStandsSetting("Won't follow armor stands.", true));
		
		builder
			.add(new FilterMinecartsSetting("Won't follow minecarts.", true));
		
		return new FollowFilterList(builder);
	}
}
