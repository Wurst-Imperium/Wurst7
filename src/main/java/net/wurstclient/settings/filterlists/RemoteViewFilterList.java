/*
 * Copyright (c) 2014-2023 Wurst-Imperium and contributors.
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
		
		builder.add(new FilterMonstersSetting(
			"Won't view zombies, creepers, etc.", true));
		
		builder.add(new FilterPigmenSetting("Won't view zombie pigmen.", true));
		
		builder.add(new FilterEndermenSetting("Won't view endermen.", true));
		
		builder
			.add(new FilterAnimalsSetting("Won't view pigs, cows, etc.", true));
		
		builder.add(new FilterBabiesSetting(
			"Won't view baby pigs, baby villagers, etc.", true));
		
		builder.add(new FilterPetsSetting(
			"Won't view tamed wolves, tamed horses, etc.", true));
		
		builder.add(new FilterTradersSetting(
			"Won't view villagers, wandering traders, etc.", true));
		
		builder.add(new FilterGolemsSetting(
			"Won't view iron golems, snow golems and shulkers.", true));
		
		builder.add(new FilterAllaysSetting(
				"Won't view allays.", true));
			
		builder.add(new FilterInvisibleSetting("Won't view invisible entities.",
			false));
		
		builder.add(
			new FilterArmorStandsSetting("Won't view armor stands.", true));
		
		return new RemoteViewFilterList(builder);
	}
}
