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

public final class MobEspFilterList extends EntityFilterList
{
	private MobEspFilterList(List<EntityFilter> filters)
	{
		super(filters);
	}
	
	public static MobEspFilterList create()
	{
		ArrayList<EntityFilter> builder = new ArrayList<>();
		
		builder.add(new FilterMonstersSetting(
			"Won't show zombies, creepers, etc.", false));
		
		builder
			.add(new FilterPigmenSetting("Won't show zombie pigmen.", false));
		
		builder.add(new FilterEndermenSetting("Won't show endermen.", false));
		
		builder.add(
			new FilterAnimalsSetting("Won't show pigs, cows, etc.", false));
		
		builder.add(FilterBatsSetting.genericVision(false));
		
		builder.add(new FilterPetsSetting(
			"Won't show tamed wolves, tamed horses, etc.", false));
		
		builder.add(new FilterTradersSetting(
			"Won't show villagers, wandering traders, etc.", false));
		
		builder.add(new FilterGolemsSetting(
			"Won't show iron golems, snow golems and shulkers.", false));
		
		builder.add(new FilterAllaysSetting("Won't show allays.", false));
		
		builder.add(
			new FilterInvisibleSetting("Won't show invisible mobs.", false));
		
		builder.add(
			new FilterArmorStandsSetting("Won't show armor stands.", true));
		
		return new MobEspFilterList(builder);
	}
}
